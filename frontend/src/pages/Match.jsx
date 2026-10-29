import { useState } from 'react';
import { api } from '../api.js';
import { fmt, fmtDate, rwf, useLoad } from '../hooks.js';
import { Alert, Badge, Modal, PageHeader, Spinner, Table, Field } from '../ui.jsx';

function MatchModal({ demand, onClose, onDone }) {
  const sug = useLoad(() => api('/api/orders/suggestions', { params: { demandId: demand.id } }), [demand.id]);
  const [sel, setSel] = useState(null);
  const [f, setF] = useState({ quantity: '', agreedPrice: '', deliveryDate: demand.neededBy, notes: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  function pick(s) {
    setSel(s);
    setF({ ...f, quantity: String(Math.min(Number(demand.quantity), Number(s.supply.quantityAvailable))), agreedPrice: String(s.supply.pricePerUnit) });
  }

  async function submit(e) {
    e.preventDefault(); setErr(''); setBusy(true);
    try {
      await api('/api/orders', { method: 'POST', body: {
        demandId: demand.id, supplyId: sel.supply.id, quantity: Number(f.quantity),
        agreedPrice: Number(f.agreedPrice), deliveryDate: f.deliveryDate, notes: f.notes || null } });
      onDone();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  return (
    <Modal title={`Match ${demand.shopName}: ${fmt(demand.quantity)} ${demand.unit} ${demand.cropName}`} onClose={onClose}>
      {sug.loading ? <Spinner /> : sug.error ? <Alert>{sug.error}</Alert> : sug.data.length === 0 ? (
        <Alert type="info">No farmer has {demand.cropName} available for {fmtDate(demand.neededBy)} yet.</Alert>
      ) : (
        <div className="space-y-2">
          {sug.data.map((s) => (
            <button type="button" key={s.supply.id} onClick={() => pick(s)}
              className={`w-full rounded-lg border p-3 text-left text-sm ${sel?.supply.id === s.supply.id ? 'border-green-700 bg-green-50' : 'border-stone-200 hover:bg-stone-50'}`}>
              <div className="flex justify-between gap-2">
                <span className="font-medium">{s.supply.farmerName}{s.supply.district ? ` · ${s.supply.district}` : ''}</span>
                {s.coversFullQuantity ? <span className="text-xs font-semibold text-green-700">Covers full amount</span> : <span className="text-xs text-amber-700">Partial</span>}
              </div>
              <div className="text-stone-600">{fmt(s.supply.quantityAvailable)} {s.supply.unit} at {rwf(s.supply.pricePerUnit)}</div>
            </button>
          ))}
        </div>
      )}
      {sel && (
        <form onSubmit={submit} className="mt-4 space-y-3 border-t pt-4">
          <Alert>{err}</Alert>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Quantity"><input className="input" type="number" step="0.01" min="0.01" value={f.quantity} onChange={set('quantity')} required /></Field>
            <Field label="Agreed price (RWF)"><input className="input" type="number" min="0" value={f.agreedPrice} onChange={set('agreedPrice')} required /></Field>
          </div>
          <Field label="Delivery date"><input className="input" type="date" value={f.deliveryDate} onChange={set('deliveryDate')} required /></Field>
          <Field label="Notes"><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} placeholder="Who pays transport, quality, pickup point…" /></Field>
          <button className="btn-primary w-full" disabled={busy}>{busy ? 'Matching…' : 'Create order'}</button>
        </form>
      )}
    </Modal>
  );
}

export default function Match() {
  const list = useLoad(() => api('/api/demands', { params: { status: 'OPEN' } }));
  const [sel, setSel] = useState(null);
  return (
    <>
      <PageHeader title="Match requests" subtitle="Open shop requests waiting for a farmer. Confirm terms with both sides before matching." />
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={['Shop', 'Crop', 'Quantity', 'Max price', 'Needed by', 'Status', '']} empty="No open requests right now.">
          {list.data.map((d) => (
            <tr key={d.id}>
              <td className="td font-medium">{d.shopName}</td>
              <td className="td">{d.cropName}</td>
              <td className="td">{fmt(d.quantity)} {d.unit}</td>
              <td className="td">{rwf(d.maxPrice)}</td>
              <td className="td">{fmtDate(d.neededBy)}</td>
              <td className="td"><Badge value={d.status} /></td>
              <td className="td text-right"><button className="btn-primary btn-sm" onClick={() => setSel(d)}>Find supply</button></td>
            </tr>
          ))}
        </Table>
      )}
      {sel && <MatchModal demand={sel} onClose={() => setSel(null)} onDone={() => { setSel(null); list.reload(); }} />}
    </>
  );
}
