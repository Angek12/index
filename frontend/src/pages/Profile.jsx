import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, Field, PageHeader } from '../ui.jsx';
import { DISTRICTS } from './Register.jsx';

export default function Profile() {
  const { user, refresh } = useAuth();
  const [f, setF] = useState({
    fullName: user.fullName || '', phone: user.phone || '', businessName: user.businessName || '',
    district: user.district || '', sector: user.sector || '', address: user.address || '' });
  const [msg, setMsg] = useState({ type: '', text: '' });
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const hasBusiness = user.role !== 'ADMIN';

  async function submit(e) {
    e.preventDefault(); setBusy(true); setMsg({ type: '', text: '' });
    try {
      await api('/api/auth/me', { method: 'PUT', body: f });
      await refresh();
      setMsg({ type: 'success', text: 'Profile saved.' });
    } catch (ex) { setMsg({ type: 'error', text: ex.message }); } finally { setBusy(false); }
  }

  return (
    <>
      <PageHeader title="Profile" subtitle={`${user.email} · ${user.role}`} />
      <form onSubmit={submit} className="card grid max-w-2xl gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2"><Alert type={msg.type || 'error'}>{msg.text}</Alert></div>
        <Field label="Full name"><input className="input" value={f.fullName} onChange={set('fullName')} required /></Field>
        <Field label="Phone"><input className="input" value={f.phone} onChange={set('phone')} /></Field>
        {hasBusiness && (
          <>
            <Field label={user.role === 'SHOP' ? 'Shop name' : 'Farm / cooperative name'}><input className="input" value={f.businessName} onChange={set('businessName')} required /></Field>
            <Field label="District">
              <input className="input" list="dist" value={f.district} onChange={set('district')} />
              <datalist id="dist">{DISTRICTS.map((d) => <option key={d} value={d} />)}</datalist>
            </Field>
            <Field label="Sector"><input className="input" value={f.sector} onChange={set('sector')} /></Field>
            <Field label="Address / landmark"><input className="input" value={f.address} onChange={set('address')} /></Field>
          </>
        )}
        <div className="sm:col-span-2"><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save changes'}</button></div>
      </form>
    </>
  );
}
