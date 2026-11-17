import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Field, PageHeader, Spinner, Table } from '../ui.jsx';

export default function Team() {
  const { user } = useAuth();
  const { t } = useT();
  const list = useLoad(() => api('/api/team'));
  const [f, setF] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');
  const [busy, setBusy] = useState(false);
  const isOwner = user.teamRole === 'OWNER';
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function add(e) {
    e.preventDefault(); setErr(''); setOk(''); setBusy(true);
    try {
      await api('/api/team', { method: 'POST', body: f });
      setF({ fullName: '', email: '', phone: '', password: '' });
      setOk('Team member added. They can sign in with the email and password you set.');
      list.reload();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function remove(m) {
    if (!confirm(t('Remove {name} from your team?', { name: m.fullName }))) return;
    try { await api(`/api/team/${m.userId}`, { method: 'DELETE' }); list.reload(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title={t('Team')} subtitle={user.role === 'FARMER' && user.cooperative ? t('Members of your cooperative share the same listings and orders.') : t('People who work with you share the same records and orders.')} />
      {isOwner && (
        <form onSubmit={add} className="card mb-5 grid gap-3 sm:grid-cols-2">
          <h2 className="font-semibold sm:col-span-2">{t('Add a team member')}</h2>
          <div className="sm:col-span-2"><Alert>{err}</Alert><Alert type="success">{ok}</Alert></div>
          <Field label={t('Full name')}><input className="input" value={f.fullName} onChange={set('fullName')} required /></Field>
          <Field label={t('Phone')}><input className="input" value={f.phone} onChange={set('phone')} /></Field>
          <Field label={t('Email')}><input className="input" type="email" value={f.email} onChange={set('email')} required /></Field>
          <Field label={t('Password')} hint={t('At least 8 characters. Share it with them privately.')}><input className="input" type="text" minLength={8} value={f.password} onChange={set('password')} required /></Field>
          <div className="sm:col-span-2"><button className="btn-primary" disabled={busy}>{busy ? t('Saving…') : t('Add member')}</button></div>
        </form>
      )}
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('Name'), t('Contact'), t('Role'), '']}>
          {list.data.map((m) => (
            <tr key={m.userId} className={m.active ? '' : 'opacity-50'}>
              <td className="td font-medium">{m.fullName}</td>
              <td className="td">{m.email}<br /><span className="text-xs text-stone-500">{m.phone}</span></td>
              <td className="td">{m.teamRole === 'OWNER' ? t('Owner') : t('Member')}</td>
              <td className="td text-right">{isOwner && m.teamRole !== 'OWNER' && m.active && <button className="btn-secondary btn-sm" onClick={() => remove(m)}>{t('Remove')}</button>}</td>
            </tr>
          ))}
        </Table>
      )}
    </>
  );
}
