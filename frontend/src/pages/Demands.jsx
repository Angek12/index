import { useState } from 'react';
import { api } from '../api.js';
import { fmt, fmtDate, rwf, today, useCrops, useLoad } from '../hooks.js';
import { Alert, Badge, CropSelect, Field, PageHeader, Spinner, Table } from '../ui.jsx';

export default function Demands() {
  const crops = useCrops();
  const list = useLoad(() => api('/api/demands/mine'));
  const [f, setF] = useState({ cropId: '', quantity: '', maxPrice: '', neededBy: today(), notes: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      await api('/api/demands', { method: 'POST', body: {
        cropId: Number(f.cropId), quantity: Number(f.quantity),
        maxPrice: f.maxPrice === '' ? null : Number(f.maxPrice), neededBy: f.neededBy, notes: f.notes || null } });
      setF({ ...f, quantity: '', maxPrice: '', notes: '' });
      list.reload();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function cancel(id) {
    if (!confirm('Cancel this request?')) return;
    try { await api(`/api/demands/${id}/cancel`, { method: 'PATCH' }); list.reload(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title="Produce requests" subtitle="Tell us what you need. An admin will match you with a farmer." />
      <form onSubmit={submit} className="card mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Field label="Crop"><CropSelect crops={crops.data} value={f.cropId} onChange={set('cropId')} /></Field>
        <Field label="Quantity needed"><input className="input" type="number" step="0.01" min="0.01" value={f.quantity} onChange={set('quantity')} required /></Field>
        <Field label="Max price per unit (RWF)" hint="Optional"><input className="input" type="number" min="0" value={f.maxPrice} onChange={set('maxPrice')} /></Field>
        <Field label="Needed by"><input className="input" type="date" min={today()} value={f.neededBy} onChange={set('neededBy')} required /></Field>
        <div className="sm:col-span-2 lg:col-span-3"><Field label="Notes (quality, delivery…)"><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} /></Field></div>
        <div className="flex items-end"><button className="btn-primary w-full" disabled={busy}>{busy ? 'Sending…' : 'Send request'}</button></div>
        <div className="sm:col-span-2 lg:col-span-4"><Alert>{err}</Alert></div>
      </form>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={['Crop', 'Quantity', 'Max price', 'Needed by', 'Status', '']} empty="You have no requests yet.">
          {list.data.map((d) => (
            <tr key={d.id}>
              <td className="td font-medium">{d.cropName}</td>
              <td className="td">{fmt(d.quantity)} {d.unit}</td>
              <td className="td">{rwf(d.maxPrice)}</td>
              <td className="td">{fmtDate(d.neededBy)}</td>
              <td className="td"><Badge value={d.status} /></td>
              <td className="td text-right">{d.status === 'OPEN' && <button className="btn-secondary btn-sm" onClick={() => cancel(d.id)}>Cancel</button>}</td>
            </tr>
          ))}
        </Table>
      )}
    </>
  );
}
