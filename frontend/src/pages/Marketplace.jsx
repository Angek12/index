import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { addDays, fmt, fmtDate, rwf, today, useCrops, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, CropSelect, Field, GradeTag, Modal, PageHeader, Photo, RepLine, Spinner } from '../ui.jsx';
import { DELIVERY } from './Orders.jsx';
import { DISTRICTS } from './Register.jsx';

function OrderModal({ listing, onClose }) {
  const { t } = useT();
  const nav = useNavigate();
  const minDate = listing.availableFrom > today() ? listing.availableFrom : today();
  const [f, setF] = useState({ quantity: String(Math.min(Number(listing.quantityAvailable), Math.max(Number(listing.minOrderQuantity || 0), 1))), deliveryDate: minDate > addDays(2) ? minDate : addDays(2), deliveryMethod: 'TO_BE_AGREED', notes: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const total = Number(f.quantity || 0) * Number(listing.pricePerUnit);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault(); setErr(''); setBusy(true);
    try {
      await api('/api/marketplace/orders', { method: 'POST', body: {
        supplyId: listing.id, quantity: Number(f.quantity), deliveryDate: f.deliveryDate, deliveryMethod: f.deliveryMethod, notes: f.notes || null } });
      nav('/orders');
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  return (
    <Modal title={`${t('Order')} · ${listing.cropName}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <p className="text-sm text-stone-600">{listing.farmerName} · {rwf(listing.pricePerUnit)} / {listing.unit} · {fmt(listing.quantityAvailable)} {listing.unit} {t('available')}</p>
        <Alert>{err}</Alert>
        <Field label={`${t('Quantity')} (${listing.unit})`} hint={listing.minOrderQuantity != null ? t('Minimum order {n}', { n: fmt(listing.minOrderQuantity) }) : undefined}>
          <input className="input" type="number" step="0.01" min="0.01" max={listing.quantityAvailable} value={f.quantity} onChange={set('quantity')} required />
        </Field>
        <Field label={t('Delivery date')}>
          <input className="input" type="date" min={minDate} max={listing.availableUntil || undefined} value={f.deliveryDate} onChange={set('deliveryDate')} required />
        </Field>
        <Field label={t('How will the goods travel?')}>
          <select className="input" value={f.deliveryMethod} onChange={set('deliveryMethod')}>
            {Object.entries(DELIVERY).map(([k, v]) => <option key={k} value={k}>{t(v)}</option>)}
          </select>
        </Field>
        <Field label={t('Notes')}><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} /></Field>
        <div className="rounded-lg bg-stone-50 p-3 text-sm">{t('Total')}: <b>{rwf(total)}</b></div>
        <p className="text-xs text-stone-500">{t('The stock is held for you. The farmer confirms or declines the order.')}</p>
        <button className="btn-primary w-full" disabled={busy}>{busy ? t('Sending…') : t('Send order')}</button>
      </form>
    </Modal>
  );
}

export default function Marketplace() {
  const { t } = useT();
  const crops = useCrops();
  const [cropId, setCropId] = useState('');
  const [district, setDistrict] = useState('');
  const list = useLoad(() => api('/api/marketplace/listings', { params: { cropId, district } }), [cropId, district]);
  const [sel, setSel] = useState(null);

  return (
    <>
      <PageHeader title={t('Marketplace')} subtitle={t('Order directly from farmers. The cheapest listings come first.')} />
      <div className="card mb-5 grid gap-3 sm:grid-cols-2">
        <Field label={t('Crop')}><CropSelect crops={crops.data} value={cropId} onChange={(e) => setCropId(e.target.value)} required={false} /></Field>
        <Field label={t('District')}>
          <input className="input" list="mk-districts" value={district} onChange={(e) => setDistrict(e.target.value)} placeholder={t('All districts')} />
          <datalist id="mk-districts">{DISTRICTS.map((d) => <option key={d} value={d} />)}</datalist>
        </Field>
      </div>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : list.data.length === 0 ? (
        <div className="card text-center text-sm text-stone-500">{t('No listings match. Try another crop or district, or send a request and the Index team will find a farmer.')}</div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          {list.data.map((l) => (
            <div key={l.id} className="card flex gap-3">
              <Photo url={l.photoUrl} className="h-20 w-20 shrink-0" />
              <div className="min-w-0 flex-1 space-y-1">
                <div className="flex items-start justify-between gap-2">
                  <div className="font-semibold">{l.cropName} <GradeTag grade={l.qualityGrade} /></div>
                  <div className="text-right font-bold text-green-800">{rwf(l.pricePerUnit)}<div className="text-xs font-normal text-stone-500">/ {l.unit}</div></div>
                </div>
                <div className="text-sm text-stone-700">{l.farmerName}{l.cooperative ? ` (${t('Cooperative')})` : ''}{l.district ? ` · ${l.district}` : ''}</div>
                <div className="text-xs text-stone-500">
                  {fmt(l.quantityAvailable)} {l.unit} {t('available')}
                  {l.minOrderQuantity != null && ` · ${t('Min {n}', { n: fmt(l.minOrderQuantity) })}`}
                  {l.harvestDate && ` · ${t('Harvested {d}', { d: fmtDate(l.harvestDate) })}`}
                  {l.pickupPoint && ` · ${t('Pickup: {p}', { p: l.pickupPoint })}`}
                </div>
                <div><RepLine rep={l.reputation} /></div>
                <button className="btn-primary btn-sm mt-1" onClick={() => setSel(l)}>{t('Order')}</button>
              </div>
            </div>
          ))}
        </div>
      )}
      {sel && <OrderModal listing={sel} onClose={() => setSel(null)} />}
    </>
  );
}
