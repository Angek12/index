import { api } from './api.js';

const KEY = 'index_offline_sales';

export const queuedSales = () => {
  try { return JSON.parse(localStorage.getItem(KEY) || '[]'); } catch { return []; }
};

const save = (items) => {
  if (items.length) localStorage.setItem(KEY, JSON.stringify(items));
  else localStorage.removeItem(KEY);
  window.dispatchEvent(new Event('index:queue'));
};

export function enqueueSales(items) {
  save([...queuedSales(), ...items]);
}

/** Sends saved sales to the server. Returns how many were sent. Safe to call at any time. */
export async function syncSales() {
  let sent = 0;
  let queue = queuedSales();
  while (queue.length) {
    const chunk = queue.slice(0, 50);
    try {
      await api('/api/sales/batch', { method: 'POST', body: { items: chunk } });
    } catch (e) {
      if (e.status === 0 || e.status === 401 || e.status >= 500) break; // try again later
      // the server rejected these entries (for example a crop was hidden): drop them so they do not block the rest
    }
    sent += chunk.length;
    queue = queue.slice(50);
    save(queue);
  }
  return sent;
}
