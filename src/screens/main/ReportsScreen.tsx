import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { useSQLiteContext } from 'expo-sqlite';
import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';

interface Transaction {
  id: number;
  amount: number;
  type: string;
  description: string;
  date: string;
  category_name: string | null;
}

export default function ReportsScreen() {
  const db = useSQLiteContext();
  const [isGenerating, setIsGenerating] = useState(false);

  const generatePDF = async () => {
    try {
      setIsGenerating(true);

      const currentMonth = new Date().toISOString().slice(0, 7); // YYYY-MM
      const monthLabel = new Date().toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });

      // Buscar transações do mês com categoria
      const transactions = await db.getAllAsync<Transaction>(`
        SELECT 
          t.id,
          t.amount,
          t.type,
          t.description,
          t.date,
          c.name as category_name
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.id
        WHERE strftime('%Y-%m', t.date) = ?
        ORDER BY t.date DESC
      `, [currentMonth]);

      // Calcular totais
      let totalIncome = 0;
      let totalExpense = 0;
      transactions.forEach(t => {
        if (t.type === 'income') totalIncome += t.amount;
        if (t.type === 'expense') totalExpense += t.amount;
      });
      const balance = totalIncome - totalExpense;

      // Gerar linhas da tabela
      const rows = transactions.length > 0
        ? transactions.map(t => {
            const date = new Date(t.date).toLocaleDateString('pt-BR');
            const typeLabel = t.type === 'income' ? 'Receita' : 'Despesa';
            const color = t.type === 'income' ? '#22863a' : '#b31d28';
            const sign = t.type === 'income' ? '+' : '-';
            return `
              <tr>
                <td>${date}</td>
                <td>${t.description || '—'}</td>
                <td>${t.category_name || '—'}</td>
                <td style="color:${color}; font-weight:600;">${typeLabel}</td>
                <td style="color:${color}; font-weight:600; text-align:right;">${sign} R$ ${t.amount.toFixed(2)}</td>
              </tr>
            `;
          }).join('')
        : `<tr><td colspan="5" style="text-align:center; color:#999;">Nenhuma transação neste mês.</td></tr>`;

      const html = `
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="UTF-8" />
          <style>
            body { font-family: Arial, sans-serif; padding: 30px; color: #222; }
            h1 { color: #32CD32; font-size: 28px; margin-bottom: 4px; }
            h2 { color: #555; font-size: 16px; font-weight: normal; margin-top: 0; }
            .summary { display: flex; gap: 20px; margin: 24px 0; }
            .card { flex: 1; border-radius: 10px; padding: 16px 20px; }
            .card.income { background: #e6f9ee; border: 1px solid #b7e4c7; }
            .card.expense { background: #fdecea; border: 1px solid #f5c6c6; }
            .card.balance { background: #eef2ff; border: 1px solid #c5cffa; }
            .card-label { font-size: 13px; color: #666; margin-bottom: 6px; }
            .card-value { font-size: 22px; font-weight: bold; }
            .income .card-value { color: #22863a; }
            .expense .card-value { color: #b31d28; }
            .balance .card-value { color: #3451db; }
            table { width: 100%; border-collapse: collapse; margin-top: 10px; }
            th { background: #32CD32; color: #fff; padding: 10px 12px; text-align: left; font-size: 13px; }
            td { padding: 9px 12px; font-size: 13px; border-bottom: 1px solid #eee; }
            tr:nth-child(even) td { background: #f9f9f9; }
            .footer { margin-top: 30px; font-size: 12px; color: #aaa; text-align: center; }
          </style>
        </head>
        <body>
          <h1>Cashtelo</h1>
          <h2>Relatório de ${monthLabel.charAt(0).toUpperCase() + monthLabel.slice(1)}</h2>

          <div class="summary">
            <div class="card income">
              <div class="card-label">Total de Receitas</div>
              <div class="card-value">+ R$ ${totalIncome.toFixed(2)}</div>
            </div>
            <div class="card expense">
              <div class="card-label">Total de Despesas</div>
              <div class="card-value">- R$ ${totalExpense.toFixed(2)}</div>
            </div>
            <div class="card balance">
              <div class="card-label">Saldo do Mês</div>
              <div class="card-value">R$ ${balance.toFixed(2)}</div>
            </div>
          </div>

          <table>
            <thead>
              <tr>
                <th>Data</th>
                <th>Descrição</th>
                <th>Categoria</th>
                <th>Tipo</th>
                <th style="text-align:right;">Valor</th>
              </tr>
            </thead>
            <tbody>
              ${rows}
            </tbody>
          </table>

          <div class="footer">
            Gerado em ${new Date().toLocaleDateString('pt-BR')} às ${new Date().toLocaleTimeString('pt-BR')} • Cashtelo
          </div>
        </body>
        </html>
      `;

      const { uri } = await Print.printToFileAsync({ html });

      const canShare = await Sharing.isAvailableAsync();
      if (canShare) {
        await Sharing.shareAsync(uri, {
          mimeType: 'application/pdf',
          dialogTitle: `Relatório ${monthLabel}`,
          UTI: 'com.adobe.pdf',
        });
      } else {
        Alert.alert('PDF gerado', `Arquivo salvo em:\n${uri}`);
      }
    } catch (error) {
      console.error('Erro ao gerar PDF:', error);
      Alert.alert('Erro', 'Não foi possível gerar o relatório.');
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Relatórios Mensais</Text>
      <Text style={styles.description}>
        Gere um relatório detalhado em PDF contendo todas as transações do mês atual,
        com resumo de receitas, despesas e saldo.
      </Text>

      <TouchableOpacity
        style={[styles.button, isGenerating && styles.buttonDisabled]}
        onPress={generatePDF}
        disabled={isGenerating}
      >
        {isGenerating ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Gerar PDF do Mês</Text>
        )}
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
    marginBottom: 10,
    color: '#333',
  },
  description: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    marginBottom: 30,
    lineHeight: 24,
  },
  button: {
    backgroundColor: '#32CD32',
    paddingVertical: 15,
    paddingHorizontal: 40,
    borderRadius: 10,
    width: '100%',
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
