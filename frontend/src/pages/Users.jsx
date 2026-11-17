import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, Field, Modal, PageHeader, Spinner, Table } from '../ui.jsx';

function AdminModal({ onClose, onDone }) {
  const { t } = useT();
  const [f, setF] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  async function submit(e) {
    e.preventDefault(); setErr('');
    try { await api('/api/admin/admins', { method: 'POST', body: f }); onDone(); } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={t('Add an admin')} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        <Field label={t('Full name')}><input className="input" value={f.fullName} onChange={set('fullName')} required /></Field>
        <Field label={t('Email')}><input className="input" type="email" value={f.email} onChange={set('email')} required /></Field>
        <Field label={t('Phone')}><input className="input" value={f.phone} onChange={set('phone')} /></Field>
        <Field label={t('Password')} hint={t('At least 8 characters. Share it with them privately.')}><input className="input" type="text" minLength={8} value={f.password} onChange={set('password')} required /></Field>
        <button className="btn-primary w-full">{t('Add an admin')}</button>
      </form>
    </Modal>
  );
}

export default function Users() {
  const { user: me } = useAuth();
  const { t } = useT();
  const [tab, setTab] = useState('pending');
  const [role, setRole] = useState('');
  const list = useLoad(() => api('/api/admin/users', { params: tab === 'pending' ? { approval: 'PENDING' } : { role } }), [tab, role]);
  const [err, setErr] = useState('');
  const [code, setCode] = useState(null);
  const [adminForm, setAdminForm] = useState(false);

  async function run(fn) {
    setErr('');
    try { await fn(); list.reload(); } catch (ex) { setErr(ex.message); }
  }
  const toggle = (u) => run(() => api(`/api/admin/users/${u.id}/active`, { method: 'PATCH', params: { value: !u.active } }));
  const approve = (u) => run(() => api(`/api/admin/users/${u.id}/approve`, { method: 'PATCH' }));
  const verifyPhone = (u) => run(() => api(`/api/admin/users/${u.id}/verify-phone`, { method: 'PATCH' }));
  const reject = (u) => { if (confirm(t('Reject this account?'))) run(() => api(`/api/admin/users/${u.id}/reject`, { method: 'PATCH' })); };
  async function resetCode(u) {
    setErr('');
    try { const r = await api(`/api/admin/users/${u.id}/reset-code`, { method: 'POST' }); setCode({ user: u, code: r.code }); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title={t('Users')} subtitle={t('Approve new accounts and manage existing ones.')}
        action={tab === 'all' && (
          <select className="input w-auto" value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="">{t('All roles')}</option><option value="SHOP">{t('Shops')}</option><option value="FARMER">{t('Farmers')}</option><option value="ADMIN">{t('Admins')}</option>
          </select>
        )} />
      <div className="mb-3"><button className="btn-secondary btn-sm" onClick={() => setAdminForm(true)}>+ {t('Add an admin')}</button></div>
      <div className="mb-4 flex gap-2">
        {[['pending', 'Waiting for approval'], ['all', 'All users']].map(([k, label]) => (
          <button key={k} onClick={() => setTab(k)} className={`rounded-lg border px-3 py-1.5 text-sm font-medium ${tab === k ? 'border-green-700 bg-green-50 text-green-900' : 'border-stone-300 bg-white text-stone-700'}`}>{t(label)}</button>
        ))}
      </div>
      <div className="mb-3"><Alert>{err}</Alert></div>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('Name'), t('Business'), t('Role'), t('Contact'), t('Location'), t('Status'), '']} empty={tab === 'pending' ? t('No accounts are waiting for approval.') : undefined}>
          {list.data.map((u) => (
            <tr key={u.id} className={u.active ? '' : 'opacity-60'}>
              <td className="td font-medium">{u.fullName}</td>
              <td className="td">{u.businessName || '—'}{u.cooperative ? ` (${t('Cooperative')})` : ''}</td>
              <td className="td">{t(u.role === 'SHOP' ? 'Shop' : u.role === 'FARMER' ? 'Farmer' : 'Admin')}</td>
              <td className="td">{u.email}<br /><span className="text-xs text-stone-500">{u.phone}</span>{u.phone && !u.phoneVerified && <span className="ml-1 text-xs text-amber-700">({t('not confirmed')})</span>}</td>
              <td className="td">{[u.district, u.sector].filter(Boolean).join(', ') || '—'}</td>
              <td className="td"><Badge value={u.approvalStatus} /></td>
              <td className="td whitespace-nowrap text-right">
                {u.approvalStatus === 'PENDING' && (
                  <>
                    <button className="btn-primary btn-sm mr-1" onClick={() => approve(u)}>{t('Approve')}</button>
                    <button className="btn-secondary btn-sm" onClick={() => reject(u)}>{t('Reject')}</button>
                  </>
                )}
                {u.approvalStatus !== 'PENDING' && u.id !== me.id && (
                  <>
                    {u.phone && !u.phoneVerified && <button className="btn-secondary btn-sm mr-1" onClick={() => verifyPhone(u)}>{t('Confirm phone')}</button>}
                    <button className="btn-secondary btn-sm mr-1" onClick={() => resetCode(u)}>{t('Reset code')}</button>
                    <button className="btn-secondary btn-sm" onClick={() => toggle(u)}>{u.active ? t('Deactivate') : t('Activate')}</button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </Table>
      )}
      {adminForm && <AdminModal onClose={() => setAdminForm(false)} onDone={() => { setAdminForm(false); list.reload(); }} />}
      {code && (
        <Modal title={t('One-time reset code')} onClose={() => setCode(null)}>
          <p className="text-sm text-stone-600">{t('Give this code to {name} by phone or WhatsApp. It works for 24 hours. They enter it on the Forgot password page.', { name: code.user.fullName })}</p>
          <div className="my-4 rounded-lg bg-stone-100 py-4 text-center text-3xl font-bold tracking-widest">{code.code}</div>
          <button className="btn-secondary w-full" onClick={() => navigator.clipboard?.writeText(code.code)}>{t('Copy code')}</button>
        </Modal>
      )}
    </>
  );
}
