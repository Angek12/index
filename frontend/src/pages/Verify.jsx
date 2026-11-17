import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { LangSwitch, useT } from '../i18n.jsx';
import { Alert, Field } from '../ui.jsx';

export default function Verify() {
  const { verifyPhone } = useAuth();
  const { t } = useT();
  const nav = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState(params.get('email') || '');
  const [code, setCode] = useState('');
  const [err, setErr] = useState('');
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setErr(''); setMsg(''); setBusy(true);
    try {
      const res = await verifyPhone(email, code);
      if (res.token) nav('/dashboard');
      else setMsg(res.message || 'Phone confirmed.');
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function resend() {
    setErr(''); setMsg('');
    try {
      await api('/api/auth/resend-code', { method: 'POST', body: { email } });
      setMsg('If the number is waiting for confirmation, a new code was sent.');
    } catch (ex) { setErr(ex.message); }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-stone-50 p-4">
      <form onSubmit={submit} className="card w-full max-w-sm space-y-4 p-6">
        <div className="flex justify-end"><LangSwitch /></div>
        <h1 className="text-center text-xl font-bold text-green-800">{t('Confirm your phone number')}</h1>
        <p className="text-sm text-stone-600">{t('We sent a 6-digit code to your phone by SMS. Enter it below.')}</p>
        <Alert>{err}</Alert>
        <Alert type="success">{msg}</Alert>
        <Field label={t('Email')}><input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></Field>
        <Field label={t('6-digit code')}><input className="input" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(e.target.value)} required autoFocus /></Field>
        <button className="btn-primary w-full" disabled={busy}>{busy ? t('Checking…') : t('Confirm')}</button>
        <button type="button" onClick={resend} className="w-full text-center text-sm text-green-700 underline">{t('Send a new code')}</button>
        <p className="text-center text-sm"><Link to="/login" className="text-green-700 underline">{t('Back to sign in')}</Link></p>
      </form>
    </div>
  );
}
