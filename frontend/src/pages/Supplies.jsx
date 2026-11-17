import { useRef, useState } from 'react';
import { api, upload } from '../api.js';
import { compressImage, fmt, fmtDate, rwf, today, useCrops, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, CropSelect, Field, GradeTag, Modal, PageHeader, Photo, Spinner, Table } from '../ui.jsx';

function Extra({ f, set }) {
  const { t } = useT();
  return (
    <>
      <Field label={t('Quality grade')} hint={t('A is the best')}>
        <select className="input" value={f.qualityGrade} onChange={set('qualityGrade')}>
          <option value="">{t('Not set')}</option>
          <option value="A">A</option><option value="B">B</option><option value="C">C</option>
        </select>
      </Field>
      <Field label={t('Harvest date')}><input className="input" type="date" max={today()} value={f.harvestDate} onChange={set('harvestDate')} /></Field>
      <Field label={t('Minimum order')} hint={t('Optional')}><input className="input" type="number" step="0.01" min="0" value={f.minOrderQuantity} onChange={set('minOrderQuantity')} /></Field>
      <Field label={t('Pickup point')} hint={t('Where the shop can collect')}><input className="input" maxLength={200} value={f.pickupPoint} onChange={set('pickupPoint')} /></Field>
    </>
  );
}

const common = (f) => ({
  qualityGrade: f.qualityGrade || null, harvestDate: f.harvestDate || null,
  minOrderQuantity: f.minOrderQuantity === '' ? null : Number(f.minOrderQuantity),
  pickupPoint: f.pickupPoint || null, notes: f.notes || null,
});

async function sendPhoto(id, file) {
  const blob = await compressImage(file);
  return upload(`/api/supplies/${id}/photo`, blob);
}

function EditModal({ item, onClose, onDone }) {
  const { t } = useT();
  const [f, setF] = useState({
    quantityAvailable: item.quantityAvailable, pricePerUnit: item.pricePerUnit, availableFrom: item.availableFrom,
    availableUntil: item.availableUntil || '', qualityGrade: item.qualityGrade || '', harvestDate: item.harvestDate || '',
    minOrderQuantity: item.minOrderQuantity ?? '', pickupPoint: item.pickupPoint || '', notes: item.notes || '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const fileRef = useRef(null);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault(); setErr(''); setBusy(true);
    try {
      await api(`/api/supplies/${item.id}`, { method: 'PUT', body: {
        quantityAvailable: Number(f.quantityAvailable), pricePerUnit: Number(f.pricePerUnit),
        availableFrom: f.availableFrom, availableUntil: f.availableUntil || null, ...common(f) } });
      const file = fileRef.current?.files?.[0];
      if (file) await sendPhoto(item.id, file);
      onDone();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function removePhoto() {
    try { await api(`/api/supplies/${item.id}/photo`, { method: 'DELETE' }); onDone(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <Modal title={t('Edit {crop} listing', { crop: item.cropName })} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        <Field label={`${t('Quantity available')} (${item.unit})`}><input className="input" type="number" step="0.01" min="0" value={f.quantityAvailable} onChange={set('quantityAvailable')} required /></Field>
        <Field label={t('Price per unit (RWF)')}><input className="input" type="number" min="0" value={f.pricePerUnit} onChange={set('pricePerUnit')} required /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label={t('Available from')}><input className="input" type="date" value={f.availableFrom} onChange={set('availableFrom')} required /></Field>
          <Field label={t('Until')}><input className="input" type="date" value={f.availableUntil} onChange={set('availableUntil')} /></Field>
        </div>
        <div className="grid grid-cols-2 gap-3"><Extra f={f} set={set} /></div>
        <Field label={t('Notes')}><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} /></Field>
        <div className="flex items-center gap-3">
          <Photo url={item.photoUrl} className="h-16 w-16" />
          <div className="flex-1">
            <Field label={t('Photo')} hint={t('A clear photo helps shops trust your produce.')}><input ref={fileRef} className="text-sm" type="file" accept="image/*" /></Field>
            {item.photoUrl && <button type="button" className="mt-1 text-xs text-red-700 underline" onClick={removePhoto}>{t('Remove photo')}</button>}
          </div>
        </div>
        <button className="btn-primary w-full" disabled={busy}>{busy ? t('Saving…') : t('Save changes')}</button>
      </form>
    </Modal>
  );
}

export default function Supplies() {
  const { t } = useT();
  const crops = useCrops();
  const list = useLoad(() => api('/api/supplies/mine'));
  const [f, setF] = useState({ cropId: '', quantityAvailable: '', pricePerUnit: '', availableFrom: today(), availableUntil: '', qualityGrade: '', harvestDate: '', minOrderQuantity: '', pickupPoint: '', notes: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const [edit, setEdit] = useState(null);
  const fileRef = useRef(null);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      const created = await api('/api/supplies', { method: 'POST', body: {
        cropId: Number(f.cropId), quantityAvailable: Number(f.quantityAvailable), pricePerUnit: Number(f.pricePerUnit),
        availableFrom: f.availableFrom, availableUntil: f.availableUntil || null, ...common(f) } });
      const file = fileRef.current?.files?.[0];
      if (file) {
        try { await sendPhoto(created.id, file); } catch (ex) { setErr(t('Listing saved, but the photo could not be uploaded: {m}', { m: t(ex.message) })); }
        fileRef.current.value = '';
      }
      setF({ ...f, quantityAvailable: '', notes: '', harvestDate: '' });
      list.reload();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  async function withdraw(id) {
    if (!confirm(t('Withdraw this listing?'))) return;
    try { await api(`/api/supplies/${id}/withdraw`, { method: 'PATCH' }); list.reload(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title={t('My supply')} subtitle={t('List the produce you can deliver.')} />
      <form onSubmit={submit} className="card mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Field label={t('Crop')}><CropSelect crops={crops.data} value={f.cropId} onChange={set('cropId')} /></Field>
        <Field label={t('Quantity available')}><input className="input" type="number" step="0.01" min="0.01" value={f.quantityAvailable} onChange={set('quantityAvailable')} required /></Field>
        <Field label={t('Price per unit (RWF)')}><input className="input" type="number" min="0" value={f.pricePerUnit} onChange={set('pricePerUnit')} required /></Field>
        <Field label={t('Available from')}><input className="input" type="date" value={f.availableFrom} onChange={set('availableFrom')} required /></Field>
        <Field label={t('Available until')} hint={t('Optional')}><input className="input" type="date" value={f.availableUntil} onChange={set('availableUntil')} /></Field>
        <Extra f={f} set={set} />
        <div className="sm:col-span-2"><Field label={t('Notes (quality, harvest date…)')}><input className="input" maxLength={500} value={f.notes} onChange={set('notes')} /></Field></div>
        <div className="sm:col-span-2"><Field label={t('Photo')} hint={t('Optional. It is shrunk automatically.')}><input ref={fileRef} className="text-sm" type="file" accept="image/*" /></Field></div>
        <div className="flex items-end"><button className="btn-primary w-full" disabled={busy}>{busy ? t('Saving…') : t('Add listing')}</button></div>
        <div className="sm:col-span-2 lg:col-span-4"><Alert>{err}</Alert></div>
      </form>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={['', t('Crop'), t('Available'), t('Price'), t('Window'), t('Status'), '']} empty={t('You have no listings yet.')}>
          {list.data.map((s) => (
            <tr key={s.id}>
              <td className="td"><Photo url={s.photoUrl} className="h-12 w-12" /></td>
              <td className="td font-medium">{s.cropName} <GradeTag grade={s.qualityGrade} />
                {s.pickupPoint && <div className="text-xs font-normal text-stone-500">{t('Pickup: {p}', { p: s.pickupPoint })}</div>}
              </td>
              <td className="td">{fmt(s.quantityAvailable)} {s.unit}
                {s.minOrderQuantity != null && <div className="text-xs text-stone-500">{t('Min {n}', { n: fmt(s.minOrderQuantity) })}</div>}
              </td>
              <td className="td">{rwf(s.pricePerUnit)}</td>
              <td className="td">{fmtDate(s.availableFrom)} → {s.availableUntil ? fmtDate(s.availableUntil) : t('open')}</td>
              <td className="td"><Badge value={s.status} /></td>
              <td className="td whitespace-nowrap text-right">
                {s.status !== 'WITHDRAWN' && (
                  <>
                    <button className="btn-secondary btn-sm mr-1" onClick={() => setEdit(s)}>{t('Edit')}</button>
                    <button className="btn-secondary btn-sm" onClick={() => withdraw(s.id)}>{t('Withdraw')}</button>
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
