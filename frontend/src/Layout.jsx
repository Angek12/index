import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api } from './api.js';
import { useAuth } from './auth.jsx';
import { fmtDateTime } from './hooks.js';
import { LangSwitch, useT } from './i18n.jsx';
import { queuedSales, syncSales } from './offline.js';

const NAV = {
  SHOP: [['/dashboard', 'Dashboard'], ['/marketplace', 'Marketplace'], ['/sales', 'Sales'], ['/demands', 'Produce requests'], ['/standing', 'Repeating requests'], ['/orders', 'Orders'], ['/prices', 'Prices'], ['/forecast', 'Forecast'], ['/team', 'Team'], ['/support', 'Support'], ['/help', 'Help'], ['/profile', 'Profile']],
  FARMER: [['/dashboard', 'Dashboard'], ['/supplies', 'My supply'], ['/orders', 'Orders'], ['/prices', 'Prices'], ['/team', 'Team'], ['/support', 'Support'], ['/help', 'Help'], ['/profile', 'Profile']],
  ADMIN: [['/dashboard', 'Dashboard'], ['/match', 'Match requests'], ['/orders', 'Orders'], ['/issues', 'Problems'], ['/kpis', 'KPI dashboard'], ['/reports', 'Reports'], ['/forecast', 'Forecast'], ['/prices', 'Prices'], ['/users', 'Users'], ['/crops', 'Crops'], ['/activity', 'Activity log'], ['/support', 'Support inbox'], ['/help', 'Help'], ['/profile', 'Profile']],
};

function Bell() {
  const { t } = useT();
  const nav = useNavigate();
  const [count, setCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);

  const load = useCallback(() => {
    api('/api/notifications/unread-count').then((r) => setCount(r.count)).catch(() => {});
  }, []);

  useEffect(() => {
    load();
    const id = setInterval(load, 30000);
    return () => clearInterval(id);
  }, [load]);

  async function toggle() {
    if (!open) {
      try {
        setItems(await api('/api/notifications'));
        await api('/api/notifications/read-all', { method: 'PATCH' });
        setCount(0);
      } catch { /* ignore */ }
    }
    setOpen(!open);
  }

  return (
    <div className="relative">
      <button onClick={toggle} aria-label={t('Notifications')} className="relative rounded-lg border border-stone-300 px-2.5 py-1.5 text-sm hover:bg-stone-100">
        🔔
        {count > 0 && <span className="absolute -right-1.5 -top-1.5 rounded-full bg-red-600 px-1.5 text-[10px] font-bold text-white">{count}</span>}
      </button>
      {open && (
        <div className="absolute right-0 z-50 mt-2 max-h-96 w-80 max-w-[85vw] overflow-y-auto rounded-xl border border-stone-200 bg-white shadow-lg">
          <div className="border-b px-3 py-2 text-sm font-semibold">{t('Notifications')}</div>
          {items.length === 0 && <div className="p-4 text-sm text-stone-500">{t('No notifications yet.')}</div>}
          {items.map((n) => (
            <button key={n.id} onClick={() => { setOpen(false); if (n.link) nav(n.link); }}
              className={`block w-full border-b border-stone-100 px-3 py-2 text-left text-sm hover:bg-stone-50 ${n.read ? '' : 'bg-green-50'}`}>
              <div>{n.message}</div>
              <div className="text-xs text-stone-500">{fmtDateTime(n.createdAt)}</div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function Layout() {
  const { user, logout } = useAuth();
  const { t } = useT();
  const [open, setOpen] = useState(false);
  const [online, setOnline] = useState(navigator.onLine);
  const [pending, setPending] = useState(queuedSales().length);
  useLocation();
  const links = NAV[user.role] || [];

  // keep language in the account too, so SMS arrive in the same language
  const saveLanguage = (l) => api('/api/auth/me', { method: 'PUT', body: { language: l } }).catch(() => {});

  useEffect(() => {
    const on = () => { setOnline(true); if (user.role === 'SHOP') syncSales(); };
    const off = () => setOnline(false);
    const q = () => setPending(queuedSales().length);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    window.addEventListener('index:queue', q);
    if (user.role === 'SHOP' && navigator.onLine) syncSales();
    return () => {
      window.removeEventListener('online', on);
      window.removeEventListener('offline', off);
      window.removeEventListener('index:queue', q);
    };
  }, [user.role]);

  return (
    <div className="min-h-screen bg-stone-50 md:flex">
      <aside className={`no-print ${open ? 'block' : 'hidden'} border-b border-stone-200 bg-white md:sticky md:top-0 md:block md:h-screen md:w-60 md:shrink-0 md:overflow-y-auto md:border-b-0 md:border-r`}>
        <div className="hidden px-5 py-5 text-xl font-bold text-green-800 md:block">🌱 Index</div>
        <nav className="flex flex-col gap-1 p-3">
          {links.map(([to, label]) => (
            <NavLink key={to} to={to} onClick={() => setOpen(false)}
              className={({ isActive }) => `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-green-100 text-green-900' : 'text-stone-700 hover:bg-stone-100'}`}>
              {t(label)}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-stone-200 p-4 text-sm">
          <div className="font-medium text-stone-900">{user.fullName}</div>
          <div className="text-xs text-stone-500">{t(user.role === 'SHOP' ? 'Shop' : user.role === 'FARMER' ? 'Farmer' : 'Admin')}{user.businessName ? ` · ${user.businessName}` : ''}</div>
          <button onClick={logout} className="btn-secondary btn-sm mt-3 w-full">{t('Log out')}</button>
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <div className="no-print sticky top-0 z-30 flex items-center justify-between gap-2 border-b border-stone-200 bg-white px-4 py-2">
          <div className="flex items-center gap-2">
            <button onClick={() => setOpen(!open)} className="rounded-lg border border-stone-300 px-3 py-1 text-sm md:hidden" aria-label={t('Menu')}>
              {open ? t('Close') : t('Menu')}
            </button>
            <span className="text-lg font-bold text-green-800 md:hidden">🌱 Index</span>
          </div>
          <div className="flex items-center gap-2">
            <LangSwitch onChange={saveLanguage} />
            <Bell />
          </div>
        </div>

        {!online && (
          <div className="no-print bg-amber-100 px-4 py-2 text-center text-sm text-amber-900">
            {t('You are offline. Sales you save stay on this device and are sent when you are back online.')}
          </div>
        )}
        {online && pending > 0 && (
          <div className="no-print bg-sky-100 px-4 py-2 text-center text-sm text-sky-900">
            {t('Sending {n} saved sales…', { n: pending })}
          </div>
        )}

        <main className="p-4 sm:p-6">
          <div className="mx-auto max-w-5xl"><Outlet /></div>
        </main>
      </div>
    </div>
  );
}
