import { useEffect } from 'react';

export function Spinner() {
  return (
    <div className="flex justify-center py-10">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-stone-200 border-t-green-700" />
    </div>
  );
}

export function Alert({ type = 'error', children }) {
  if (!children) return null;
  const styles = {
    error: 'border-red-200 bg-red-50 text-red-800',
    success: 'border-green-200 bg-green-50 text-green-800',
    info: 'border-sky-200 bg-sky-50 text-sky-800',
  };
  return <div className={`rounded-lg border px-3 py-2 text-sm ${styles[type]}`}>{children}</div>;
}

export function Field({ label, hint, children }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-stone-700">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-stone-500">{hint}</span>}
    </label>
  );
}

export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 className="text-xl font-bold text-stone-900 sm:text-2xl">{title}</h1>
        {subtitle && <p className="text-sm text-stone-500">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function Stat({ label, value, sub }) {
  return (
    <div className="card">
      <div className="text-xs font-medium uppercase tracking-wide text-stone-500">{label}</div>
      <div className="mt-1 text-2xl font-bold text-stone-900">{value}</div>
      {sub && <div className="text-xs text-stone-500">{sub}</div>}
    </div>
  );
}

const BADGE = {
  OPEN: 'bg-sky-100 text-sky-800',
  MATCHED: 'bg-amber-100 text-amber-800',
  CONFIRMED: 'bg-indigo-100 text-indigo-800',
  DELIVERED: 'bg-teal-100 text-teal-800',
  COMPLETED: 'bg-green-100 text-green-800',
  FULFILLED: 'bg-green-100 text-green-800',
  AVAILABLE: 'bg-green-100 text-green-800',
  SOLD_OUT: 'bg-stone-200 text-stone-700',
  WITHDRAWN: 'bg-stone-200 text-stone-700',
  CANCELLED: 'bg-stone-200 text-stone-700',
  FAILED: 'bg-red-100 text-red-800',
};
export function Badge({ value }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${BADGE[value] || 'bg-stone-100 text-stone-700'}`}>
      {String(value).replace('_', ' ')}
    </span>
  );
}

export function Table({ head, children, empty }) {
  const rows = Array.isArray(children) ? children.length : children ? 1 : 0;
  return (
    <div className="card overflow-x-auto p-0">
      <table className="min-w-full divide-y divide-stone-200">
        <thead className="bg-stone-50">
          <tr>{head.map((h) => <th key={h} className="th">{h}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-stone-100">{children}</tbody>
      </table>
      {rows === 0 && <div className="p-6 text-center text-sm text-stone-500">{empty || 'Nothing here yet.'}</div>}
    </div>
  );
}

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-4" onClick={onClose}>
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-t-2xl bg-white p-5 shadow-xl sm:rounded-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="rounded p-1 text-stone-500 hover:bg-stone-100" aria-label="Close">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function CropSelect({ crops, value, onChange, required = true }) {
  return (
    <select className="input" value={value} onChange={onChange} required={required}>
      <option value="">Select a crop…</option>
      {(crops || []).map((c) => <option key={c.id} value={c.id}>{c.name} ({c.unit})</option>)}
    </select>
  );
}
