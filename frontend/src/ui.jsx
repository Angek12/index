import { useEffect } from 'react';
import { photoSrc } from './api.js';
import { fmt } from './hooks.js';
import { useT } from './i18n.jsx';

export function Spinner() {
  return (
    <div className="flex justify-center py-10">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-stone-200 border-t-green-700" />
    </div>
  );
}

export function Alert({ type = 'error', children }) {
  const { t } = useT();
  if (!children) return null;
  const styles = {
    error: 'border-red-200 bg-red-50 text-red-800',
    success: 'border-green-200 bg-green-50 text-green-800',
    info: 'border-sky-200 bg-sky-50 text-sky-800',
  };
  const text = typeof children === 'string' ? t(children) : children;
  return <div className={`rounded-lg border px-3 py-2 text-sm ${styles[type]}`}>{text}</div>;
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
  OPEN: ['bg-sky-100 text-sky-800', 'Open'],
  PARTIAL: ['bg-amber-100 text-amber-800', 'Partly matched'],
  MATCHED: ['bg-amber-100 text-amber-800', 'Matched'],
  CONFIRMED: ['bg-indigo-100 text-indigo-800', 'Confirmed'],
  DELIVERED: ['bg-teal-100 text-teal-800', 'Delivered'],
  COMPLETED: ['bg-green-100 text-green-800', 'Completed'],
  FULFILLED: ['bg-green-100 text-green-800', 'Fulfilled'],
  AVAILABLE: ['bg-green-100 text-green-800', 'Available'],
  SOLD_OUT: ['bg-stone-200 text-stone-700', 'Sold out'],
  WITHDRAWN: ['bg-stone-200 text-stone-700', 'Withdrawn'],
  CANCELLED: ['bg-stone-200 text-stone-700', 'Cancelled'],
  FAILED: ['bg-red-100 text-red-800', 'Failed'],
  UNPAID: ['bg-stone-200 text-stone-700', 'Unpaid'],
  PAID: ['bg-amber-100 text-amber-800', 'Paid, waiting for farmer'],
  RECEIVED: ['bg-green-100 text-green-800', 'Payment received'],
  PENDING: ['bg-amber-100 text-amber-800', 'Pending approval'],
  APPROVED: ['bg-green-100 text-green-800', 'Approved'],
  REJECTED: ['bg-red-100 text-red-800', 'Rejected'],
  RESOLVED: ['bg-green-100 text-green-800', 'Resolved'],
  DISMISSED: ['bg-stone-200 text-stone-700', 'Dismissed'],
  ANSWERED: ['bg-green-100 text-green-800', 'Answered'],
  CLOSED: ['bg-stone-200 text-stone-700', 'Closed'],
};
export function Badge({ value }) {
  const { t } = useT();
  const [cls, label] = BADGE[value] || ['bg-stone-100 text-stone-700', String(value).replace('_', ' ')];
  return <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${cls}`}>{t(label)}</span>;
}

export function Table({ head, children, empty }) {
  const { t } = useT();
  const rows = Array.isArray(children) ? children.length : children ? 1 : 0;
  return (
    <div className="card overflow-x-auto p-0">
      <table className="min-w-full divide-y divide-stone-200">
        <thead className="bg-stone-50">
          <tr>{head.map((h, i) => <th key={i} className="th">{h}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-stone-100">{children}</tbody>
      </table>
      {rows === 0 && <div className="p-6 text-center text-sm text-stone-500">{empty || t('Nothing here yet.')}</div>}
    </div>
  );
}

export function Modal({ title, onClose, children }) {
  const { t } = useT();
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="no-print fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-4" onClick={onClose}>
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-t-2xl bg-white p-5 shadow-xl sm:rounded-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="rounded p-1 text-stone-500 hover:bg-stone-100" aria-label={t('Close')}>✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function CropSelect({ crops, value, onChange, required = true }) {
  const { t } = useT();
  return (
    <select className="input" value={value} onChange={onChange} required={required}>
      <option value="">{t('Select a crop…')}</option>
      {(crops || []).map((c) => <option key={c.id} value={c.id}>{c.name} ({c.unit})</option>)}
    </select>
  );
}

/** Row of stars. Pass onChange to let people pick a rating. */
export function Stars({ value = 0, onChange }) {
  return (
    <span className="inline-flex text-xl leading-none" aria-label={`${value} / 5`}>
      {[1, 2, 3, 4, 5].map((n) => (
        onChange
          ? <button type="button" key={n} onClick={() => onChange(n)} className={n <= value ? 'text-amber-500' : 'text-stone-300'} aria-label={`${n}`}>★</button>
          : <span key={n} className={n <= value ? 'text-amber-500' : 'text-stone-300'}>★</span>
      ))}
    </span>
  );
}

/** Short reliability line: stars, number of ratings and completion rate. */
export function RepLine({ rep }) {
  const { t } = useT();
  if (!rep) return null;
  const hasStars = rep.avgStars !== null && rep.avgStars !== undefined;
  const hasRel = rep.reliabilityPercent !== null && rep.reliabilityPercent !== undefined;
  if (!hasStars && !hasRel) return <span className="text-xs text-stone-500">{t('New, no history yet')}</span>;
  return (
    <span className="text-xs text-stone-600">
      {hasStars && <>★ {fmt(rep.avgStars)} ({rep.ratingCount})</>}
      {hasStars && hasRel && ' · '}
      {hasRel && t('{n}% completed', { n: fmt(rep.reliabilityPercent) })}
    </span>
  );
}

export function Photo({ url, className = 'h-16 w-16' }) {
  if (!url) return null;
  return <img src={photoSrc(url)} alt="" loading="lazy" className={`${className} rounded-lg border border-stone-200 object-cover`} />;
}

export function GradeTag({ grade }) {
  const { t } = useT();
  if (!grade) return null;
  return <span className="rounded bg-stone-100 px-1.5 py-0.5 text-xs font-semibold text-stone-700">{t('Grade {g}', { g: grade })}</span>;
}
