import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { LangSwitch, useT } from '../i18n.jsx';
import { Alert, Field } from '../ui.jsx';

export const DISTRICTS = ['Bugesera','Burera','Gakenke','Gasabo','Gatsibo','Gicumbi','Gisagara','Huye','Kamonyi','Karongi','Kayonza','Kicukiro','Kirehe','Muhanga','Musanze','Ngoma','Ngororero','Nyabihu','Nyagatare','Nyamagabe','Nyamasheke','Nyanza','Nyarugenge','Nyaruguru','Rubavu','Ruhango','Rulindo','Rusizi','Rutsiro','Rwamagana'];

export default function Register() {
  const { register } = useAuth();
  const { t } = useT();
  const nav = useNavigate();
  const [f, setF] = useState({ role: 'SHOP', fullName: '', email: '', phone: '', password: '', businessName: '', district: '', sector: '', address: '', cooperative: false, acceptTerms: false });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      const res = await register(f);
      if (res.phoneVerificationRequired) nav(`/verify?email=${encodeURIComponent(f.email)}`);
      else if (!res.token) setPending(true); // otherwise the app signs the person in
    } catch (ex) {
      setErr(ex.message);
    } finally {
      setBusy(false);
    }
  }

  if (pending) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-stone-50 p-4">
        <div className="card w-full max-w-md space-y-4 p-6 text-center">
          <div className="text-3xl">⏳</div>
          <h1 className="text-xl font-bold text-green-800">{t('Thank you. Your account is waiting for approval.')}</h1>
          <p className="text-sm text-stone-600">{t('The Index team will check your details. You will get a message when you can sign in.')}</p>
          <Link to="/login" className="btn-primary w-full">{t('Back to sign in')}</Link>
        </div>
      </div>
    );
  }

  const shop = f.role === 'SHOP';
  return (
    <div className="flex min-h-screen items-center justify-center bg-stone-50 p-4">
      <form onSubmit={submit} className="card w-full max-w-lg space-y-4 p-6">
        <div className="flex justify-end"><LangSwitch /></div>
        <div className="text-center">
          <h1 className="text-2xl font-bold text-green-800">🌱 {t('Create your Index account')}</h1>
        </div>
        <div className="grid grid-cols-2 gap-2">
          {[['SHOP', 'I run a shop'], ['FARMER', "I'm a farmer"]].map(([r, label]) => (
            <button type="button" key={r} onClick={() => setF({ ...f, role: r })}
              className={`rounded-lg border px-3 py-2 text-sm font-medium ${f.role === r ? 'border-green-700 bg-green-50 text-green-900' : 'border-stone-300 bg-white text-stone-700'}`}>
              {t(label)}
            </button>
          ))}
        </div>
        <Alert>{err}</Alert>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={t('Full name')}><input className="input" value={f.fullName} onChange={set('fullName')} required /></Field>
          <Field label={t('Phone')} hint={t('Used for order contact and SMS alerts')}><input className="input" value={f.phone} onChange={set('phone')} placeholder="07…" /></Field>
          <Field label={t('Email')}><input className="input" type="email" value={f.email} onChange={set('email')} required /></Field>
          <Field label={t('Password')} hint={t('At least 8 characters')}><input className="input" type="password" minLength={8} value={f.password} onChange={set('password')} required /></Field>
          <Field label={shop ? t('Shop name') : t('Farm / cooperative name')}><input className="input" value={f.businessName} onChange={set('businessName')} required /></Field>
          <Field label={t('District')}>
            <input className="input" list="districts" value={f.district} onChange={set('district')} />
            <datalist id="districts">{DISTRICTS.map((d) => <option key={d} value={d} />)}</datalist>
          </Field>
          <Field label={t('Sector')}><input className="input" value={f.sector} onChange={set('sector')} /></Field>
          <Field label={t('Address / landmark')}><input className="input" value={f.address} onChange={set('address')} /></Field>
        </div>
        {!shop && (
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={f.cooperative} onChange={(e) => setF({ ...f, cooperative: e.target.checked })} />
            {t('This is a cooperative with several members')}
          </label>
        )}
        <label className="flex items-start gap-2 text-sm">
          <input type="checkbox" className="mt-1" checked={f.acceptTerms} onChange={(e) => setF({ ...f, acceptTerms: e.target.checked })} required />
          <span>{t('I accept the terms and privacy notice')} (<Link to="/terms" target="_blank" className="text-green-700 underline">{t('read them')}</Link>)</span>
        </label>
        <button className="btn-primary w-full" disabled={busy}>{busy ? t('Creating…') : t('Create account')}</button>
        <p className="text-center text-sm text-stone-600">{t('Already registered?')} <Link to="/login" className="font-medium text-green-700 underline">{t('Sign in')}</Link></p>
      </form>
    </div>
  );
}
