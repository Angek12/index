import { fmtDateTime, useLoad } from '../hooks.js';
import { api } from '../api.js';
import { useT } from '../i18n.jsx';
import { Alert, PageHeader, Spinner, Table } from '../ui.jsx';

const ACTIONS = {
  USER_APPROVED: 'Account approved', USER_REJECTED: 'Account rejected', USER_ACTIVATED: 'Account activated',
  USER_DEACTIVATED: 'Account deactivated', RESET_CODE_ISSUED: 'Reset code issued', PHONE_VERIFIED: 'Phone confirmed by an admin',
  ADMIN_CREATED: 'Admin added', ISSUE_RESOLVED: 'Problem resolved', ISSUE_DISMISSED: 'Problem dismissed',
  REPEATING_REQUESTS_RUN: 'Repeating requests run by hand',
};

export default function Activity() {
  const { t } = useT();
  const list = useLoad(() => api('/api/admin/audit'));
  return (
    <>
      <PageHeader title={t('Activity log')} subtitle={t('The latest admin actions, newest first.')} />
      {list.loading ? <Spinner /> : list.error ? <Alert>{list.error}</Alert> : (
        <Table head={[t('When'), t('Who'), t('What'), t('Details')]} empty={t('Nothing here yet.')}>
          {list.data.map((a) => (
            <tr key={a.id}>
              <td className="td whitespace-nowrap">{fmtDateTime(a.createdAt)}</td>
              <td className="td">{a.actor || '—'}</td>
              <td className="td">{t(ACTIONS[a.action] || a.action)}</td>
              <td className="td text-xs text-stone-600">{a.targetType ? `${a.targetType} #${a.targetId}` : ''} {a.details}</td>
            </tr>
          ))}
        </Table>
      )}
    </>
  );
}
