import { useState } from 'react';
import { api } from '../api.js';
import { fmt, fmtDate, rwf, today, useCrops, useLoad } from '../hooks.js';
import { Alert, CropSelect, Field, PageHeader, Spinner, Table } from '../ui.jsx';

export default function Sales() {
  const crops = useCrops();
  const list = useLoad(() => api('/api/sales'));
  const [f, setF] = useState({ cropId: '', quantity: '', unitPrice: '', soldOn: today() });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      await api('/api/sales', { method: 'POST', body: { cropId: Number(f.cropId), quantity: Number(f.quantity), unitPrice: Number(f.unitPrice), soldOn: f.soldOn } });
      setF({ ...f, quantity: '', unitPrice: f.unitPrice });
      list.reload();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function remove(id) {
    if (!confirm('Delete this sales record?')) return;
    try { await api(`/api/sales/${id}`, { method: 'DELETE' }); list.reload(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title="Sales records" subtitle="Record what you sold. This powers your demand forecast." />
      <form onSubmit={submit} className="card mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-5 lg:items-end">
        <Field label="Crop"><CropSelect crops={crops.data} value={f.cropId} onChange={set('cropId')} /></Field>
        <Field label="Quantity sold"><input className="input" type="number" step="0.01" min="0.01" value={f.quantity} onChange={set('quantity')} required /></Field>
        <Field label="Price per unit (RWF)"><input className="input" type="number" step="1" min="0" value={f.unitPrice} onChange={set('unitPrice')} required /></Field>
        <Field label="Date"><input className="input" type="date" max={today()} value={f.soldOn} onChange={set('soldOn')} required /></Field>
        <button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Add'}</button>
        <div className="sm:col-span-2 lg:col-span-5"><Alert>{err}</Alert></div>
      </form>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={['Date', 'Crop', 'Quantity', 'Price', 'Total', '']} empty="No sales in the last 90 days.">
          {list.data.map((s) => (
            <tr key={s.id}>
              <td className="td">{fmtDate(s.soldOn)}</td>
              <td className="td">{s.cropName}</td>
              <td className="td">{fmt(s.quantity)} {s.unit}</td>
              <td className="td">{rwf(s.unitPrice)}</td>
              <td className="td font-medium">{rwf(s.total)}</td>
              <td className="td text-right"><button className="btn-secondary btn-sm" onClick={() => remove(s.id)}>Delete</button></td>
            </tr>
          ))}
        </Table>
      )}
    </>
  );
}
