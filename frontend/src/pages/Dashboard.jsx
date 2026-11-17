import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { fmt, fmtDate, rwf, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, PageHeader, RepLine, Spinner, Stat, Table } from '../ui.jsx';

const ACTIVE = ['MATCHED', 'CONFIRMED', 'DELIVERED'];

function RecentOrders({ orders }) {
  const { t } = useT();
  return (
    <>
      <div className="mb-2 mt-6 flex items-center justify-between">
        <h2 className="font-semibold">{t('Recent orders')}</h2>
        <Link to="/orders" className="text-sm font-medium text-green-700 underline">{t('View all')}</Link>
      </div>
      <Table head={[t('Crop'), t('Quantity'), t('Delivery'), t('Status')]} empty={t('No orders yet.')}>
        {orders.slice(0, 5).map((o) => (
          <tr key={o.id}>
            <td className="td">{o.cropName}</td>
            <td className="td">{fmt(o.quantity)} {o.unit}</td>
            <td className="td">{fmtDate(o.deliveryDate)}</td>
            <td className="td"><Badge value={o.status} /></td>
          </tr>
        ))}
      </Table>
    </>
  );
}

function Reputation() {
  const { t } = useT();
  const { data } = useLoad(() => api('/api/reputation/me'));
  if (!data) return null;
  return (
    <div className="card mt-3 flex flex-wrap items-center justify-between gap-2">
      <span className="text-sm font-medium">{t('Your reputation')}</span>
      <RepLine rep={data} />
    </div>
  );
}

function ShopDashboard() {
  const { t } = useT();
  const { data, loading, error } = useLoad(() => Promise.all([api('/api/sales'), api('/api/demands/mine'), api('/api/orders')]));
  if (loading) return <Spinner />;
  if (error) return <Alert>{error}</Alert>;
  const [sales, demands, orders] = data;
  const weekAgo = new Date(Date.now() - 7 * 864e5).toISOString().slice(0, 10);
  const week = sales.filter((s) => s.soldOn >= weekAgo);
  return (
    <>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label={t('Sold (7 days)')} value={fmt(week.reduce((a, s) => a + Number(s.quantity), 0))} sub={t('units recorded')} />
        <Stat label={t('Sales value (7 days)')} value={rwf(week.reduce((a, s) => a + Number(s.total), 0))} />
        <Stat label={t('Open requests')} value={demands.filter((d) => d.status === 'OPEN' || d.status === 'PARTIAL').length} />
        <Stat label={t('Active orders')} value={orders.filter((o) => ACTIVE.includes(o.status)).length} />
      </div>
      <div className="mt-4 flex flex-wrap gap-2">
        <Link to="/sales" className="btn-primary">+ {t('Record sales')}</Link>
        <Link to="/demands" className="btn-secondary">{t('Request produce')}</Link>
        <Link to="/marketplace" className="btn-secondary">{t('Marketplace')}</Link>
      </div>
      <Reputation />
      <RecentOrders orders={orders} />
    </>
  );
}

function FarmerDashboard() {
  const { t } = useT();
  const { data, loading, error } = useLoad(() => Promise.all([api('/api/supplies/mine'), api('/api/orders'), api('/api/market/demand')]));
  if (loading) return <Spinner />;
  if (error) return <Alert>{error}</Alert>;
  const [supplies, orders, market] = data;
  return (
    <>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
        <Stat label={t('Active listings')} value={supplies.filter((s) => s.status === 'AVAILABLE').length} />
        <Stat label={t('Active orders')} value={orders.filter((o) => ACTIVE.includes(o.status)).length} />
        <Stat label={t('Completed orders')} value={orders.filter((o) => o.status === 'COMPLETED').length} />
      </div>
      <div className="mt-4"><Link to="/supplies" className="btn-primary">+ {t('List produce')}</Link></div>
      <Reputation />
      <h2 className="mb-2 mt-6 font-semibold">{t('What shops are asking for right now')}</h2>
      <Table head={[t('Crop'), t('Total wanted'), t('Earliest needed'), t('Requests')]} empty={t('No open requests from shops at the moment.')}>
        {market.map((m) => (
          <tr key={m.cropId}>
            <td className="td font-medium">{m.cropName}</td>
            <td className="td">{fmt(m.totalQuantity)} {m.unit}</td>
            <td className="td">{fmtDate(m.earliestNeededBy)}</td>
            <td className="td">{m.requestCount}</td>
          </tr>
        ))}
      </Table>
      <RecentOrders orders={orders} />
    </>
  );
}

function AdminDashboard() {
  const { t } = useT();
  const { data, loading, error } = useLoad(() => api('/api/admin/dashboard'));
  const att = useLoad(() => api('/api/admin/attention'));
  if (loading) return <Spinner />;
  if (error) return <Alert>{error}</Alert>;
  const by = data.ordersByStatus || {};
  return (
    <>
      {data.pendingAccounts > 0 && (
        <Link to="/users" className="mb-4 block rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          {t('{n} new accounts are waiting for your approval.', { n: data.pendingAccounts })} <span className="font-semibold underline">{t('Review')}</span>
        </Link>
      )}
      {att.data && att.data.total > 0 && (
        <div className="mb-4 rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900">
          <div className="mb-1 font-semibold">{t('Needs your attention')}</div>
          <ul className="list-disc space-y-0.5 pl-5">
            {att.data.items.filter((i) => i.count > 0).map((i) => <li key={i.key}>{i.count} {t(i.label)}</li>)}
          </ul>
        </div>
      )}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label={t('Shops')} value={data.shops} />
        <Stat label={t('Farmers')} value={data.farmers} />
        <Stat label={t('Open requests')} value={data.openDemandRequests} sub={t('waiting for a match')} />
        <Stat label={t('Available listings')} value={data.availableSupplyListings} />
      </div>
      <h2 className="mb-2 mt-6 font-semibold">{t('Orders by status')}</h2>
      <div className="flex flex-wrap gap-2">
        {Object.keys(by).length === 0 && <span className="text-sm text-stone-500">{t('No orders yet.')}</span>}
        {Object.entries(by).map(([k, v]) => (
          <div key={k} className="card flex items-center gap-2 py-2"><Badge value={k} /><span className="font-bold">{v}</span></div>
        ))}
      </div>
      <div className="mt-6 flex flex-wrap gap-2">
        <Link to="/match" className="btn-primary">{t('Match requests')}</Link>
        <Link to="/reports" className="btn-secondary">{t('Weekly report')}</Link>
        <Link to="/kpis" className="btn-secondary">{t('KPI dashboard')}</Link>
        <Link to="/issues" className="btn-secondary">{t('Problems')}</Link>
      </div>
    </>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const { t } = useT();
  const View = { SHOP: ShopDashboard, FARMER: FarmerDashboard, ADMIN: AdminDashboard }[user.role];
  return (
    <>
      <PageHeader title={t('Hello, {name}', { name: user.fullName.split(' ')[0] })} subtitle={user.businessName || t('Platform overview')} />
      <View />
    </>
  );
}
