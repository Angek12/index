import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { fmt, fmtDate, fmtDateTime, rwf, useLoad } from '../hooks.js';
import { Alert, Badge, Field, Modal, PageHeader, Spinner } from '../ui.jsx';

const STATUSES = ['MATCHED', 'CONFIRMED', 'DELIVERED', 'COMPLETED', 'FAILED', 'CANCELLED'];
const LABEL = { CONFIRMED: 'Confirm', DELIVERED: 'Mark delivered', COMPLETED: 'Confirm received', CANCELLED: 'Cancel order', FAILED: 'Mark failed' };

function actionsFor(o, role) {
  const farmerSide = role === 'FARMER' || role === 'ADMIN';
  const shopSide = role === 'SHOP' || role === 'ADMIN';
  const a = [];
  if (o.status === 'MATCHED' && farmerSide) a.push('CONFIRMED');
  if (o.status === 'CONFIRMED' && farmerSide) a.push('DELIVERED');
  if (o.status === 'DELIVERED' && shopSide) a.push('COMPLETED');
  if (['MATCHED', 'CONFIRMED'].includes(o.status)) a.push('CANCELLED');
  if (['CONFIRMED', 'DELIVERED'].includes(o.status)) a.push('FAILED');
  return a;
}

function StatusModal({ order, target, role, onClose, onDone }) {
  const [note, setNote] = useState('');
  const [transport, setTransport] = useState('');
  const [spoiled, setSpoiled] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const needsReason = target === 'CANCELLED' || target === 'FAILED';
  const farmerSide = role === 'FARMER' || role === 'ADMIN';
  const shopSide = role === 'SHOP' || role === 'ADMIN';

  async function submit(e) {
    e.preventDefault(); setErr(''); setBusy(true);
    try {
      await api(`/api/orders/${order.id}/status`, { method: 'PATCH', body: {
        status: target, note: note || null,
        transportCost: target === 'DELIVERED' && farmerSide && transport !== '' ? Number(transport) : null,
        spoiledQuantity: target === 'COMPLETED' && shopSide && spoiled !== '' ? Number(spoiled) : null } });
      onDone();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  return (
    <Modal title={`${LABEL[target]} · ${order.cropName}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        {target === 'DELIVERED' && farmerSide && (
          <Field label="Transport cost paid (RWF)" hint="Optional, used for pilot economics"><input className="input" type="number" min="0" value={transport} onChange={(e) => setTransport(e.target.value)} /></Field>
        )}
        {target === 'COMPLETED' && shopSide && (
          <Field label={`Spoiled / rejected quantity (${order.unit})`} hint="Optional, enter 0 if everything was good"><input className="input" type="number" step="0.01" min="0" value={spoiled} onChange={(e) => setSpoiled(e.target.value)} /></Field>
        )}
        <Field label={needsReason ? 'Reason (required)' : 'Note (optional)'}>
          <textarea className="input" rows={3} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} required={needsReason} />
        </Field>
        <button className={needsReason ? 'btn-danger w-full' : 'btn-primary w-full'} disabled={busy}>{busy ? 'Saving…' : LABEL[target]}</button>
      </form>
    </Modal>
  );
}

function HistoryModal({ order, onClose }) {
  const h = useLoad(() => api(`/api/orders/${order.id}/history`));
  return (
    <Modal title={`History · ${order.cropName} #${order.id}`} onClose={onClose}>
      {h.loading ? <Spinner /> : h.error ? <Alert>{h.error}</Alert> : (
        <ol className="space-y-3">
          {h.data.map((x) => (
            <li key={x.id} className="border-l-2 border-green-600 pl-3">
              <div className="flex items-center gap-2">
                {x.fromStatus && <><Badge value={x.fromStatus} /><span>→</span></>}
                <Badge value={x.toStatus} />
              </div>
              <div className="mt-1 text-xs text-stone-500">{fmtDateTime(x.changedAt)}{x.changedBy ? ` · ${x.changedBy}` : ''}</div>
              {x.note && <div className="text-sm">{x.note}</div>}
            </li>
          ))}
        </ol>
      )}
    </Modal>
  );
}

export default function Orders() {
  const { user } = useAuth();
  const [status, setStatus] = useState('');
  const list = useLoad(() => api('/api/orders', { params: { status } }), [status]);
  const [act, setAct] = useState(null);
  const [hist, setHist] = useState(null);

  return (
    <>
      <PageHeader title="Orders" subtitle="Track every order from match to delivery."
        action={<select className="input w-auto" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>} />
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : list.data.length === 0 ? (
        <div className="card text-center text-sm text-stone-500">No orders to show.</div>
      ) : (
        <div className="space-y-3">
          {list.data.map((o) => (
            <div key={o.id} className="card">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <div className="font-semibold">{o.cropName} · {fmt(o.quantity)} {o.unit}</div>
                  <div className="text-sm text-stone-600">{o.shopName} ⇄ {o.farmerName}</div>
                </div>
                <Badge value={o.status} />
              </div>
              <div className="mt-2 grid grid-cols-2 gap-2 text-sm text-stone-700 sm:grid-cols-4">
                <div><span className="text-stone-500">Price:</span> {rwf(o.agreedPrice)}</div>
                <div><span className="text-stone-500">Total:</span> {rwf(o.totalValue)}</div>
                <div><span className="text-stone-500">Delivery:</span> {fmtDate(o.deliveryDate)}</div>
                <div><span className="text-stone-500">Transport:</span> {rwf(o.transportCost)}</div>
              </div>
              {o.notes && <div className="mt-1 text-sm text-stone-500">{o.notes}</div>}
              <div className="mt-3 flex flex-wrap gap-2">
                {actionsFor(o, user.role).map((a) => (
                  <button key={a} className={`btn-sm ${a === 'CANCELLED' || a === 'FAILED' ? 'btn-secondary' : 'btn-primary'}`} onClick={() => setAct({ order: o, target: a })}>{LABEL[a]}</button>
                ))}
                <button className="btn-secondary btn-sm" onClick={() => setHist(o)}>History</button>
              </div>
            </div>
          ))}
        </div>
      )}
      {act && <StatusModal {...act} role={user.role} onClose={() => setAct(null)} onDone={() => { setAct(null); list.reload(); }} />}
      {hist && <HistoryModal order={hist} onClose={() => setHist(null)} />}
    </>
  );
}
