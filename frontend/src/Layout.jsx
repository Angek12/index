import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './auth.jsx';

const NAV = {
  SHOP: [['/dashboard', 'Dashboard'], ['/sales', 'Sales'], ['/demands', 'Produce requests'], ['/forecast', 'Forecast'], ['/orders', 'Orders'], ['/profile', 'Profile']],
  FARMER: [['/dashboard', 'Dashboard'], ['/supplies', 'My supply'], ['/orders', 'Orders'], ['/profile', 'Profile']],
  ADMIN: [['/dashboard', 'Dashboard'], ['/match', 'Match requests'], ['/orders', 'Orders'], ['/forecast', 'Forecast'], ['/reports', 'Reports'], ['/users', 'Users'], ['/crops', 'Crops'], ['/profile', 'Profile']],
};

export default function Layout() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  useLocation();
  const links = NAV[user.role] || [];

  const nav = (
    <nav className="flex flex-col gap-1 p-3">
      {links.map(([to, label]) => (
        <NavLink
          key={to}
          to={to}
          onClick={() => setOpen(false)}
          className={({ isActive }) =>
            `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-green-100 text-green-900' : 'text-stone-700 hover:bg-stone-100'}`
          }
        >
          {label}
        </NavLink>
      ))}
    </nav>
  );

  return (
    <div className="min-h-screen bg-stone-50 md:flex">
      <header className="sticky top-0 z-40 flex items-center justify-between border-b border-stone-200 bg-white px-4 py-3 md:hidden">
        <span className="text-lg font-bold text-green-800">🌱 Index</span>
        <button onClick={() => setOpen(!open)} className="rounded-lg border border-stone-300 px-3 py-1 text-sm" aria-label="Menu">
          {open ? 'Close' : 'Menu'}
        </button>
      </header>

      <aside className={`${open ? 'block' : 'hidden'} border-b border-stone-200 bg-white md:sticky md:top-0 md:block md:h-screen md:w-60 md:shrink-0 md:border-b-0 md:border-r`}>
        <div className="hidden px-5 py-5 text-xl font-bold text-green-800 md:block">🌱 Index</div>
        {nav}
        <div className="border-t border-stone-200 p-4 text-sm">
          <div className="font-medium text-stone-900">{user.fullName}</div>
          <div className="text-xs text-stone-500">{user.role}{user.businessName ? ` · ${user.businessName}` : ''}</div>
          <button onClick={logout} className="btn-secondary btn-sm mt-3 w-full">Log out</button>
        </div>
      </aside>

      <main className="min-w-0 flex-1 p-4 sm:p-6">
        <div className="mx-auto max-w-5xl"><Outlet /></div>
      </main>
    </div>
  );
}
