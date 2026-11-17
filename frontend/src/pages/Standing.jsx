import { useState } from 'react';
import { api } from '../api.js';
import { addDays, fmt, fmtDate, rwf, today, useCrops, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, CropSelect, Field, PageHeader, Spinner, Table } from '../ui.jsx';

const FREQ = { WEEKLY: 'Every week', EVERY_2_WEEKS: 'Every 2 weeks', MONTHLY: 'Every month' };

export default function Standing() {
  const { t } = useT();
  const crops = useCrops();
  const list = useLoad(() => api('/api/standing-orders'));
  const [f, setF] = useState({ cropId: '', quantity: '', maxPrice: '', frequency: 'WEEKLY', firstNeededDate: addDays(3), leadDays: 2, notes: '' });
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault(); setErr(''); setOk(''); setBusy(true);
    try {
      await api('/api/standing-orders', { method: 'POST', body: {
        cropId: Number(f.cropId), quantity: Number(f.quantity), maxPrice: f.maxPrice === '' ? null : Number(f.maxPrice),
        frequency: f.frequency, firstNeededDate: f.firstNeededDate, leadDays: Number(f.leadDays), notes: f.notes || null } });
      setF({ ...f, quantity: '', maxPrice: '', notes: '' });
      setOk('Saved. A request is sent automatically a few days before each date.');
      list.reload();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function toggle(s) {
    try { await api(`/api/standing-orders/${s.id}/active`, { method: 'PATCH', params: { value: !s.active } }); list.reload(); } catch (ex) { setErr(ex.message); }
  }
  async function remove(s) {
    if (!confirm(t('Delete this repeating request?'))) return;
    try { await api(`/api/standing-orders/${s.id}`, { method: 'DELETE' }); list.reload(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title={t('Repeating requests')} subtitle={t('Ask for the same produce on a schedule, for example 50 kg of tomatoes every Monday.')} />
      <form onSubmit={submit} className="card mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Field label={t('Crop')}><CropSelect crops={crops.data} value={f.cropId} onChange={set('cropId')} /></Field>
        <Field label={t('Quantity needed')}><input className="input" type="number" step="0.01" min="0.01" value={f.quantity} onChange={set('quantity')} required /></Field>
        <Field label={t('Max price per unit (RWF)')} hint={t('Optional')}><input className="input" type="number" min="0" value={f.maxPrice} onChange={set('maxPrice')} /></Field>
        <Field label={t('How often?')}>
          <select className="input" value={f.frequency} onChange={set('frequency')}>
            {Object.entries(FREQ).map(([k, v]) => <option key={k} value={k}>{t(v)}</option>)}
          </select>
        </Field>
        <Field label={t('First date needed')}><input className="input" type="date" min={today()} value={f.firstNeededDate} onChange={set('firstNeededDate')} required /></Field>
        <Field label={t('Send the request how many days before?')}><input className="input" type="number" min="0" max="14" value={f.leadDays} onChange={set('leadDays')} required /></Field>
        <div className="sm:col-span-2"><Field label={t('Notes')}><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} /></Field></div>
        <div className="sm:col-span-2 lg:col-span-4 flex items-center gap-3">
          <button className="btn-primary" disabled={busy}>{busy ? t('Saving…') : t('Save repeating request')}</button>
        </div>
        <div className="sm:col-span-2 lg:col-span-4"><Alert>{err}</Alert><Alert type="success">{ok}</Alert></div>
      </form>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('Crop'), t('Quantity'), t('How often?'), t('Next date needed'), t('Status'), '']} empty={t('You have no repeating requests yet.')}>
          {list.data.map((s) => (
            <tr key={s.id} className={s.active ? '' : 'opacity-60'}>
              <td className="td font-medium">{s.cropName}{s.maxPrice != null && <div className="text-xs font-normal text-stone-500">{t('Max price')} {rwf(s.maxPrice)}</div>}</td>
              <td className="td">{fmt(s.quantity)} {s.unit}</td>
              <td className="td">{t(FREQ[s.frequency])}<div className="text-xs text-stone-500">{t('{n} days before', { n: s.leadDays })}</div></td>
              <td className="td">{fmtDate(s.nextNeededDate)}</td>
              <td className="td"><Badge value={s.active ? 'AVAILABLE' : 'WITHDRAWN'} /></td>
              <td className="td whitespace-nowrap text-right">
                <button className="btn-secondary btn-sm mr-1" onClick={() => toggle(s)}>{s.active ? t('Pause') : t('Resume')}</button>
                <button className="btn-secondary btn-sm" onClick={() => remove(s)}>{t('Delete')}</button>
              </td>
            </tr>
          ))}
        </Table>
      )}
    </>
  );
}
