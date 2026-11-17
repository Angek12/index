import { useState } from 'react';
import { api } from '../api.js';
import { fmt, fmtDateTime, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, Field, Modal, PageHeader, Spinner, Table } from '../ui.jsx';
import { ISSUE_TYPES } from './Orders.jsx';

function ResolveModal({ issue, onClose, onDone }) {
  const { t } = useT();
  const [status, setStatus] = useState('RESOLVED');
  const [resolution, setResolution] = useState('');
  const [err, setErr] = useState('');
  async function submit(e) {
    e.preventDefault(); setErr('');
    try { await api(`/api/admin/issues/${issue.id}`, { method: 'PATCH', body: { status, resolution } }); onDone(); } catch (ex) { setErr(ex.message); }
  }
  return (
    <Modal title={`${t('Problem')} · ${issue.cropName} #${issue.orderId}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <div className="rounded-lg bg-stone-50 p-3 text-sm">
          <div className="font-medium">{t(ISSUE_TYPES[issue.type])}</div>
          <div>{issue.description}</div>
          <div className="mt-1 text-xs text-stone-500">{issue.reporterName} ({t(issue.reporterRole === 'SHOP' ? 'Shop' : 'Farmer')})</div>
        </div>
        <Alert>{err}</Alert>
        <Field label={t('Decision')}>
          <select className="input" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="RESOLVED">{t('Resolved')}</option>
            <option value="DISMISSED">{t('Dismissed (no action needed)')}</option>
          </select>
        </Field>
        <Field label={t('What was decided? (both sides are told)')}><textarea className="input" rows={3} maxLength={500} value={resolution} onChange={(e) => setResolution(e.target.value)} required /></Field>
        <button className="btn-primary w-full">{t('Save decision')}</button>
      </form>
    </Modal>
  );
}

export default function Issues() {
  const { t } = useT();
  const [status, setStatus] = useState('OPEN');
  const list = useLoad(() => api('/api/admin/issues', { params: { status } }), [status]);
  const [sel, setSel] = useState(null);
  return (
    <>
      <PageHeader title={t('Problems')} subtitle={t('Problems reported by shops and farmers on their orders.')}
        action={<select className="input w-auto" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="OPEN">{t('Open')}</option><option value="RESOLVED">{t('Resolved')}</option><option value="DISMISSED">{t('Dismissed')}</option><option value="">{t('All')}</option>
        </select>} />
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('Order'), t('Problem'), t('Between'), t('Reported'), t('Status'), '']} empty={t('No problems here.')}>
          {list.data.map((i) => (
            <tr key={i.id}>
              <td className="td">#{i.orderId}<div className="text-xs text-stone-500">{i.cropName}</div></td>
              <td className="td"><div className="font-medium">{t(ISSUE_TYPES[i.type])}</div><div className="max-w-xs text-xs text-stone-600">{i.description}</div>{i.quantityAffected != null && <div className="text-xs text-stone-500">{fmt(i.quantityAffected)}</div>}</td>
              <td className="td text-xs">{i.shopName}<br />{i.farmerName}</td>
              <td className="td text-xs">{i.reporterName}<br />{fmtDateTime(i.createdAt)}</td>
              <td className="td"><Badge value={i.status} />{i.resolution && <div className="mt-1 max-w-xs text-xs text-stone-500">{i.resolution}</div>}</td>
              <td className="td text-right">{i.status === 'OPEN' && <button className="btn-primary btn-sm" onClick={() => setSel(i)}>{t('Decide')}</button>}</td>
            </tr>
          ))}
        </Table>
      )}
      {sel && <ResolveModal issue={sel} onClose={() => setSel(null)} onDone={() => { setSel(null); list.reload(); }} />}
    </>
  );
}
