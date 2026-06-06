import React, { createContext, useState, useContext, ReactNode } from 'react';

interface AuthContextData {
  isUnlocked: boolean;
  hasPin: boolean;
  unlock: () => void;
  setHasPinState: (hasPin: boolean) => void;
  lock: () => void;
}

const AuthContext = createContext<AuthContextData>({} as AuthContextData);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isUnlocked, setIsUnlocked] = useState(false);
  const [hasPin, setHasPin] = useState(false);

  const unlock = () => setIsUnlocked(true);
  const lock = () => setIsUnlocked(false);
  const setHasPinState = (state: boolean) => setHasPin(state);

  return (
    <AuthContext.Provider value={{ isUnlocked, hasPin, unlock, lock, setHasPinState }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};
