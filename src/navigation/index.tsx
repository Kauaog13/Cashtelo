import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Home, PlusCircle, FileText, User } from 'lucide-react-native';

import DashboardScreen from '../screens/main/DashboardScreen';
import AddTransactionScreen from '../screens/main/AddTransactionScreen';
import ReportsScreen from '../screens/main/ReportsScreen';
import ProfileScreen from '../screens/main/ProfileScreen';
import PinScreen from '../screens/auth/PinScreen';
import { useAuth } from '../context/AuthContext';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ color, size }) => {
          if (route.name === 'Dashboard') return <Home color={color} size={size} />;
          if (route.name === 'Adicionar') return <PlusCircle color={color} size={size} />;
          if (route.name === 'Relatórios') return <FileText color={color} size={size} />;
          if (route.name === 'Perfil') return <User color={color} size={size} />;
        },
        tabBarActiveTintColor: '#32CD32',
        tabBarInactiveTintColor: 'gray',
        headerShown: true,
      })}
    >
      <Tab.Screen name="Dashboard" component={DashboardScreen} />
      <Tab.Screen name="Adicionar" component={AddTransactionScreen} />
      <Tab.Screen name="Relatórios" component={ReportsScreen} />
      <Tab.Screen name="Perfil" component={ProfileScreen} />
    </Tab.Navigator>
  );
}

export default function Navigation() {
  const { isUnlocked } = useAuth();

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {!isUnlocked ? (
          <Stack.Screen name="Pin" component={PinScreen} />
        ) : (
          <Stack.Screen name="Main" component={MainTabs} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
