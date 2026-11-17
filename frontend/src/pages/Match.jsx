import { useState } from 'react';
import { api } from '../api.js';
import { fmt, fmtDate, rwf, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, Field, GradeTag, Modal, PageHeader, Photo, RepLine, Spinner, Table } from '../ui.jsx';
import { DELIVERY } from './Orders.jsx';

function MatchModal({ demand, onClose, onDone }) {
  const { t } = useT();
  const sug = useLoad(() => api('/api/orders/suggestions', { params: { demandId: demand.id } }), [demand.id]);
  const [sel, setSel] = useState(null);
  const [f, setF] = useState({ quantity: '', agreedPrice: '', deliveryDate: demand.neededBy, deliveryMethod: 'TO_BE_AGREED', pickupPoint: '', transportPaidBy: '', notes: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const remaining = Number(demand.remainingQuantity);

  function pick(s) {
    setSel(s);
    setF({ ...f, quantity: String(Math.min(remaining, Number(s.supply.quantityAvailable))), agreedPrice: String(s.supply.pricePerUnit), pickupPoint: s.supply.pickupPoint || '' });
  }

  async function submit(e) {
    e.preventDefault(); setErr(''); setBusy(true);
    try {
      await api('/api/orders', { method: 'POST', body: {
        demandId: demand.id, supplyId: sel.supply.id, quantity: Number(f.quantity), agreedPrice: Number(f.agreedPrice),
        deliveryDate: f.deliveryDate, deliveryMethod: f.deliveryMethod, pickupPoint: f.pickupPoint || null,
        transportPaidBy: f.transportPaidBy || null, notes: f.notes || null } });
      onDone();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  return (
    <Modal title={`${demand.shopName}: ${fmt(remaining)} ${demand.unit} ${demand.cropName}`} onClose={onClose}>
      {demand.status === 'PARTIAL' && <p className="mb-3 text-sm text-stone-600">{t('{m} of {q} already matched. {r} still needed.', { m: fmt(demand.matchedQuantity), q: fmt(demand.quantity), r: fmt(remaining) })}</p>}
      {sug.loading ? <Spinner /> : sug.error ? <Alert>{sug.error}</Alert> : sug.data.length === 0 ? (
        <Alert type="info">{t('No farmer has {crop} available for {date} yet.', { crop: demand.cropName, date: fmtDate(demand.neededBy) })}</Alert>
      ) : (
        <div className="space-y-2">
          {sug.data.map((s) => (
            <button type="button" key={s.supply.id} onClick={() => pick(s)}
              className={`flex w-full gap-3 rounded-lg border p-3 text-left text-sm ${sel?.supply.id === s.supply.id ? 'border-green-700 bg-green-50' : 'border-stone-200 hover:bg-stone-50'}`}>
              <Photo url={s.supply.photoUrl} className="h-14 w-14 shrink-0" />
              <div className="min-w-0 flex-1">
                <div className="flex justify-between gap-2">
                  <span className="font-medium">{s.supply.farmerName}{s.supply.cooperative ? ` (${t('Cooperative')})` : ''}{s.supply.district ? ` · ${s.supply.district}` : ''}</span>
                  {s.coversFullQuantity ? <span className="shrink-0 text-xs font-semibold text-green-700">{t('Covers full amount')}</span> : <span className="shrink-0 text-xs text-amber-700">{t('Partial')}</span>}
                </div>
                <div className="text-stone-600">{fmt(s.supply.quantityAvailable)} {s.supply.unit} {t('at')} {rwf(s.supply.pricePerUnit)} <GradeTag grade={s.supply.qualityGrade} /></div>
                <div className="text-xs text-stone-500">
                  {s.supply.harvestDate && <>{t('Harvested {d}', { d: fmtDate(s.supply.harvestDate) })} · </>}
                  {s.supply.minOrderQuantity != null && <>{t('Min {n}', { n: fmt(s.supply.minOrderQuantity) })} · </>}
                  {s.supply.pickupPoint && <>{t('Pickup: {p}', { p: s.supply.pickupPoint })} · </>}
                  <RepLine rep={s.supply.reputation} />
                </div>
              </div>
            </button>
          ))}
        </div>
      )}
      {sel && (
        <form onSubmit={submit} className="mt-4 space-y-3 border-t pt-4">
          <Alert>{err}</Alert>
          <div className="grid grid-cols-2 gap-3">
            <Field label={t('Quantity')}><input className="input" type="number" step="0.01" min="0.01" value={f.quantity} onChange={set('quantity')} required /></Field>
            <Field label={t('Agreed price (RWF)')}><input className="input" type="number" min="0" value={f.agreedPrice} onChange={set('agreedPrice')} required /></Field>
          </div>
          <Field label={t('Delivery date')}><input className="input" type="date" value={f.deliveryDate} onChange={set('deliveryDate')} required /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label={t('How will the goods travel?')}>
              <select className="input" value={f.deliveryMethod} onChange={set('deliveryMethod')}>
                {Object.entries(DELIVERY).map(([k, v]) => <option key={k} value={k}>{t(v)}</option>)}
              </select>
            </Field>
            <Field label={t('Who pays for transport?')}>
              <select className="input" value={f.transportPaidBy} onChange={set('transportPaidBy')}>
                <option value="">{t('Not decided')}</option>
                <option value="FARMER">{t('Farmer pays transport')}</option>
                <option value="SHOP">{t('Shop pays transport')}</option>
                <option value="SHARED">{t('Transport cost shared')}</option>
              </select>
            </Field>
          </div>
          <Field label={t('Pickup point or delivery address')}><input className="input" maxLength={200} value={f.pickupPoint} onChange={set('pickupPoint')} /></Field>
          <Field label={t('Notes')}><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} placeholder={t('Quality, pickup point…')} /></Field>
          <button className="btn-primary w-full" disabled={busy}>{busy ? t('Matching…') : t('Create order')}</button>
        </form>
      )}
    </Modal>
  );
}

export default function Match() {
  const { t } = useT();
  const list = useLoad(() => api('/api/demands', { params: { needsMatch: true } }));
  const [sel, setSel] = useState(null);
  return (
    <>
      <PageHeader title={t('Match requests')} subtitle={t('Requests waiting for a farmer. One request can be filled by several farmers.')} />
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('Shop'), t('Crop'), t('Still needed'), t('Max price'), t('Needed by'), t('Status'), '']} empty={t('No open requests right now.')}>
          {list.data.map((d) => (
            <tr key={d.id}>
              <td className="td font-medium">{d.shopName}</td>
              <td className="td">{d.cropName}</td>
              <td className="td">{fmt(d.remainingQuantity)} {d.unit}{d.status === 'PARTIAL' && <div className="text-xs text-stone-500">{t('of {q}', { q: fmt(d.quantity) })}</div>}</td>
              <td className="td">{rwf(d.maxPrice)}</td>
              <td className="td">{fmtDate(d.neededBy)}</td>
              <td className="td"><Badge value={d.status} /></td>
              <td className="td text-right"><button className="btn-primary btn-sm" onClick={() => setSel(d)}>{t('Find supply')}</button></td>
            </tr>
          ))}
        </Table>
      )}
      {sel && <MatchModal demand={sel} onClose={() => setSel(null)} onDone={() => { setSel(null); list.reload(); }} />}
    </>
  );
}
