import { useState } from 'react';
import { api } from '../api.js';
import { addDays, fmt, fmtDate, rwf, today, useCrops, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, CropSelect, Field, Modal, PageHeader, Spinner, Table } from '../ui.jsx';

function Form({ f, set, crops }) {
  const { t } = useT();
  return (
    <>
      <Field label={t('Crop')}><CropSelect crops={crops} value={f.cropId} onChange={set('cropId')} /></Field>
      <Field label={t('Quantity needed')}><input className="input" type="number" step="0.01" min="0.01" value={f.quantity} onChange={set('quantity')} required /></Field>
      <Field label={t('Max price per unit (RWF)')} hint={t('Optional')}><input className="input" type="number" min="0" value={f.maxPrice} onChange={set('maxPrice')} /></Field>
      <Field label={t('Needed by')}><input className="input" type="date" min={today()} value={f.neededBy} onChange={set('neededBy')} required /></Field>
      <Field label={t('Notes (quality, delivery…)')}><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} /></Field>
    </>
  );
}

const body = (f) => ({ cropId: Number(f.cropId), quantity: Number(f.quantity), maxPrice: f.maxPrice === '' || f.maxPrice === null ? null : Number(f.maxPrice), neededBy: f.neededBy, notes: f.notes || null });

function EditModal({ item, crops, onClose, onDone }) {
  const { t } = useT();
  const [f, setF] = useState({ cropId: String(item.cropId), quantity: item.quantity, maxPrice: item.maxPrice ?? '', neededBy: item.neededBy < today() ? today() : item.neededBy, notes: item.notes || '' });
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  async function submit(e) {
    e.preventDefault(); setErr('');
    try { await api(`/api/demands/${item.id}`, { method: 'PUT', body: body(f) }); onDone(); } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={t('Edit request')} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        <Form f={f} set={set} crops={crops} />
        <button className="btn-primary w-full">{t('Save changes')}</button>
      </form>
    </Modal>
  );
}

function RepeatModal({ item, onClose, onDone }) {
  const { t } = useT();
  const [date, setDate] = useState(addDays(7));
  const [err, setErr] = useState('');
  async function submit(e) {
    e.preventDefault(); setErr('');
    try { await api(`/api/demands/${item.id}/repeat`, { method: 'POST', body: { neededBy: date } }); onDone(); } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={t('Repeat this request')} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <p className="text-sm text-stone-600">{item.cropName} · {fmt(item.quantity)} {item.unit}</p>
        <Alert>{err}</Alert>
        <Field label={t('Needed by')}><input className="input" type="date" min={today()} value={date} onChange={(e) => setDate(e.target.value)} required /></Field>
        <button className="btn-primary w-full">{t('Send the same request again')}</button>
      </form>
    </Modal>
  );
}

export default function Demands() {
  const { t } = useT();
  const crops = useCrops();
  const list = useLoad(() => api('/api/demands/mine'));
  const [f, setF] = useState({ cropId: '', quantity: '', maxPrice: '', neededBy: today(), notes: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const [edit, setEdit] = useState(null);
  const [repeat, setRepeat] = useState(null);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      await api('/api/demands', { method: 'POST', body: body(f) });
      setF({ ...f, quantity: '', maxPrice: '', notes: '' });
      list.reload();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function cancel(id) {
    if (!confirm(t('Cancel this request?'))) return;
    try { await api(`/api/demands/${id}/cancel`, { method: 'PATCH' }); list.reload(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title={t('Produce requests')} subtitle={t('Tell us what you need. An admin will match you with a farmer.')} />
      <form onSubmit={submit} className="card mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Form f={f} set={set} crops={crops.data} />
        <div className="flex items-end"><button className="btn-primary w-full" disabled={busy}>{busy ? t('Sending…') : t('Send request')}</button></div>
        <div className="sm:col-span-2 lg:col-span-4"><Alert>{err}</Alert></div>
      </form>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('Crop'), t('Quantity'), t('Max price'), t('Needed by'), t('Status'), '']} empty={t('You have no requests yet.')}>
          {list.data.map((d) => (
            <tr key={d.id}>
              <td className="td font-medium">{d.cropName}</td>
              <td className="td">
                {fmt(d.quantity)} {d.unit}
                {d.status === 'PARTIAL' && <div className="text-xs text-stone-500">{t('{m} of {q} matched', { m: fmt(d.matchedQuantity), q: fmt(d.quantity) })}</div>}
              </td>
              <td className="td">{rwf(d.maxPrice)}</td>
              <td className="td">{fmtDate(d.neededBy)}</td>
              <td className="td"><Badge value={d.status} /></td>
              <td className="td whitespace-nowrap text-right">
                {d.status === 'OPEN' && <button className="btn-secondary btn-sm mr-1" onClick={() => setEdit(d)}>{t('Edit')}</button>}
                <button className="btn-secondary btn-sm mr-1" onClick={() => setRepeat(d)}>{t('Repeat')}</button>
                {d.status === 'OPEN' && <button className="btn-secondary btn-sm" onClick={() => cancel(d.id)}>{t('Cancel')}</button>}
              </td>
            </tr>
          ))}
        </Table>
      )}
      {edit && <EditModal item={edit} crops={crops.data} onClose={() => setEdit(null)} onDone={() => { setEdit(null); list.reload(); }} />}
      {repeat && <RepeatModal item={repeat} onClose={() => setRepeat(null)} onDone={() => { setRepeat(null); list.reload(); }} />}
    </>
  );
}
