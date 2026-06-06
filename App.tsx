import React from 'react';
import { SQLiteProvider } from 'expo-sqlite';
import { AuthProvider } from './src/context/AuthContext';
import Navigation from './src/navigation';
import { initializeDatabase } from './src/database/schema';

export default function App() {
  return (
    <SQLiteProvider databaseName="cashtelo.db" onInit={initializeDatabase}>
      <AuthProvider>
        <Navigation />
      </AuthProvider>
    </SQLiteProvider>
  );
}
