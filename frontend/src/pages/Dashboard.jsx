import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { fmt, fmtDate, rwf, useLoad } from '../hooks.js';
import { Alert, Badge, PageHeader, Spinner, Stat, Table } from '../ui.jsx';

const ACTIVE = ['MATCHED', 'CONFIRMED', 'DELIVERED'];

function RecentOrders({ orders }) {
  return (
    <>
      <div className="mb-2 mt-6 flex items-center justify-between">
        <h2 className="font-semibold">Recent orders</h2>
        <Link to="/orders" className="text-sm font-medium text-green-700 underline">View all</Link>
      </div>
      <Table head={['Crop', 'Quantity', 'Delivery', 'Status']} empty="No orders yet.">
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

function ShopDashboard() {
  const { data, loading, error } = useLoad(() => Promise.all([api('/api/sales'), api('/api/demands/mine'), api('/api/orders')]));
  if (loading) return <Spinner />;
  if (error) return <Alert>{error}</Alert>;
  const [sales, demands, orders] = data;
  const weekAgo = new Date(Date.now() - 7 * 864e5).toISOString().slice(0, 10);
  const week = sales.filter((s) => s.soldOn >= weekAgo);
  return (
    <>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Sold (7 days)" value={fmt(week.reduce((a, s) => a + Number(s.quantity), 0))} sub="kg recorded" />
        <Stat label="Sales value (7 days)" value={rwf(week.reduce((a, s) => a + Number(s.total), 0))} />
        <Stat label="Open requests" value={demands.filter((d) => d.status === 'OPEN').length} />
        <Stat label="Active orders" value={orders.filter((o) => ACTIVE.includes(o.status)).length} />
      </div>
      <div className="mt-4 flex flex-wrap gap-2">
        <Link to="/sales" className="btn-primary">+ Record sales</Link>
        <Link to="/demands" className="btn-secondary">Request produce</Link>
      </div>
      <RecentOrders orders={orders} />
    </>
  );
}

function FarmerDashboard() {
  const { data, loading, error } = useLoad(() => Promise.all([api('/api/supplies/mine'), api('/api/orders'), api('/api/market/demand')]));
  if (loading) return <Spinner />;
  if (error) return <Alert>{error}</Alert>;
  const [supplies, orders, market] = data;
  return (
    <>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
        <Stat label="Active listings" value={supplies.filter((s) => s.status === 'AVAILABLE').length} />
        <Stat label="Active orders" value={orders.filter((o) => ACTIVE.includes(o.status)).length} />
        <Stat label="Completed orders" value={orders.filter((o) => o.status === 'COMPLETED').length} />
      </div>
      <div className="mt-4"><Link to="/supplies" className="btn-primary">+ List produce</Link></div>
      <h2 className="mb-2 mt-6 font-semibold">What shops are asking for right now</h2>
      <Table head={['Crop', 'Total wanted', 'Earliest needed', 'Requests']} empty="No open requests from shops at the moment.">
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
  const { data, loading, error } = useLoad(() => api('/api/admin/dashboard'));
  if (loading) return <Spinner />;
  if (error) return <Alert>{error}</Alert>;
  const by = data.ordersByStatus || {};
  return (
    <>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Shops" value={data.shops} />
        <Stat label="Farmers" value={data.farmers} />
        <Stat label="Open requests" value={data.openDemandRequests} sub="waiting for a match" />
        <Stat label="Available listings" value={data.availableSupplyListings} />
      </div>
      <h2 className="mb-2 mt-6 font-semibold">Orders by status</h2>
      <div className="flex flex-wrap gap-2">
        {Object.keys(by).length === 0 && <span className="text-sm text-stone-500">No orders yet.</span>}
        {Object.entries(by).map(([k, v]) => (
          <div key={k} className="card flex items-center gap-2 py-2"><Badge value={k} /><span className="font-bold">{v}</span></div>
        ))}
      </div>
      <div className="mt-6 flex flex-wrap gap-2">
        <Link to="/match" className="btn-primary">Match requests</Link>
        <Link to="/reports" className="btn-secondary">Weekly report</Link>
      </div>
    </>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const View = { SHOP: ShopDashboard, FARMER: FarmerDashboard, ADMIN: AdminDashboard }[user.role];
  return (
    <>
      <PageHeader title={`Hello, ${user.fullName.split(' ')[0]}`} subtitle={user.businessName || 'Platform overview'} />
      <View />
    </>
  );
}
