import { SQLiteDatabase } from 'expo-sqlite';

export async function initializeDatabase(db: SQLiteDatabase) {
  // Configuração pragmas (Foreign keys)
  await db.execAsync(`PRAGMA foreign_keys = ON;`);

  // Tabelas
  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      pin TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS accounts (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS categories (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      color TEXT NOT NULL,
      icon TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      amount REAL NOT NULL,
      type TEXT CHECK( type IN ('income','expense','transfer') ) NOT NULL,
      description TEXT,
      date TEXT NOT NULL,
      category_id INTEGER,
      account_id INTEGER NOT NULL,
      FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL,
      FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE
    );
  `);

  // Seed initial data se necessário (Conta Padrão e Categorias Básicas)
  const accountsResult = await db.getAllAsync<{id: number}>('SELECT id FROM accounts LIMIT 1');
  if (accountsResult.length === 0) {
    await db.runAsync('INSERT INTO accounts (name) VALUES (?)', ['Carteira Principal']);
    
    // Categorias padrão
    await db.execAsync(`
      INSERT INTO categories (name, color, icon) VALUES 
        ('Alimentação', '#FF6347', 'utensils'),
        ('Transporte', '#4682B4', 'car'),
        ('Moradia', '#32CD32', 'home'),
        ('Salário', '#FFD700', 'wallet'),
        ('Lazer', '#8A2BE2', 'smile');
    `);
  }
}
