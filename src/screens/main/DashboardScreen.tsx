import React, { useState, useCallback } from 'react';
import { View, Text, StyleSheet, ScrollView, Dimensions, ActivityIndicator } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useSQLiteContext } from 'expo-sqlite';
import { PieChart } from 'react-native-chart-kit';

const screenWidth = Dimensions.get('window').width;

interface ChartData {
  name: string;
  total: number;
  color: string;
  legendFontColor: string;
  legendFontSize: number;
}

export default function DashboardScreen() {
  const db = useSQLiteContext();
  const [balance, setBalance] = useState(0);
  const [income, setIncome] = useState(0);
  const [expense, setExpense] = useState(0);
  const [chartData, setChartData] = useState<ChartData[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const currentMonth = new Date().toISOString().slice(0, 7); // YYYY-MM
      
      // Calculate totals
      const totalsResult = await db.getAllAsync<{ type: string; total: number }>(`
        SELECT type, SUM(amount) as total 
        FROM transactions 
        WHERE strftime('%Y-%m', date) = ?
        GROUP BY type
      `, [currentMonth]);

      let totalIncome = 0;
      let totalExpense = 0;

      totalsResult.forEach(row => {
        if (row.type === 'income') totalIncome = row.total;
        if (row.type === 'expense') totalExpense = row.total;
      });

      setIncome(totalIncome);
      setExpense(totalExpense);
      setBalance(totalIncome - totalExpense);

      // Fetch expenses grouped by category for the chart
      const chartResult = await db.getAllAsync<{ name: string; color: string; total: number }>(`
        SELECT c.name, c.color, SUM(t.amount) as total
        FROM transactions t
        JOIN categories c ON t.category_id = c.id
        WHERE t.type = 'expense' AND strftime('%Y-%m', t.date) = ?
        GROUP BY c.id
      `, [currentMonth]);

      const formattedChartData = chartResult.map(item => ({
        name: item.name,
        total: item.total,
        color: item.color,
        legendFontColor: '#7F7F7F',
        legendFontSize: 12,
      }));

      setChartData(formattedChartData);
    } catch (error) {
      console.error("Error fetching dashboard data:", error);
    } finally {
      setLoading(false);
    }
  };

  useFocusEffect(
    useCallback(() => {
      fetchDashboardData();
    }, [])
  );

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#32CD32" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Saldo Atual</Text>
        <Text style={[styles.balanceAmount, { color: balance >= 0 ? '#32CD32' : '#FF6347' }]}>
          R$ {balance.toFixed(2)}
        </Text>
        <View style={styles.row}>
          <View style={styles.statBox}>
            <Text style={styles.statLabel}>Receitas</Text>
            <Text style={styles.incomeText}>+ R$ {income.toFixed(2)}</Text>
          </View>
          <View style={styles.statBox}>
            <Text style={styles.statLabel}>Despesas</Text>
            <Text style={styles.expenseText}>- R$ {expense.toFixed(2)}</Text>
          </View>
        </View>
      </View>

      <Text style={styles.sectionTitle}>Despesas por Categoria</Text>
      {chartData.length > 0 ? (
        <View style={styles.chartContainer}>
          <PieChart
            data={chartData}
            width={screenWidth - 40}
            height={220}
            chartConfig={{
              color: (opacity = 1) => `rgba(0, 0, 0, ${opacity})`,
            }}
            accessor={"total"}
            backgroundColor={"transparent"}
            paddingLeft={"15"}
            center={[10, 0]}
            absolute
          />
        </View>
      ) : (
        <View style={styles.emptyContainer}>
          <Text style={styles.emptyText}>Nenhuma despesa registrada este mês.</Text>
        </View>
      )}
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
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 15,
    padding: 20,
    marginBottom: 25,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 5 },
    elevation: 3,
  },
  cardTitle: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  balanceAmount: {
    fontSize: 36,
    fontWeight: 'bold',
    textAlign: 'center',
    marginVertical: 10,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 15,
  },
  statBox: {
    alignItems: 'center',
  },
  statLabel: {
    fontSize: 14,
    color: '#999',
    marginBottom: 5,
  },
  incomeText: {
    fontSize: 16,
    color: '#32CD32',
    fontWeight: '600',
  },
  expenseText: {
    fontSize: 16,
    color: '#FF6347',
    fontWeight: '600',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 15,
  },
  chartContainer: {
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 15,
    paddingVertical: 10,
  },
  emptyContainer: {
    alignItems: 'center',
    padding: 20,
  },
  emptyText: {
    color: '#999',
    fontSize: 16,
  },
});
