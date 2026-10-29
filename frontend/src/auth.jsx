import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, getToken, setToken } from './api.js';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(!!getToken());

  const refresh = useCallback(async () => {
    try {
      setUser(await api('/api/auth/me'));
    } catch {
      setToken(null);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (getToken()) refresh();
    const onLogout = () => setUser(null);
    window.addEventListener('index:logout', onLogout);
    return () => window.removeEventListener('index:logout', onLogout);
  }, [refresh]);

  async function login(email, password) {
    const res = await api('/api/auth/login', { method: 'POST', body: { email, password } });
    setToken(res.token);
    setUser(res.user);
    return res.user;
  }

  async function register(payload) {
    const res = await api('/api/auth/register', { method: 'POST', body: payload });
    setToken(res.token);
    setUser(res.user);
    return res.user;
  }

  function logout() {
    setToken(null);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}
