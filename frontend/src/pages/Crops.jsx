import { useState } from 'react';
import { api } from '../api.js';
import { useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Field, Modal, PageHeader, Spinner, Table } from '../ui.jsx';

function CropForm({ initial, onDone, onClose }) {
  const { t } = useT();
  const [f, setF] = useState({ name: initial?.name || '', category: initial?.category || '', unit: initial?.unit || 'kg', active: initial?.active ?? true });
  const [err, setErr] = useState('');
  async function submit(e) {
    e.preventDefault(); setErr('');
    try {
      await api(initial ? `/api/crops/${initial.id}` : '/api/crops', { method: initial ? 'PUT' : 'POST', body: f });
      onDone();
    } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={initial ? t('Edit crop') : t('Add crop')} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Alert>{err}</Alert>
        <Field label={t('Name')}><input className="input" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} required /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label={t('Category')}><input className="input" value={f.category} onChange={(e) => setF({ ...f, category: e.target.value })} /></Field>
          <Field label={t('Unit')}><input className="input" value={f.unit} onChange={(e) => setF({ ...f, unit: e.target.value })} required /></Field>
        </div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> {t('Active (selectable by users)')}</label>
        <button className="btn-primary w-full">{t('Save')}</button>
      </form>
    </Modal>
  );
}

export default function Crops() {
  const { t } = useT();
  const list = useLoad(() => api('/api/crops', { params: { includeInactive: true } }));
  const [edit, setEdit] = useState(undefined); // undefined = closed, null = new
  return (
    <>
      <PageHeader title={t('Crops')} subtitle={t('Start with your 2–3 pilot crops; hide the rest.')}
        action={<button className="btn-primary" onClick={() => setEdit(null)}>+ {t('Add crop')}</button>} />
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('Name'), t('Category'), t('Unit'), t('Active'), '']}>
          {list.data.map((c) => (
            <tr key={c.id}>
              <td className="td font-medium">{c.name}</td>
              <td className="td">{c.category || '—'}</td>
              <td className="td">{c.unit}</td>
              <td className="td">{c.active ? t('Yes') : t('No')}</td>
              <td className="td text-right"><button className="btn-secondary btn-sm" onClick={() => setEdit(c)}>{t('Edit')}</button></td>
            </tr>
          ))}
        </Table>
      )}
      {edit !== undefined && <CropForm initial={edit} onClose={() => setEdit(undefined)} onDone={() => { setEdit(undefined); list.reload(); }} />}
    </>
  );
}
