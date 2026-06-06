import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, Alert } from 'react-native';
import { useSQLiteContext } from 'expo-sqlite';
import { useAuth } from '../../context/AuthContext';

export default function PinScreen() {
  const db = useSQLiteContext();
  const { unlock } = useAuth();
  
  const [pin, setPin] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkExistingPin();
  }, []);

  const checkExistingPin = async () => {
    try {
      const result = await db.getAllAsync<{ id: number; pin: string }>('SELECT * FROM users LIMIT 1');
      if (result.length === 0) {
        setIsCreating(true);
      } else {
        setIsCreating(false);
      }
    } catch (error) {
      console.error("Error checking PIN:", error);
    } finally {
      setLoading(false);
    }
  };

  const handlePinSubmit = async () => {
    if (pin.length < 4) {
      Alert.alert('Erro', 'O PIN deve ter pelo menos 4 dígitos.');
      return;
    }

    try {
      if (isCreating) {
        await db.runAsync('INSERT INTO users (pin) VALUES (?)', [pin]);
        unlock();
      } else {
        const result = await db.getAllAsync<{ id: number; pin: string }>('SELECT * FROM users LIMIT 1');
        if (result.length > 0 && result[0].pin === pin) {
          unlock();
        } else {
          Alert.alert('Erro', 'PIN incorreto.');
          setPin('');
        }
      }
    } catch (error) {
      console.error("Error handling PIN:", error);
      Alert.alert('Erro', 'Ocorreu um erro ao processar o PIN.');
    }
  };

  if (loading) {
    return (
      <View style={styles.container}>
        <Text>Carregando...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>{isCreating ? 'Crie um PIN' : 'Digite seu PIN'}</Text>
      <TextInput
        style={styles.input}
        value={pin}
        onChangeText={setPin}
        keyboardType="numeric"
        secureTextEntry
        maxLength={6}
        placeholder="****"
      />
      <TouchableOpacity style={styles.button} onPress={handlePinSubmit}>
        <Text style={styles.buttonText}>{isCreating ? 'Salvar PIN' : 'Entrar'}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#333',
  },
  input: {
    width: '100%',
    height: 60,
    backgroundColor: '#fff',
    borderRadius: 10,
    paddingHorizontal: 20,
    fontSize: 24,
    textAlign: 'center',
    letterSpacing: 10,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  button: {
    backgroundColor: '#32CD32',
    paddingVertical: 15,
    paddingHorizontal: 40,
    borderRadius: 10,
    width: '100%',
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
