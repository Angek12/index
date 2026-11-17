import { useState } from 'react';
import { api, download } from '../api.js';
import { fmt, fmtDate, rwf, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, PageHeader, Spinner, Stat, Table } from '../ui.jsx';

export default function Reports() {
  const { t } = useT();
  const [weekStart, setWeekStart] = useState('');
  const [err, setErr] = useState('');
  const weekly = useLoad(() => api('/api/reports/weekly-summary', { params: { weekStart } }), [weekStart]);
  const pilot = useLoad(() => api('/api/reports/pilot-economics'));
  const p = pilot.data;
  const dl = (path, name, params) => download(path, name, params).catch((ex) => setErr(ex.message));

  return (
    <>
      <PageHeader title={t('Reports')} subtitle={t('Weekly demand vs supply, and pilot results.')}
        action={<button className="btn-secondary btn-sm no-print" onClick={() => window.print()}>{t('Print or save as PDF')}</button>} />
      <div className="mb-3"><Alert>{err}</Alert></div>

      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-semibold">{t('Weekly summary')} {weekly.data && `(${fmtDate(weekly.data.weekStart)} – ${fmtDate(weekly.data.weekEnd)})`}</h2>
        <div className="no-print flex items-center gap-2">
          <input className="input w-auto" type="date" value={weekStart} onChange={(e) => setWeekStart(e.target.value)} aria-label={t('Week containing')} />
          <button className="btn-secondary btn-sm" onClick={() => dl('/api/export/weekly-summary.csv', 'index-weekly-summary.csv', { weekStart })}>{t('Download for Excel')}</button>
        </div>
      </div>
      {weekly.loading ? <Spinner /> : weekly.error ? <Alert>{weekly.error}</Alert> : (
        <Table head={[t('Crop'), t('Requested'), t('Supply listed'), t('Sold (shops)'), t('Balance')]} empty={t('No crops.')}>
          {weekly.data.crops.map((c) => (
            <tr key={c.cropId}>
              <td className="td font-medium">{c.cropName}</td>
              <td className="td">{fmt(c.demandQty)}</td>
              <td className="td">{fmt(c.supplyQty)}</td>
              <td className="td">{fmt(c.soldQty)}</td>
              <td className={`td font-semibold ${Number(c.gap) > 0 ? 'text-red-700' : 'text-green-700'}`}>
                {Number(c.gap) > 0 ? t('Short {n}', { n: fmt(c.gap) }) : Number(c.gap) < 0 ? t('Surplus {n}', { n: fmt(-c.gap) }) : t('Balanced')}
              </td>
            </tr>
          ))}
        </Table>
      )}

      <div className="mb-2 mt-8 flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-semibold">{t('Pilot economics')}</h2>
        <button className="btn-secondary btn-sm no-print" onClick={() => dl('/api/export/pilot.csv', 'index-pilot-results.csv')}>{t('Download for Excel')}</button>
      </div>
      {pilot.loading ? <Spinner /> : pilot.error ? <Alert>{pilot.error}</Alert> : (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Stat label={t('Total orders')} value={p.totalOrders} sub={t('{n} cancelled', { n: p.cancelledOrders })} />
          <Stat label={t('Completed')} value={p.completedOrders} sub={t('{n} failed', { n: p.failedOrders })} />
          <Stat label={t('Fulfilment rate')} value={p.fulfilmentRatePercent == null ? '—' : `${fmt(p.fulfilmentRatePercent)}%`} sub={t('completed / (completed + failed)')} />
          <Stat label={t('Cost per completed order')} value={rwf(p.transportCostPerCompletedOrder)} sub={t('transport only')} />
          <Stat label={t('Quantity delivered')} value={fmt(p.completedQuantity)} />
          <Stat label={t('Value traded')} value={rwf(p.completedValue)} />
          <Stat label={t('Payments confirmed')} value={rwf(p.paymentsReceivedValue)} sub={t('farmers confirmed receiving')} />
          <Stat label={t('Not yet paid')} value={rwf(p.unpaidCompletedValue)} sub={t('completed orders')} />
          <Stat label={t('Transport spent')} value={rwf(p.totalTransportCost)} />
          <Stat label={t('Spoiled quantity')} value={fmt(p.spoiledQuantity)} />
        </div>
      )}
    </>
  );
}
