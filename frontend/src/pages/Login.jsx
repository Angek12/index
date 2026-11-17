import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { LangSwitch, useT } from '../i18n.jsx';
import { Alert, Field } from '../ui.jsx';

export default function Login() {
  const { login } = useAuth();
  const { t } = useT();
  const nav = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      await login(email, password);
      nav('/dashboard');
    } catch (ex) {
      setErr(ex.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-stone-50 p-4">
      <form onSubmit={submit} className="card w-full max-w-sm space-y-4 p-6">
        <div className="flex justify-end"><LangSwitch /></div>
        <div className="text-center">
          <div className="text-3xl">🌱</div>
          <h1 className="text-2xl font-bold text-green-800">Index</h1>
          <p className="text-sm text-stone-500">{t('Connecting shops and farmers')}</p>
        </div>
        <Alert>{err}</Alert>
        {err.startsWith('Please confirm your phone') && <p className="text-center text-sm"><Link to={`/verify?email=${encodeURIComponent(email)}`} className="font-medium text-green-700 underline">{t('Enter my confirmation code')}</Link></p>}
        <Field label={t('Email')}><input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus /></Field>
        <Field label={t('Password')}><input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></Field>
        <button className="btn-primary w-full" disabled={busy}>{busy ? t('Signing in…') : t('Sign in')}</button>
        <p className="text-center text-sm"><Link to="/forgot" className="text-green-700 underline">{t('Forgot your password?')}</Link></p>
        <p className="text-center text-sm text-stone-600">{t('New here?')} <Link to="/register" className="font-medium text-green-700 underline">{t('Create an account')}</Link></p>
      </form>
    </div>
  );
}
