import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useLoad } from '../hooks.js';
import { Alert, PageHeader, Spinner, Table } from '../ui.jsx';

export default function Users() {
  const { user: me } = useAuth();
  const [role, setRole] = useState('');
  const list = useLoad(() => api('/api/admin/users', { params: { role } }), [role]);
  const [err, setErr] = useState('');

  async function toggle(u) {
    setErr('');
    try { await api(`/api/admin/users/${u.id}/active`, { method: 'PATCH', params: { value: !u.active } }); list.reload(); }
    catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title="Users" subtitle="Shops, farmers and admins."
        action={<select className="input w-auto" value={role} onChange={(e) => setRole(e.target.value)}>
          <option value="">All roles</option><option value="SHOP">Shops</option><option value="FARMER">Farmers</option><option value="ADMIN">Admins</option>
        </select>} />
      <div className="mb-3"><Alert>{err}</Alert></div>
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={['Name', 'Business', 'Role', 'Contact', 'Location', '']}>
          {list.data.map((u) => (
            <tr key={u.id} className={u.active ? '' : 'opacity-50'}>
              <td className="td font-medium">{u.fullName}</td>
              <td className="td">{u.businessName || '—'}</td>
              <td className="td">{u.role}</td>
              <td className="td">{u.email}<br /><span className="text-xs text-stone-500">{u.phone}</span></td>
              <td className="td">{[u.district, u.sector].filter(Boolean).join(', ') || '—'}</td>
              <td className="td text-right">
                {u.id !== me.id && <button className="btn-secondary btn-sm" onClick={() => toggle(u)}>{u.active ? 'Deactivate' : 'Activate'}</button>}
              </td>
            </tr>
          ))}
        </Table>
      )}
    </>
  );
}
