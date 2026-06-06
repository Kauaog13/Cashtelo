export type TransactionType = 'income' | 'expense' | 'transfer';

export interface User {
  id: number;
  pin: string;
}

export interface Account {
  id: number;
  name: string;
}

export interface Category {
  id: number;
  name: string;
  color: string;
  icon: string;
}

export interface Transaction {
  id: number;
  amount: number;
  type: TransactionType;
  description: string;
  date: string; // ISO format YYYY-MM-DD
  category_id: number;
  account_id: number;
}
