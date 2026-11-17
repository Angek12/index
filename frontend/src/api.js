const BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const TOKEN_KEY = 'index_token';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (t) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY));
export const apiBase = BASE;

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function send(url, init) {
  const token = getToken();
  let res;
  try {
    res = await fetch(url, {
      ...init,
      headers: { ...(init.headers || {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    });
  } catch {
    throw new ApiError('Cannot reach the server. Check your connection.', 0);
  }
  if (res.status === 401 && token) {
    setToken(null);
    window.dispatchEvent(new Event('index:logout'));
  }
  return res;
}

async function parse(res) {
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* not JSON */ }
  if (!res.ok) {
    let msg = (data && data.message) || `Request failed (${res.status})`;
    if (data && data.fieldErrors) {
      msg += ': ' + Object.entries(data.fieldErrors).map(([f, m]) => `${f} ${m}`).join('; ');
    }
    throw new ApiError(msg, res.status);
  }
  return data;
}

function buildUrl(path, params) {
  const url = new URL(BASE + path);
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v);
    });
  }
  return url;
}

export async function api(path, { method = 'GET', body, params } = {}) {
  const res = await send(buildUrl(path, params), {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  return parse(res);
}

/** Uploads one image file as multipart form data. */
export async function upload(path, file) {
  const fd = new FormData();
  fd.append('file', file, 'photo.jpg');
  return parse(await send(buildUrl(path), { method: 'POST', body: fd }));
}

/** Downloads a file (for example a CSV for Excel) with the signed-in user's permissions. */
export async function download(path, filename, params) {
  const res = await send(buildUrl(path, params), { method: 'GET', headers: {} });
  if (!res.ok) await parse(res);
  const blob = await res.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 2000);
}

export const photoSrc = (url) => (url ? BASE + url : null);
