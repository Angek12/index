import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { LangSwitch, useT } from '../i18n.jsx';
import { Alert, Field } from '../ui.jsx';

export default function Forgot() {
  const { t } = useT();
  const nav = useNavigate();
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [pw, setPw] = useState('');
  const [err, setErr] = useState('');
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  async function askCode(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      await api('/api/auth/forgot-password', { method: 'POST', body: { email } });
      setStep(2);
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function reset(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      await api('/api/auth/reset-password', { method: 'POST', body: { email, code, newPassword: pw } });
      setMsg('Password changed. You can sign in now.');
      setTimeout(() => nav('/login'), 1500);
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-stone-50 p-4">
      <div className="card w-full max-w-sm space-y-4 p-6">
        <div className="flex justify-end"><LangSwitch /></div>
        <h1 className="text-center text-xl font-bold text-green-800">{t('Reset your password')}</h1>
        <Alert>{err}</Alert>
        <Alert type="success">{msg}</Alert>
        {step === 1 ? (
          <form onSubmit={askCode} className="space-y-4">
            <p className="text-sm text-stone-600">{t('Enter your email. If your account has a phone number, we send you a 6-digit code by SMS.')}</p>
            <Field label={t('Email')}><input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus /></Field>
            <button className="btn-primary w-full" disabled={busy}>{busy ? t('Sending…') : t('Send me a code')}</button>
            <button type="button" className="w-full text-center text-sm text-green-700 underline" onClick={() => setStep(2)}>{t('I already have a code from the Index team')}</button>
          </form>
        ) : (
          <form onSubmit={reset} className="space-y-4">
            <p className="text-sm text-stone-600">{t('Enter the 6-digit code and choose a new password.')}</p>
            <Field label={t('Email')}><input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></Field>
            <Field label={t('6-digit code')}><input className="input" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(e.target.value)} required /></Field>
            <Field label={t('New password')} hint={t('At least 8 characters')}><input className="input" type="password" minLength={8} value={pw} onChange={(e) => setPw(e.target.value)} required /></Field>
            <button className="btn-primary w-full" disabled={busy}>{busy ? t('Saving…') : t('Change password')}</button>
          </form>
        )}
        <p className="text-center text-sm"><Link to="/login" className="text-green-700 underline">{t('Back to sign in')}</Link></p>
      </div>
    </div>
  );
}
