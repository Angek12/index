import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, getToken, setToken } from './api.js';
import { useT } from './i18n.jsx';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const { lang, setLang } = useT();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(!!getToken());

  // Use the saved language only if this device has no choice of its own yet.
  const adoptLanguage = useCallback((u) => {
    if (u && u.language === 'rw' && !localStorage.getItem('index_lang')) setLang('rw');
  }, [setLang]);

  const refresh = useCallback(async () => {
    try {
      const u = await api('/api/auth/me');
      setUser(u);
      adoptLanguage(u);
    } catch {
      setToken(null);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, [adoptLanguage]);

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
    adoptLanguage(res.user);
    return res.user;
  }

  /** Returns the server answer. token is null while the account waits for approval. */
  async function register(payload) {
    const res = await api('/api/auth/register', { method: 'POST', body: { ...payload, language: lang } });
    if (res.token) {
      setToken(res.token);
      setUser(res.user);
    }
    return res;
  }

  /** Confirms the phone number. Signs the person in when the account is ready. */
  async function verifyPhone(email, code) {
    const res = await api('/api/auth/verify-phone', { method: 'POST', body: { email, code } });
    if (res.token) {
      setToken(res.token);
      setUser(res.user);
    }
    return res;
  }

  function logout() {
    setToken(null);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, verifyPhone, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}
