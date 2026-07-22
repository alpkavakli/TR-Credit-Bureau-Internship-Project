import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('token');
    const email = localStorage.getItem('email');
    const role = localStorage.getItem('role');
    return token ? { token, email, role } : null;
  });

  const login = (data) => {
    localStorage.setItem('token', data.token);
    // Refresh token backend'den geldiğinde saklıyoruz; axiosInstance 401'de bununla
    // sessizce yeni access token alır.
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    localStorage.setItem('email', data.email);
    localStorage.setItem('role', data.role);
    setUser({ token: data.token, email: data.email, role: data.role });
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
