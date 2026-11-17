import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { fmtDateTime, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, Badge, Field, Modal, PageHeader, Spinner, Table } from '../ui.jsx';

const CATEGORIES = { QUESTION: 'Question', PROBLEM: 'Problem', ACCOUNT: 'My account', IDEA: 'Idea' };

function Conversation({ id, isAdmin, onClose }) {
  const { t } = useT();
  const res = useLoad(() => api(`/api/support/tickets/${id}`), [id]);
  const [body, setBody] = useState('');
  const [err, setErr] = useState('');

  async function send(e) {
    e.preventDefault(); setErr('');
    try { await api(`/api/support/tickets/${id}/messages`, { method: 'POST', body: { body } }); setBody(''); res.reload(); } catch (ex) { setErr(ex.message); }
  }
  async function close() {
    try { await api(`/api/support/tickets/${id}/close`, { method: 'PATCH' }); res.reload(); } catch (ex) { setErr(ex.message); }
  }

  const d = res.data;
  return (
    <Modal title={d ? d.ticket.subject : t('Support')} onClose={onClose}>
      {res.loading ? <Spinner /> : res.error ? <Alert>{res.error}</Alert> : (
        <>
          <div className="mb-3 flex flex-wrap items-center gap-2 text-xs text-stone-500">
            <Badge value={d.ticket.status} /><span>{t(CATEGORIES[d.ticket.category])}</span>
            {isAdmin && <span>· {d.ticket.userName} ({t(d.ticket.userRole === 'SHOP' ? 'Shop' : d.ticket.userRole === 'FARMER' ? 'Farmer' : 'Admin')})</span>}
          </div>
          <div className="mb-4 max-h-80 space-y-2 overflow-y-auto">
            {d.messages.map((m) => (
              <div key={m.id} className={`rounded-lg p-3 text-sm ${m.senderRole === 'ADMIN' ? 'bg-green-50' : 'bg-stone-100'}`}>
                <div className="mb-1 text-xs font-semibold text-stone-600">{m.senderRole === 'ADMIN' ? t('Index team') : m.senderName} · {fmtDateTime(m.createdAt)}</div>
                <div className="whitespace-pre-wrap">{m.body}</div>
              </div>
            ))}
          </div>
          <Alert>{err}</Alert>
          {d.ticket.status !== 'CLOSED' ? (
            <form onSubmit={send} className="space-y-2">
              <textarea className="input" rows={3} maxLength={1000} placeholder={t('Write a message…')} value={body} onChange={(e) => setBody(e.target.value)} required />
              <div className="flex gap-2">
                <button className="btn-primary">{t('Send')}</button>
                <button type="button" className="btn-secondary" onClick={close}>{t('Close this conversation')}</button>
              </div>
            </form>
          ) : (
            <form onSubmit={send} className="space-y-2">
              <p className="text-sm text-stone-500">{t('This conversation is closed. Write below to open it again.')}</p>
              <textarea className="input" rows={2} maxLength={1000} value={body} onChange={(e) => setBody(e.target.value)} required />
              <button className="btn-secondary">{t('Open again')}</button>
            </form>
          )}
        </>
      )}
    </Modal>
  );
}

export default function Support() {
  const { user } = useAuth();
  const { t } = useT();
  const isAdmin = user.role === 'ADMIN';
  const [status, setStatus] = useState(isAdmin ? 'OPEN' : '');
  const list = useLoad(() => api('/api/support/tickets', { params: { status } }), [status]);
  const [f, setF] = useState({ subject: '', category: 'QUESTION', message: '' });
  const [err, setErr] = useState('');
  const [open, setOpen] = useState(null);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function create(e) {
    e.preventDefault(); setErr('');
    try {
      const d = await api('/api/support/tickets', { method: 'POST', body: f });
      setF({ subject: '', category: 'QUESTION', message: '' });
      list.reload();
      setOpen(d.ticket.id);
    } catch (ex) { setErr(ex.message); }
  }

  return (
    <>
      <PageHeader title={isAdmin ? t('Support inbox') : t('Support')}
        subtitle={isAdmin ? t('Messages from shops and farmers.') : t('Ask the Index team a question or report a problem with your account.')}
        action={isAdmin && (
          <select className="input w-auto" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="OPEN">{t('Waiting for a reply')}</option><option value="ANSWERED">{t('Answered')}</option><option value="CLOSED">{t('Closed')}</option><option value="">{t('All')}</option>
          </select>
        )} />
      {!isAdmin && (
        <form onSubmit={create} className="card mb-5 grid gap-3 sm:grid-cols-2">
          <h2 className="font-semibold sm:col-span-2">{t('New message')}</h2>
          <div className="sm:col-span-2"><Alert>{err}</Alert></div>
          <Field label={t('Subject')}><input className="input" maxLength={120} value={f.subject} onChange={set('subject')} required /></Field>
          <Field label={t('Topic')}>
            <select className="input" value={f.category} onChange={set('category')}>
              {Object.entries(CATEGORIES).map(([k, v]) => <option key={k} value={k}>{t(v)}</option>)}
            </select>
          </Field>
          <div className="sm:col-span-2"><Field label={t('Message')}><textarea className="input" rows={4} maxLength={1000} value={f.message} onChange={set('message')} required /></Field></div>
          <div className="sm:col-span-2"><button className="btn-primary">{t('Send')}</button></div>
        </form>
      )}
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={isAdmin ? [t('Subject'), t('From'), t('Topic'), t('Status'), t('Last update'), ''] : [t('Subject'), t('Topic'), t('Status'), t('Last update'), '']} empty={t('No messages yet.')}>
          {list.data.map((x) => (
            <tr key={x.id}>
              <td className="td font-medium">{x.subject}</td>
              {isAdmin && <td className="td">{x.userName}<div className="text-xs text-stone-500">{t(x.userRole === 'SHOP' ? 'Shop' : x.userRole === 'FARMER' ? 'Farmer' : 'Admin')}</div></td>}
              <td className="td">{t(CATEGORIES[x.category])}</td>
              <td className="td"><Badge value={x.status} /></td>
              <td className="td">{fmtDateTime(x.updatedAt)}</td>
              <td className="td text-right"><button className="btn-secondary btn-sm" onClick={() => setOpen(x.id)}>{t('Open')}</button></td>
            </tr>
          ))}
        </Table>
      )}
      {open && <Conversation id={open} isAdmin={isAdmin} onClose={() => { setOpen(null); list.reload(); }} />}
    </>
  );
}
