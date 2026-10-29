import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { Alert, Field } from '../ui.jsx';

export const DISTRICTS = ['Bugesera','Burera','Gakenke','Gasabo','Gatsibo','Gicumbi','Gisagara','Huye','Kamonyi','Karongi','Kayonza','Kicukiro','Kirehe','Muhanga','Musanze','Ngoma','Ngororero','Nyabihu','Nyagatare','Nyamagabe','Nyamasheke','Nyanza','Nyarugenge','Nyaruguru','Rubavu','Ruhango','Rulindo','Rusizi','Rutsiro','Rwamagana'];

export default function Register() {
  const { register } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ role: 'SHOP', fullName: '', email: '', phone: '', password: '', businessName: '', district: '', sector: '', address: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      await register(f);
      nav('/dashboard');
    } catch (ex) {
      setErr(ex.message);
    } finally {
      setBusy(false);
    }
  }

  const shop = f.role === 'SHOP';
  return (
    <div className="flex min-h-screen items-center justify-center bg-stone-50 p-4">
      <form onSubmit={submit} className="card w-full max-w-lg space-y-4 p-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-green-800">🌱 Create your Index account</h1>
        </div>
        <div className="grid grid-cols-2 gap-2">
          {[['SHOP', 'I run a shop'], ['FARMER', "I'm a farmer"]].map(([r, label]) => (
            <button type="button" key={r} onClick={() => setF({ ...f, role: r })}
              className={`rounded-lg border px-3 py-2 text-sm font-medium ${f.role === r ? 'border-green-700 bg-green-50 text-green-900' : 'border-stone-300 bg-white text-stone-700'}`}>
              {label}
            </button>
          ))}
        </div>
        <Alert>{err}</Alert>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Full name"><input className="input" value={f.fullName} onChange={set('fullName')} required /></Field>
          <Field label="Phone"><input className="input" value={f.phone} onChange={set('phone')} placeholder="07…" /></Field>
          <Field label="Email"><input className="input" type="email" value={f.email} onChange={set('email')} required /></Field>
          <Field label="Password" hint="At least 8 characters"><input className="input" type="password" minLength={8} value={f.password} onChange={set('password')} required /></Field>
          <Field label={shop ? 'Shop name' : 'Farm / cooperative name'}><input className="input" value={f.businessName} onChange={set('businessName')} required /></Field>
          <Field label="District">
            <input className="input" list="districts" value={f.district} onChange={set('district')} />
            <datalist id="districts">{DISTRICTS.map((d) => <option key={d} value={d} />)}</datalist>
          </Field>
          <Field label="Sector"><input className="input" value={f.sector} onChange={set('sector')} /></Field>
          <Field label="Address / landmark"><input className="input" value={f.address} onChange={set('address')} /></Field>
        </div>
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
        <p className="text-center text-sm text-stone-600">Already registered? <Link to="/login" className="font-medium text-green-700 underline">Sign in</Link></p>
      </form>
    </div>
  );
}
