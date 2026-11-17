import { useState } from 'react';
import { api, download } from '../api.js';
import { useAuth } from '../auth.jsx';
import { digits, fmt, fmtDate, fmtDateTime, rwf, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, Field, Modal, PageHeader, Spinner, Stars } from '../ui.jsx';

const STATUSES = ['MATCHED', 'CONFIRMED', 'DELIVERED', 'COMPLETED', 'FAILED', 'CANCELLED'];
const LABEL = { CONFIRMED: 'Confirm', DELIVERED: 'Mark delivered', COMPLETED: 'Confirm received', CANCELLED: 'Cancel order', FAILED: 'Mark failed' };
export const DELIVERY = { TO_BE_AGREED: 'To be agreed', FARMER_DELIVERS: 'Farmer delivers', SHOP_COLLECTS: 'Shop collects', SHARED_TRANSPORT: 'Shared transport' };
const PAYER = { FARMER: 'Farmer pays transport', SHOP: 'Shop pays transport', SHARED: 'Transport cost shared' };
const METHODS = { CASH: 'Cash', MOBILE_MONEY: 'Mobile Money', BANK_TRANSFER: 'Bank transfer', OTHER: 'Other' };

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

function Contact({ label, name, phone, place, msg }) {
  const { t } = useT();
  if (!name && !phone && !place) return null;
  return (
    <div className="rounded-lg bg-stone-50 p-2 text-sm">
      <div className="text-xs font-semibold text-stone-500">{label}</div>
      <div>{name}{place ? ` · ${place}` : ''}</div>
      {phone && (
        <div className="mt-1 flex flex-wrap items-center gap-2">
          <span>{phone}</span>
          <a className="btn-secondary btn-sm" href={`tel:${phone}`}>{t('Call')}</a>
          <a className="btn-secondary btn-sm" target="_blank" rel="noopener noreferrer" href={`https://wa.me/${digits(phone)}?text=${encodeURIComponent(msg)}`}>WhatsApp</a>
        </div>
      )}
    </div>
  );
}

function StatusModal({ order, target, role, onClose, onDone }) {
  const { t } = useT();
  const [note, setNote] = useState('');
  const [transport, setTransport] = useState('');
  const [spoiled, setSpoiled] = useState('');
  const [code, setCode] = useState('');
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
        spoiledQuantity: target === 'COMPLETED' && shopSide && spoiled !== '' ? Number(spoiled) : null,
        deliveryCode: target === 'DELIVERED' && code ? code.trim() : null } });
      onDone();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  return (
    <Modal title={`${t(LABEL[target])} · ${order.cropName}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        {target === 'DELIVERED' && farmerSide && (
          <Field label={t('Transport cost paid (RWF)')} hint={t('Optional, used for pilot economics')}><input className="input" type="number" min="0" value={transport} onChange={(e) => setTransport(e.target.value)} /></Field>
        )}
        {target === 'DELIVERED' && order.deliveryCodeRequired && (
          <Field label={role === 'ADMIN' ? t('Delivery code from the shop (optional for admins)') : t('Delivery code from the shop')}
            hint={t('The shop sees this 4-digit code on its order. Ask for it when you hand over the goods.')}>
            <input className="input" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(e.target.value)} required={role !== 'ADMIN'} />
          </Field>
        )}
        {target === 'COMPLETED' && shopSide && (
          <Field label={`${t('Spoiled / rejected quantity')} (${order.unit})`} hint={t('Optional, enter 0 if everything was good')}><input className="input" type="number" step="0.01" min="0" value={spoiled} onChange={(e) => setSpoiled(e.target.value)} /></Field>
        )}
        <Field label={needsReason ? t('Reason (required)') : t('Note (optional)')}>
          <textarea className="input" rows={3} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} required={needsReason} />
        </Field>
        <button className={needsReason ? 'btn-danger w-full' : 'btn-primary w-full'} disabled={busy}>{busy ? t('Saving…') : t(LABEL[target])}</button>
      </form>
    </Modal>
  );
}

function PaymentModal({ order, action, onClose, onDone }) {
  const { t } = useT();
  const [method, setMethod] = useState('MOBILE_MONEY');
  const [reference, setReference] = useState('');
  const [err, setErr] = useState('');
  const paying = action === 'MARK_PAID';
  async function submit(e) {
    e.preventDefault(); setErr('');
    try {
      await api(`/api/orders/${order.id}/payment`, { method: 'PATCH', body: { action, method: paying ? method : null, reference: paying ? reference || null : null } });
      onDone();
    } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={paying ? t('Mark as paid') : t('Confirm payment received')} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <p className="text-sm text-stone-600">{order.cropName} · {rwf(order.totalValue)}</p>
        <p className="rounded-lg bg-stone-50 p-2 text-xs text-stone-600">{t('Index only records this. Payment is made directly between the shop and the farmer.')}</p>
        <Alert>{err}</Alert>
        {paying ? (
          <>
            <Field label={t('How was it paid?')}>
              <select className="input" value={method} onChange={(e) => setMethod(e.target.value)}>
                {Object.entries(METHODS).map(([k, v]) => <option key={k} value={k}>{t(v)}</option>)}
              </select>
            </Field>
            <Field label={t('Reference (optional)')} hint={t('For example the Mobile Money transaction number')}><input className="input" maxLength={80} value={reference} onChange={(e) => setReference(e.target.value)} /></Field>
          </>
        ) : (
          <p className="text-sm">{t('Confirm only if the money has reached you.')}</p>
        )}
        <button className="btn-primary w-full">{paying ? t('Mark as paid') : t('Confirm payment received')}</button>
      </form>
    </Modal>
  );
}

function DeliveryModal({ order, onClose, onDone }) {
  const { t } = useT();
  const [f, setF] = useState({ method: order.deliveryMethod, pickupPoint: order.pickupPoint || '', transportPaidBy: order.transportPaidBy || '' });
  const [err, setErr] = useState('');
  async function submit(e) {
    e.preventDefault(); setErr('');
    try {
      await api(`/api/orders/${order.id}/delivery`, { method: 'PATCH', body: { method: f.method, pickupPoint: f.pickupPoint || null, transportPaidBy: f.transportPaidBy || null } });
      onDone();
    } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={t('Delivery arrangement')} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        <Field label={t('How will the goods travel?')}>
          <select className="input" value={f.method} onChange={(e) => setF({ ...f, method: e.target.value })}>
            {Object.entries(DELIVERY).map(([k, v]) => <option key={k} value={k}>{t(v)}</option>)}
          </select>
        </Field>
        <Field label={t('Pickup point or delivery address')}><input className="input" maxLength={200} value={f.pickupPoint} onChange={(e) => setF({ ...f, pickupPoint: e.target.value })} /></Field>
        <Field label={t('Who pays for transport?')}>
          <select className="input" value={f.transportPaidBy} onChange={(e) => setF({ ...f, transportPaidBy: e.target.value })}>
            <option value="">{t('Not decided')}</option>
            {Object.entries(PAYER).map(([k, v]) => <option key={k} value={k}>{t(v)}</option>)}
          </select>
        </Field>
        <button className="btn-primary w-full">{t('Save changes')}</button>
      </form>
    </Modal>
  );
}

function RatingModal({ order, who, onClose, onDone }) {
  const { t } = useT();
  const [stars, setStars] = useState(5);
  const [comment, setComment] = useState('');
  const [err, setErr] = useState('');
  async function submit(e) {
    e.preventDefault(); setErr('');
    try { await api(`/api/orders/${order.id}/rating`, { method: 'POST', body: { stars, comment: comment || null } }); onDone(); } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={who === 'FARMER' ? t('Rate the farmer') : t('Rate the shop')} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        <div><Stars value={stars} onChange={setStars} /></div>
        <Field label={t('Comment (optional)')}><textarea className="input" rows={3} maxLength={300} value={comment} onChange={(e) => setComment(e.target.value)} /></Field>
        <button className="btn-primary w-full">{t('Send rating')}</button>
      </form>
    </Modal>
  );
}

function HistoryModal({ order, onClose }) {
  const { t } = useT();
  const h = useLoad(() => api(`/api/orders/${order.id}/history`));
  return (
    <Modal title={`${t('History')} · ${order.cropName} #${order.id}`} onClose={onClose}>
      {h.loading ? <Spinner /> : h.error ? <Alert>{h.error}</Alert> : (
        <ol className="space-y-3">
          {h.data.map((x) => (
            <li key={x.id} className="border-l-2 border-green-600 pl-3">
              <div className="flex items-center gap-2">
                {x.fromStatus && x.fromStatus !== x.toStatus && <><Badge value={x.fromStatus} /><span>→</span></>}
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

export const ISSUE_TYPES = { SHORT_DELIVERY: 'Short delivery', SPOILED: 'Spoiled goods', WRONG_QUALITY: 'Wrong quality', LATE: 'Late delivery', NOT_PAID: 'Payment not received', OTHER: 'Other problem' };

function IssuesModal({ order, role, onClose }) {
  const { t } = useT();
  const list = useLoad(() => api(`/api/orders/${order.id}/issues`));
  const [f, setF] = useState({ type: 'SHORT_DELIVERY', description: '', quantityAffected: '' });
  const [err, setErr] = useState('');
  const canReport = role !== 'ADMIN' && !['MATCHED', 'CANCELLED'].includes(order.status);

  async function submit(e) {
    e.preventDefault(); setErr('');
    try {
      await api(`/api/orders/${order.id}/issues`, { method: 'POST', body: {
        type: f.type, description: f.description, quantityAffected: f.quantityAffected === '' ? null : Number(f.quantityAffected) } });
      setF({ type: 'SHORT_DELIVERY', description: '', quantityAffected: '' });
      list.reload();
    } catch (ex) { setErr(ex.message); }
  }

  return (
    <Modal title={`${t('Problems')} · ${order.cropName} #${order.id}`} onClose={onClose}>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <div className="mb-4 space-y-3">
          {list.data.length === 0 && <p className="text-sm text-stone-500">{t('No problems reported on this order.')}</p>}
          {list.data.map((i) => (
            <div key={i.id} className="rounded-lg border border-stone-200 p-3 text-sm">
              <div className="flex items-center justify-between gap-2">
                <span className="font-medium">{t(ISSUE_TYPES[i.type])}</span><Badge value={i.status} />
              </div>
              <div className="mt-1 text-stone-700">{i.description}</div>
              <div className="mt-1 text-xs text-stone-500">{t(i.reporterRole === 'SHOP' ? 'Shop' : 'Farmer')} · {i.reporterName} · {fmtDateTime(i.createdAt)}{i.quantityAffected != null ? ` · ${fmt(i.quantityAffected)} ${order.unit}` : ''}</div>
              {i.resolution && <div className="mt-2 rounded bg-stone-50 p-2 text-xs"><b>{t('Decision')}:</b> {i.resolution}{i.resolvedBy ? ` (${i.resolvedBy})` : ''}</div>}
            </div>
          ))}
        </div>
      )}
      {canReport && (
        <form onSubmit={submit} className="space-y-3 border-t pt-4">
          <h3 className="text-sm font-semibold">{t('Report a problem')}</h3>
          <Alert>{err}</Alert>
          <Field label={t('What went wrong?')}>
            <select className="input" value={f.type} onChange={(e) => setF({ ...f, type: e.target.value })}>
              {Object.entries(ISSUE_TYPES).map(([k, v]) => <option key={k} value={k}>{t(v)}</option>)}
            </select>
          </Field>
          <Field label={t('Describe it')}><textarea className="input" rows={3} maxLength={500} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} required /></Field>
          <Field label={`${t('Quantity affected')} (${order.unit})`} hint={t('Optional')}><input className="input" type="number" step="0.01" min="0" value={f.quantityAffected} onChange={(e) => setF({ ...f, quantityAffected: e.target.value })} /></Field>
          <button className="btn-primary w-full">{t('Send report')}</button>
          <p className="text-xs text-stone-500">{t('The other side and the Index team are told. The Index team will review it.')}</p>
        </form>
      )}
    </Modal>
  );
}

export default function Orders() {
  const { user } = useAuth();
  const { t } = useT();
  const [status, setStatus] = useState('');
  const list = useLoad(() => api('/api/orders', { params: { status } }), [status]);
  const [act, setAct] = useState(null);
  const [pay, setPay] = useState(null);
  const [del, setDel] = useState(null);
  const [rate, setRate] = useState(null);
  const [hist, setHist] = useState(null);
  const [probs, setProbs] = useState(null);
  const [err, setErr] = useState('');
  const role = user.role;
  const done = () => { setAct(null); setPay(null); setDel(null); setRate(null); list.reload(); };

  return (
    <>
      <PageHeader title={t('Orders')} subtitle={t('Track every order from match to delivery.')}
        action={
          <div className="flex flex-wrap items-center gap-2">
            <button className="btn-secondary btn-sm" onClick={() => download('/api/export/orders.csv', 'index-orders.csv').catch((ex) => setErr(ex.message))}>{t('Download for Excel')}</button>
            <select className="input w-auto" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">{t('All statuses')}</option>
              {STATUSES.map((s) => <option key={s} value={s}>{t(s.charAt(0) + s.slice(1).toLowerCase())}</option>)}
            </select>
          </div>} />
      <div className="mb-3"><Alert>{err}</Alert></div>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : list.data.length === 0 ? (
        <div className="card text-center text-sm text-stone-500">{t('No orders to show.')}</div>
      ) : (
        <div className="space-y-3">
          {list.data.map((o) => {
            const canPay = (role === 'SHOP' || role === 'ADMIN') && ['DELIVERED', 'COMPLETED'].includes(o.status) && o.paymentStatus === 'UNPAID';
            const canReceive = (role === 'FARMER' || role === 'ADMIN') && o.paymentStatus === 'PAID';
            const finished = ['COMPLETED', 'FAILED'].includes(o.status);
            const canRateFarmer = role === 'SHOP' && finished && !o.shopRating;
            const canRateShop = role === 'FARMER' && finished && !o.farmerRating;
            const msg = `Index order #${o.id} (${o.cropName})`;
            return (
              <div key={o.id} className="card space-y-2">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <div className="font-semibold">{o.cropName} · {fmt(o.quantity)} {o.unit}{o.source === 'MARKETPLACE' && <span className="ml-2 rounded bg-sky-100 px-1.5 py-0.5 text-xs font-medium text-sky-800">{t('Marketplace order')}</span>}</div>
                    <div className="text-sm text-stone-600">{o.shopName} ⇄ {o.farmerName}</div>
                  </div>
                  <div className="flex flex-wrap items-center gap-1"><Badge value={o.status} />{['DELIVERED', 'COMPLETED'].includes(o.status) && <Badge value={o.paymentStatus} />}</div>
                </div>
                <div className="grid grid-cols-2 gap-2 text-sm text-stone-700 sm:grid-cols-4">
                  <div><span className="text-stone-500">{t('Price')}:</span> {rwf(o.agreedPrice)}</div>
                  <div><span className="text-stone-500">{t('Total')}:</span> {rwf(o.totalValue)}</div>
                  <div><span className="text-stone-500">{t('Delivery')}:</span> {fmtDate(o.deliveryDate)}</div>
                  <div><span className="text-stone-500">{t('Transport')}:</span> {rwf(o.transportCost)}</div>
                </div>
                <div className="text-sm text-stone-700">
                  <span className="text-stone-500">{t('How it travels')}:</span> {t(DELIVERY[o.deliveryMethod])}
                  {o.pickupPoint ? ` · ${o.pickupPoint}` : ''}{o.transportPaidBy ? ` · ${t(PAYER[o.transportPaidBy])}` : ''}
                </div>
                {o.paymentStatus !== 'UNPAID' && (
                  <div className="text-sm text-stone-700"><span className="text-stone-500">{t('Payment')}:</span> {t(METHODS[o.paymentMethod] || o.paymentMethod || '')}{o.paymentReference ? ` · ${o.paymentReference}` : ''}</div>
                )}
                <div className="grid gap-2 sm:grid-cols-2">
                  {(role === 'SHOP' || role === 'ADMIN') && <Contact label={t('Farmer contact')} name={o.farmerContactName} phone={o.farmerPhone} place={o.farmerLocation} msg={msg} />}
                  {(role === 'FARMER' || role === 'ADMIN') && <Contact label={t('Shop contact')} name={o.shopContactName} phone={o.shopPhone} place={o.shopLocation} msg={msg} />}
                </div>
                {o.deliveryCode && (
                  <div className="rounded-lg border border-green-200 bg-green-50 p-3 text-sm">
                    <div className="text-xs font-semibold text-green-800">{t('Your delivery code')}</div>
                    <div className="text-2xl font-bold tracking-widest text-green-900">{o.deliveryCode}</div>
                    <div className="text-xs text-green-800">{t('Give this code to the farmer only when you receive the goods.')}</div>
                  </div>
                )}
                {o.notes && <div className="text-sm text-stone-500">{o.notes}</div>}
                {(o.shopRating || o.farmerRating) && (
                  <div className="flex flex-wrap gap-4 text-xs text-stone-600">
                    {o.shopRating && <span>{t('Shop rated the farmer')}: <Stars value={o.shopRating} /></span>}
                    {o.farmerRating && <span>{t('Farmer rated the shop')}: <Stars value={o.farmerRating} /></span>}
                  </div>
                )}
                <div className="flex flex-wrap gap-2 pt-1">
                  {actionsFor(o, role).map((a) => (
                    <button key={a} className={`btn-sm ${a === 'CANCELLED' || a === 'FAILED' ? 'btn-secondary' : 'btn-primary'}`} onClick={() => setAct({ order: o, target: a })}>{t(LABEL[a])}</button>
                  ))}
                  {canPay && <button className="btn-primary btn-sm" onClick={() => setPay({ order: o, action: 'MARK_PAID' })}>{t('Mark as paid')}</button>}
                  {canReceive && <button className="btn-primary btn-sm" onClick={() => setPay({ order: o, action: 'CONFIRM_RECEIVED' })}>{t('Confirm payment received')}</button>}
                  {['MATCHED', 'CONFIRMED'].includes(o.status) && <button className="btn-secondary btn-sm" onClick={() => setDel(o)}>{t('Delivery arrangement')}</button>}
                  {canRateFarmer && <button className="btn-secondary btn-sm" onClick={() => setRate({ order: o, who: 'FARMER' })}>{t('Rate the farmer')}</button>}
                  {canRateShop && <button className="btn-secondary btn-sm" onClick={() => setRate({ order: o, who: 'SHOP' })}>{t('Rate the shop')}</button>}
                  {o.status !== 'MATCHED' && o.status !== 'CANCELLED' && (
                    <button className="btn-secondary btn-sm" onClick={() => setProbs(o)}>{t('Problems')}{o.openIssues > 0 ? ` (${o.openIssues})` : ''}</button>
                  )}
                  <button className="btn-secondary btn-sm" onClick={() => setHist(o)}>{t('History')}</button>
                </div>
              </div>
            );
          })}
        </div>
      )}
      {act && <StatusModal {...act} role={role} onClose={() => setAct(null)} onDone={done} />}
      {pay && <PaymentModal {...pay} onClose={() => setPay(null)} onDone={done} />}
      {del && <DeliveryModal order={del} onClose={() => setDel(null)} onDone={done} />}
      {rate && <RatingModal {...rate} onClose={() => setRate(null)} onDone={done} />}
      {hist && <HistoryModal order={hist} onClose={() => setHist(null)} />}
      {probs && <IssuesModal order={probs} role={role} onClose={() => { setProbs(null); list.reload(); }} />}
    </>
  );
}
