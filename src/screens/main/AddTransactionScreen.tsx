import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { useSQLiteContext } from 'expo-sqlite';
import { useNavigation } from '@react-navigation/native';

interface Category {
  id: number;
  name: string;
  color: string;
  icon: string;
}

export default function AddTransactionScreen() {
  const db = useSQLiteContext();
  const navigation = useNavigation();

  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<'income' | 'expense'>('expense');
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [accountId, setAccountId] = useState<number | null>(null);

  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    try {
      const cats = await db.getAllAsync<Category>('SELECT * FROM categories');
      setCategories(cats);

      const acc = await db.getAllAsync<{ id: number }>('SELECT id FROM accounts LIMIT 1');
      if (acc.length > 0) {
        setAccountId(acc[0].id);
      }
    } catch (error) {
      console.error("Error fetching initial data:", error);
    }
  };

  const handleSave = async () => {
    if (!amount || isNaN(Number(amount))) {
      Alert.alert('Erro', 'Por favor, insira um valor numérico válido.');
      return;
    }
    if (!selectedCategoryId && type === 'expense') {
      Alert.alert('Erro', 'Por favor, selecione uma categoria.');
      return;
    }
    if (!accountId) {
      Alert.alert('Erro', 'Conta não encontrada.');
      return;
    }

    try {
      const date = new Date().toISOString(); // Using current date/time for simplicity
      const parsedAmount = Math.abs(parseFloat(amount)); // Force absolute value

      await db.runAsync(
        'INSERT INTO transactions (amount, type, description, date, category_id, account_id) VALUES (?, ?, ?, ?, ?, ?)',
        [parsedAmount, type, description, date, selectedCategoryId, accountId]
      );

      Alert.alert('Sucesso', 'Transação registrada!');
      setAmount('');
      setDescription('');
      setSelectedCategoryId(null);
      
      // Go back to Dashboard
      navigation.goBack();
    } catch (error) {
      console.error("Error saving transaction:", error);
      Alert.alert('Erro', 'Não foi possível salvar a transação.');
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Nova Transação</Text>

      <View style={styles.typeSelector}>
        <TouchableOpacity
          style={[styles.typeButton, type === 'expense' && styles.typeButtonExpense]}
          onPress={() => setType('expense')}
        >
          <Text style={[styles.typeButtonText, type === 'expense' && styles.typeButtonTextActive]}>
            Despesa
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.typeButton, type === 'income' && styles.typeButtonIncome]}
          onPress={() => setType('income')}
        >
          <Text style={[styles.typeButtonText, type === 'income' && styles.typeButtonTextActive]}>
            Receita
          </Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.label}>Valor</Text>
      <TextInput
        style={styles.input}
        placeholder="R$ 0,00"
        keyboardType="numeric"
        value={amount}
        onChangeText={setAmount}
      />

      <Text style={styles.label}>Descrição</Text>
      <TextInput
        style={styles.input}
        placeholder="Ex: Supermercado"
        value={description}
        onChangeText={setDescription}
      />

      <Text style={styles.label}>Categoria</Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryScroll}>
        {categories.map((cat) => (
          <TouchableOpacity
            key={cat.id}
            style={[
              styles.categoryChip,
              { backgroundColor: selectedCategoryId === cat.id ? cat.color : '#eee' }
            ]}
            onPress={() => setSelectedCategoryId(cat.id)}
          >
            <Text
              style={[
                styles.categoryChipText,
                { color: selectedCategoryId === cat.id ? '#fff' : '#333' }
              ]}
            >
              {cat.name}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      <TouchableOpacity style={styles.saveButton} onPress={handleSave}>
        <Text style={styles.saveButtonText}>Salvar</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#333',
  },
  typeSelector: {
    flexDirection: 'row',
    marginBottom: 20,
    backgroundColor: '#eee',
    borderRadius: 10,
    padding: 5,
  },
  typeButton: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderRadius: 8,
  },
  typeButtonExpense: {
    backgroundColor: '#FF6347',
  },
  typeButtonIncome: {
    backgroundColor: '#32CD32',
  },
  typeButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#666',
  },
  typeButtonTextActive: {
    color: '#fff',
  },
  label: {
    fontSize: 16,
    marginBottom: 8,
    color: '#555',
    fontWeight: '500',
  },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 10,
    padding: 15,
    fontSize: 16,
    marginBottom: 20,
  },
  categoryScroll: {
    flexDirection: 'row',
    marginBottom: 30,
  },
  categoryChip: {
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 20,
    marginRight: 10,
  },
  categoryChipText: {
    fontWeight: '600',
  },
  saveButton: {
    backgroundColor: '#32CD32',
    paddingVertical: 15,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 10,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
