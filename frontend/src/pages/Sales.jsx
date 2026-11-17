import { useEffect, useState } from 'react';
import { api, download } from '../api.js';
import { fmt, fmtDate, rwf, today, useCrops, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { enqueueSales } from '../offline.js';
import { Alert, CropSelect, Field, Modal, PageHeader, Spinner, Table } from '../ui.jsx';

function EditModal({ item, crops, onClose, onDone }) {
  const { t } = useT();
  const [f, setF] = useState({ cropId: String(item.cropId), quantity: item.quantity, unitPrice: item.unitPrice, soldOn: item.soldOn });
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  async function submit(e) {
    e.preventDefault(); setErr('');
    try {
      await api(`/api/sales/${item.id}`, { method: 'PUT', body: { cropId: Number(f.cropId), quantity: Number(f.quantity), unitPrice: Number(f.unitPrice), soldOn: f.soldOn } });
      onDone();
    } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={t('Edit sale')} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        <Field label={t('Crop')}><CropSelect crops={crops} value={f.cropId} onChange={set('cropId')} /></Field>
        <Field label={t('Quantity sold')}><input className="input" type="number" step="0.01" min="0.01" value={f.quantity} onChange={set('quantity')} required /></Field>
        <Field label={t('Price per unit (RWF)')}><input className="input" type="number" min="0" value={f.unitPrice} onChange={set('unitPrice')} required /></Field>
        <Field label={t('Date')}><input className="input" type="date" max={today()} value={f.soldOn} onChange={set('soldOn')} required /></Field>
        <button className="btn-primary w-full">{t('Save changes')}</button>
      </form>
    </Modal>
  );
}

export default function Sales() {
  const { t } = useT();
  const crops = useCrops();
  const list = useLoad(() => api('/api/sales'));
  const last = useLoad(() => api('/api/sales/last-prices'));
  const [date, setDate] = useState(today());
  const [rows, setRows] = useState({});
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');
  const [busy, setBusy] = useState(false);
  const [edit, setEdit] = useState(null);

  // pre-fill each crop's price with the last price this shop used
  useEffect(() => {
    if (!crops.data || !last.data) return;
    setRows((prev) => {
      const next = { ...prev };
      crops.data.forEach((c) => {
        if (!next[c.id]) next[c.id] = { qty: '', price: last.data[c.id] !== undefined ? String(last.data[c.id]) : '' };
      });
      return next;
    });
  }, [crops.data, last.data]);

  const setRow = (id, k, v) => setRows((p) => ({ ...p, [id]: { ...p[id], [k]: v } }));

  async function saveAll(e) {
    e.preventDefault();
    setErr(''); setOk('');
    const items = Object.entries(rows)
      .filter(([, r]) => Number(r.qty) > 0 && r.price !== '')
      .map(([id, r]) => ({ cropId: Number(id), quantity: Number(r.qty), unitPrice: Number(r.price), soldOn: date }));
    if (items.length === 0) { setErr('Enter a quantity and a price for at least one crop.'); return; }
    setBusy(true);
    const clear = () => setRows((p) => Object.fromEntries(Object.entries(p).map(([id, r]) => [id, { ...r, qty: '' }])));
    try {
      await api('/api/sales/batch', { method: 'POST', body: { items } });
      clear();
      setOk(t('Saved {n} sales.', { n: items.length }));
      list.reload(); last.reload();
    } catch (ex) {
      if (ex.status === 0) {
        enqueueSales(items);
        clear();
        setOk(t('No connection. Saved on this device and will be sent automatically.'));
      } else setErr(ex.message);
    } finally { setBusy(false); }
  }

  async function remove(id) {
    if (!confirm(t('Delete this sales record?'))) return;
    try { await api(`/api/sales/${id}`, { method: 'DELETE' }); list.reload(); } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title={t('Sales records')} subtitle={t('Record what you sold. This powers your demand forecast.')}
        action={<button className="btn-secondary btn-sm" onClick={() => download('/api/export/sales.csv', 'index-sales.csv').catch((ex) => setErr(ex.message))}>{t('Download for Excel')}</button>} />

      <form onSubmit={saveAll} className="card mb-5 space-y-3">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="font-semibold">{t('Quick entry')}</h2>
            <p className="text-xs text-stone-500">{t('Fill in only the crops you sold. Prices are filled in from your last sale.')}</p>
          </div>
          <Field label={t('Date')}><input className="input" type="date" max={today()} value={date} onChange={(e) => setDate(e.target.value)} required /></Field>
        </div>
        <Alert>{err}</Alert>
        <Alert type="success">{ok}</Alert>
        {crops.loading ? <Spinner /> : (
          <div className="divide-y divide-stone-100">
            {(crops.data || []).map((c) => (
              <div key={c.id} className="grid grid-cols-[1fr_6rem_6rem] items-center gap-2 py-2 sm:grid-cols-[1fr_9rem_9rem]">
                <span className="text-sm font-medium">{c.name} <span className="text-xs text-stone-500">({c.unit})</span></span>
                <input className="input" type="number" step="0.01" min="0" inputMode="decimal" placeholder={t('Quantity')} aria-label={`${c.name} ${t('Quantity')}`}
                  value={rows[c.id]?.qty ?? ''} onChange={(e) => setRow(c.id, 'qty', e.target.value)} />
                <input className="input" type="number" min="0" inputMode="numeric" placeholder={t('Price')} aria-label={`${c.name} ${t('Price')}`}
                  value={rows[c.id]?.price ?? ''} onChange={(e) => setRow(c.id, 'price', e.target.value)} />
              </div>
            ))}
          </div>
        )}
        <button className="btn-primary w-full sm:w-auto" disabled={busy}>{busy ? t('Saving…') : t('Save all')}</button>
      </form>

      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('Date'), t('Crop'), t('Quantity'), t('Price'), t('Total'), '']} empty={t('No sales in the last 90 days.')}>
          {list.data.map((s) => (
            <tr key={s.id}>
              <td className="td">{fmtDate(s.soldOn)}</td>
              <td className="td">{s.cropName}</td>
              <td className="td">{fmt(s.quantity)} {s.unit}</td>
              <td className="td">{rwf(s.unitPrice)}</td>
              <td className="td font-medium">{rwf(s.total)}</td>
              <td className="td whitespace-nowrap text-right">
                <button className="btn-secondary btn-sm mr-1" onClick={() => setEdit(s)}>{t('Edit')}</button>
                <button className="btn-secondary btn-sm" onClick={() => remove(s.id)}>{t('Delete')}</button>
              </td>
            </tr>
          ))}
        </Table>
      )}
      {edit && <EditModal item={edit} crops={crops.data} onClose={() => setEdit(null)} onDone={() => { setEdit(null); list.reload(); last.reload(); }} />}
    </>
  );
}
