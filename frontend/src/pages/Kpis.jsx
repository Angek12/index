import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api.js';
import { fmt, rwf, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, PageHeader, Spinner, Stat, Table } from '../ui.jsx';

const pct = (v) => (v === null || v === undefined ? '—' : `${fmt(v)}%`);

function Group({ title, children }) {
  return (
    <>
      <h2 className="mb-2 mt-6 font-semibold">{title}</h2>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">{children}</div>
    </>
  );
}

export default function Kpis() {
  const { t } = useT();
  const [days, setDays] = useState(30);
  const res = useLoad(() => api('/api/admin/kpis', { params: { days } }), [days]);
  const k = res.data;
  const weekly = k ? k.weekly.map((w) => ({ week: w.weekStart.slice(5), orders: w.orders, value: Number(w.value) })) : [];

  return (
    <>
      <PageHeader title={t('KPI dashboard')} subtitle={t('How well the platform is working. Based on the period you choose.')}
        action={
          <div className="no-print flex items-center gap-2">
            <select className="input w-auto" value={days} onChange={(e) => setDays(Number(e.target.value))}>
              <option value={30}>{t('Last 30 days')}</option><option value={90}>{t('Last 90 days')}</option><option value={365}>{t('Last 12 months')}</option>
            </select>
            <button className="btn-secondary btn-sm" onClick={() => window.print()}>{t('Print or save as PDF')}</button>
          </div>} />
      {res.loading ? <Spinner /> : res.error ? <Alert>{res.error}</Alert> : (
        <>
          <Group title={t('Trading')}>
            <Stat label={t('New requests')} value={fmt(k.newRequests)} />
            <Stat label={t('Orders created')} value={fmt(k.ordersCreated)} sub={t('{n} completed', { n: fmt(k.ordersCompleted) })} />
            <Stat label={t('Value traded')} value={rwf(k.valueTraded)} sub={t('completed orders')} />
            <Stat label={t('Quantity delivered')} value={fmt(k.quantityDelivered)} />
          </Group>
          <Group title={t('Speed and quality')}>
            <Stat label={t('Average time to match')} value={k.avgHoursToMatch == null ? '—' : t('{n} hours', { n: fmt(k.avgHoursToMatch) })} sub={t('admin matches only')} />
            <Stat label={t('Requests matched')} value={pct(k.matchedSharePercent)} />
            <Stat label={t('On-time delivery')} value={pct(k.onTimeDeliveryPercent)} />
            <Stat label={t('Fulfilment rate')} value={pct(k.fulfilmentPercent)} sub={t('completed / (completed + failed)')} />
            <Stat label={t('Spoilage')} value={pct(k.spoilagePercent)} sub={t('of delivered quantity')} />
            <Stat label={t('Payments confirmed')} value={pct(k.paymentsReceivedPercent)} sub={t('of completed value')} />
          </Group>
          <Group title={t('People')}>
            <Stat label={t('Active shops')} value={fmt(k.activeShops)} />
            <Stat label={t('Active farmers')} value={fmt(k.activeFarmers)} />
            <Stat label={t('Shops ordering again')} value={pct(k.repeatShopsPercent)} sub={t('2 or more orders')} />
            <Stat label={t('Farmers selling again')} value={pct(k.repeatFarmersPercent)} sub={t('2 or more orders')} />
          </Group>
          <Group title={t('Trust and channels')}>
            <Stat label={t('Open problems')} value={fmt(k.openIssues)} />
            <Stat label={t('Problem rate')} value={pct(k.issueRatePercent)} sub={t('reports per order')} />
            <Stat label={t('Marketplace share')} value={pct(k.marketplaceSharePercent)} sub={t('orders placed directly by shops')} />
          </Group>

          <h2 className="mb-2 mt-6 font-semibold">{t('Orders per week')}</h2>
          <div className="card h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={weekly} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="week" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v, name) => (name === t('Value') ? rwf(v) : v)} />
                <Bar dataKey="orders" name={t('Orders')} fill="#15803d" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <h2 className="mb-2 mt-6 font-semibold">{t('By district')}</h2>
          <Table head={[t('District'), t('Shops'), t('Farmers'), t('Orders'), t('Value'), t('Still needed'), t('Available'), t('Gap')]} empty={t('No data yet.')}>
            {k.districts.map((d) => {
              const gap = Number(d.openDemand) - Number(d.availableSupply);
              return (
                <tr key={d.district}>
                  <td className="td font-medium">{d.district}</td>
                  <td className="td">{d.shops}</td>
                  <td className="td">{d.farmers}</td>
                  <td className="td">{d.orders}</td>
                  <td className="td">{rwf(d.value)}</td>
                  <td className="td">{fmt(d.openDemand)}</td>
                  <td className="td">{fmt(d.availableSupply)}</td>
                  <td className={`td font-semibold ${gap > 0 ? 'text-red-700' : 'text-green-700'}`}>{gap > 0 ? t('Short {n}', { n: fmt(gap) }) : gap < 0 ? t('Surplus {n}', { n: fmt(-gap) }) : t('Balanced')}</td>
                </tr>
              );
            })}
          </Table>
          <p className="mt-2 text-xs text-stone-500">{t('Still needed and Available are in units (kg), across all crops, for requests and listings that are open now.')}</p>
        </>
      )}
    </>
  );
}
