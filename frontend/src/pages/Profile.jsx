import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useLoad } from '../hooks.js';
import { LangSwitch, useT } from '../i18n.jsx';
import { Alert, Field, PageHeader, RepLine } from '../ui.jsx';
import { DISTRICTS } from './Register.jsx';

function Reputation() {
  const { t } = useT();
  const { data } = useLoad(() => api('/api/reputation/me'));
  if (!data) return null;
  return (
    <div className="card max-w-2xl">
      <h2 className="mb-1 font-semibold">{t('Your reputation')}</h2>
      <p className="mb-2 text-sm text-stone-500">{t('Based on completed and failed orders and the stars other people gave you.')}</p>
      <RepLine rep={data} />
    </div>
  );
}

export default function Profile() {
  const { user, refresh } = useAuth();
  const { t } = useT();
  const [f, setF] = useState({
    fullName: user.fullName || '', phone: user.phone || '', businessName: user.businessName || '',
    district: user.district || '', sector: user.sector || '', address: user.address || '',
    notifySms: user.notifySms, notifyWhatsapp: user.notifyWhatsapp });
  const [msg, setMsg] = useState({ type: '', text: '' });
  const [busy, setBusy] = useState(false);
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' });
  const [pwMsg, setPwMsg] = useState({ type: '', text: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const hasBusiness = user.role !== 'ADMIN';
  const isOwner = user.role === 'ADMIN' || user.teamRole === 'OWNER';

  async function submit(e) {
    e.preventDefault(); setBusy(true); setMsg({ type: '', text: '' });
    try {
      await api('/api/auth/me', { method: 'PUT', body: f });
      await refresh();
      setMsg({ type: 'success', text: 'Profile saved.' });
    } catch (ex) { setMsg({ type: 'error', text: ex.message }); } finally { setBusy(false); }
  }

  async function changePw(e) {
    e.preventDefault(); setPwMsg({ type: '', text: '' });
    try {
      await api('/api/auth/change-password', { method: 'POST', body: pw });
      setPw({ currentPassword: '', newPassword: '' });
      setPwMsg({ type: 'success', text: 'Password changed.' });
    } catch (ex) { setPwMsg({ type: 'error', text: ex.message }); }
  }

  return (
    <>
      <PageHeader title={t('Profile')} subtitle={`${user.email} · ${t(user.role === 'SHOP' ? 'Shop' : user.role === 'FARMER' ? 'Farmer' : 'Admin')}${user.cooperative ? ' · ' + t('Cooperative') : ''}`} />
      <div className="space-y-5">
        <form onSubmit={submit} className="card grid max-w-2xl gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><Alert type={msg.type || 'error'}>{msg.text}</Alert></div>
          <Field label={t('Full name')}><input className="input" value={f.fullName} onChange={set('fullName')} required /></Field>
          <Field label={t('Phone')}><input className="input" value={f.phone} onChange={set('phone')} /></Field>
          {hasBusiness && isOwner && (
            <>
              <Field label={user.role === 'SHOP' ? t('Shop name') : t('Farm / cooperative name')}><input className="input" value={f.businessName} onChange={set('businessName')} required /></Field>
              <Field label={t('District')}>
                <input className="input" list="dist" value={f.district} onChange={set('district')} />
                <datalist id="dist">{DISTRICTS.map((d) => <option key={d} value={d} />)}</datalist>
              </Field>
              <Field label={t('Sector')}><input className="input" value={f.sector} onChange={set('sector')} /></Field>
              <Field label={t('Address / landmark')}><input className="input" value={f.address} onChange={set('address')} /></Field>
            </>
          )}
          <div className="space-y-2 sm:col-span-2">
            <div className="text-sm font-medium">{t('Language')}</div>
            <LangSwitch onChange={(l) => api('/api/auth/me', { method: 'PUT', body: { language: l } }).then(refresh).catch(() => {})} />
            <div className="pt-2 text-sm font-medium">{t('Message alerts')}</div>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.notifySms} onChange={(e) => setF({ ...f, notifySms: e.target.checked })} /> {t('Send me SMS alerts')}</label>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.notifyWhatsapp} onChange={(e) => setF({ ...f, notifyWhatsapp: e.target.checked })} /> {t('Send me WhatsApp alerts')}</label>
          </div>
          <div className="sm:col-span-2"><button className="btn-primary" disabled={busy}>{busy ? t('Saving…') : t('Save changes')}</button></div>
        </form>

        {hasBusiness && <Reputation />}

        <form onSubmit={changePw} className="card grid max-w-2xl gap-4 sm:grid-cols-2">
          <h2 className="font-semibold sm:col-span-2">{t('Change password')}</h2>
          <div className="sm:col-span-2"><Alert type={pwMsg.type || 'error'}>{pwMsg.text}</Alert></div>
          <Field label={t('Current password')}><input className="input" type="password" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} required /></Field>
          <Field label={t('New password')} hint={t('At least 8 characters')}><input className="input" type="password" minLength={8} value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} required /></Field>
          <div className="sm:col-span-2"><button className="btn-secondary">{t('Change password')}</button></div>
        </form>
      </div>
    </>
  );
}
