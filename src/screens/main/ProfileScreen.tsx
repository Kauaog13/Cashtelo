import React, { useState, useCallback } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { useSQLiteContext } from 'expo-sqlite';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../../context/AuthContext';

interface Category {
  id: number;
  name: string;
  color: string;
}

export default function ProfileScreen() {
  const db = useSQLiteContext();
  const { lock } = useAuth();
  const [categories, setCategories] = useState<Category[]>([]);

  const fetchCategories = async () => {
    try {
      const cats = await db.getAllAsync<Category>('SELECT * FROM categories');
      setCategories(cats);
    } catch (error) {
      console.error("Error fetching categories:", error);
    }
  };

  useFocusEffect(
    useCallback(() => {
      fetchCategories();
    }, [])
  );

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.sectionTitle}>Minhas Categorias</Text>
      
      <View style={styles.categoriesList}>
        {categories.map((cat) => (
          <View key={cat.id} style={styles.categoryItem}>
            <View style={[styles.colorDot, { backgroundColor: cat.color }]} />
            <Text style={styles.categoryName}>{cat.name}</Text>
          </View>
        ))}
      </View>

      <View style={styles.settingsSection}>
        <Text style={styles.sectionTitle}>Segurança</Text>
        <TouchableOpacity style={styles.lockButton} onPress={lock}>
          <Text style={styles.lockButtonText}>Bloquear Aplicativo</Text>
        </TouchableOpacity>
      </View>
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
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 15,
    marginTop: 10,
  },
  categoriesList: {
    backgroundColor: '#fff',
    borderRadius: 15,
    padding: 15,
    marginBottom: 30,
  },
  categoryItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  colorDot: {
    width: 15,
    height: 15,
    borderRadius: 7.5,
    marginRight: 15,
  },
  categoryName: {
    fontSize: 16,
    color: '#444',
  },
  settingsSection: {
    marginTop: 20,
  },
  lockButton: {
    backgroundColor: '#FF6347',
    paddingVertical: 15,
    borderRadius: 10,
    alignItems: 'center',
  },
  lockButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});
