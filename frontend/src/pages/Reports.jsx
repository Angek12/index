import { useState } from 'react';
import { api } from '../api.js';
import { fmt, fmtDate, rwf, useLoad } from '../hooks.js';
import { Alert, PageHeader, Spinner, Stat, Table } from '../ui.jsx';

export default function Reports() {
  const [weekStart, setWeekStart] = useState('');
  const weekly = useLoad(() => api('/api/reports/weekly-summary', { params: { weekStart } }), [weekStart]);
  const pilot = useLoad(() => api('/api/reports/pilot-economics'));
  const p = pilot.data;
  return (
    <>
      <PageHeader title="Reports" subtitle="Weekly demand vs supply, and pilot results." />

      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-semibold">Weekly summary {weekly.data && `(${fmtDate(weekly.data.weekStart)} – ${fmtDate(weekly.data.weekEnd)})`}</h2>
        <input className="input w-auto" type="date" value={weekStart} onChange={(e) => setWeekStart(e.target.value)} aria-label="Week containing" />
      </div>
      {weekly.loading ? <Spinner /> : weekly.error ? <Alert>{weekly.error}</Alert> : (
        <Table head={['Crop', 'Requested', 'Supply listed', 'Sold (shops)', 'Gap']} empty="No crops.">
          {weekly.data.crops.map((c) => (
            <tr key={c.cropId}>
              <td className="td font-medium">{c.cropName}</td>
              <td className="td">{fmt(c.demandQty)}</td>
              <td className="td">{fmt(c.supplyQty)}</td>
              <td className="td">{fmt(c.soldQty)}</td>
              <td className={`td font-semibold ${Number(c.gap) > 0 ? 'text-red-700' : 'text-green-700'}`}>
                {Number(c.gap) > 0 ? `Short ${fmt(c.gap)}` : Number(c.gap) < 0 ? `Surplus ${fmt(-c.gap)}` : 'Balanced'}
              </td>
            </tr>
          ))}
        </Table>
      )}

      <h2 className="mb-2 mt-8 font-semibold">Pilot economics</h2>
      {pilot.loading ? <Spinner /> : pilot.error ? <Alert>{pilot.error}</Alert> : (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Stat label="Total orders" value={p.totalOrders} sub={`${p.cancelledOrders} cancelled`} />
          <Stat label="Completed" value={p.completedOrders} sub={`${p.failedOrders} failed`} />
          <Stat label="Fulfilment rate" value={p.fulfilmentRatePercent == null ? '—' : `${fmt(p.fulfilmentRatePercent)}%`} sub="completed / (completed + failed)" />
          <Stat label="Cost per completed order" value={rwf(p.transportCostPerCompletedOrder)} sub="transport only" />
          <Stat label="Quantity delivered" value={fmt(p.completedQuantity)} />
          <Stat label="Value traded" value={rwf(p.completedValue)} />
          <Stat label="Transport spent" value={rwf(p.totalTransportCost)} />
          <Stat label="Spoiled quantity" value={fmt(p.spoiledQuantity)} />
        </div>
      )}
    </>
  );
}
