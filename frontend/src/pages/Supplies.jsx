import { useState } from 'react';
import { api } from '../api.js';
import { fmt, fmtDate, rwf, today, useCrops, useLoad } from '../hooks.js';
import { Alert, Badge, CropSelect, Field, Modal, PageHeader, Spinner, Table } from '../ui.jsx';

function EditModal({ item, onClose, onDone }) {
  const [f, setF] = useState({
    quantityAvailable: item.quantityAvailable, pricePerUnit: item.pricePerUnit,
    availableFrom: item.availableFrom, availableUntil: item.availableUntil || '', notes: item.notes || '' });
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  async function submit(e) {
    e.preventDefault(); setErr('');
    try {
      await api(`/api/supplies/${item.id}`, { method: 'PUT', body: {
        quantityAvailable: Number(f.quantityAvailable), pricePerUnit: Number(f.pricePerUnit),
        availableFrom: f.availableFrom, availableUntil: f.availableUntil || null, notes: f.notes || null } });
      onDone();
    } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={`Edit ${item.cropName} listing`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        <Field label={`Quantity available (${item.unit})`}><input className="input" type="number" step="0.01" min="0" value={f.quantityAvailable} onChange={set('quantityAvailable')} required /></Field>
        <Field label="Price per unit (RWF)"><input className="input" type="number" min="0" value={f.pricePerUnit} onChange={set('pricePerUnit')} required /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Available from"><input className="input" type="date" value={f.availableFrom} onChange={set('availableFrom')} required /></Field>
          <Field label="Until"><input className="input" type="date" value={f.availableUntil} onChange={set('availableUntil')} /></Field>
        </div>
        <Field label="Notes"><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} /></Field>
        <button className="btn-primary w-full">Save changes</button>
      </form>
    </Modal>
  );
}

export default function Supplies() {
  const crops = useCrops();
  const list = useLoad(() => api('/api/supplies/mine'));
  const [f, setF] = useState({ cropId: '', quantityAvailable: '', pricePerUnit: '', availableFrom: today(), availableUntil: '', notes: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const [edit, setEdit] = useState(null);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      await api('/api/supplies', { method: 'POST', body: {
        cropId: Number(f.cropId), quantityAvailable: Number(f.quantityAvailable), pricePerUnit: Number(f.pricePerUnit),
        availableFrom: f.availableFrom, availableUntil: f.availableUntil || null, notes: f.notes || null } });
      setF({ ...f, quantityAvailable: '', notes: '' });
      list.reload();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function withdraw(id) {
    if (!confirm('Withdraw this listing?')) return;
    try { await api(`/api/supplies/${id}/withdraw`, { method: 'PATCH' }); list.reload(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title="My supply" subtitle="List the produce you can deliver." />
      <form onSubmit={submit} className="card mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Field label="Crop"><CropSelect crops={crops.data} value={f.cropId} onChange={set('cropId')} /></Field>
        <Field label="Quantity available"><input className="input" type="number" step="0.01" min="0.01" value={f.quantityAvailable} onChange={set('quantityAvailable')} required /></Field>
        <Field label="Price per unit (RWF)"><input className="input" type="number" min="0" value={f.pricePerUnit} onChange={set('pricePerUnit')} required /></Field>
        <Field label="Available from"><input className="input" type="date" value={f.availableFrom} onChange={set('availableFrom')} required /></Field>
        <Field label="Available until" hint="Optional"><input className="input" type="date" value={f.availableUntil} onChange={set('availableUntil')} /></Field>
        <div className="sm:col-span-2"><Field label="Notes (quality, harvest date…)"><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} /></Field></div>
        <div className="flex items-end"><button className="btn-primary w-full" disabled={busy}>{busy ? 'Saving…' : 'Add listing'}</button></div>
        <div className="sm:col-span-2 lg:col-span-4"><Alert>{err}</Alert></div>
      </form>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={['Crop', 'Available', 'Price', 'Window', 'Status', '']} empty="You have no listings yet.">
          {list.data.map((s) => (
            <tr key={s.id}>
              <td className="td font-medium">{s.cropName}</td>
              <td className="td">{fmt(s.quantityAvailable)} {s.unit}</td>
              <td className="td">{rwf(s.pricePerUnit)}</td>
              <td className="td">{fmtDate(s.availableFrom)} → {s.availableUntil ? fmtDate(s.availableUntil) : 'open'}</td>
              <td className="td"><Badge value={s.status} /></td>
              <td className="td whitespace-nowrap text-right">
                {s.status !== 'WITHDRAWN' && (
                  <>
                    <button className="btn-secondary btn-sm mr-1" onClick={() => setEdit(s)}>Edit</button>
                    <button className="btn-secondary btn-sm" onClick={() => withdraw(s.id)}>Withdraw</button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </Table>
      )}
      {edit && <EditModal item={edit} onClose={() => setEdit(null)} onDone={() => { setEdit(null); list.reload(); }} />}
    </>
  );
}
