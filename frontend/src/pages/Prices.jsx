import { useState } from 'react';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api.js';
import { fmt, rwf, useCrops, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, CropSelect, Field, PageHeader, Spinner, Stat } from '../ui.jsx';

export default function Prices() {
  const { t } = useT();
  const crops = useCrops();
  const [cropId, setCropId] = useState('');
  const [weeks, setWeeks] = useState(12);
  const res = useLoad(() => (cropId ? api(`/api/market/prices/${cropId}`, { params: { weeks } }) : Promise.resolve(null)), [cropId, weeks]);
  const d = res.data;
  const chart = d ? d.points.map((p) => ({ week: p.weekStart.slice(5), retail: p.retailPrice == null ? null : Number(p.retailPrice), agreed: p.agreedPrice == null ? null : Number(p.agreedPrice) })) : [];

  return (
    <>
      <PageHeader title={t('Price trends')} subtitle={t('Average weekly prices. No shop or farmer names are shown.')} />
      <div className="card mb-5 grid gap-3 sm:grid-cols-2">
        <Field label={t('Crop')}><CropSelect crops={crops.data} value={cropId} onChange={(e) => setCropId(e.target.value)} /></Field>
        <Field label={t('History (weeks)')}><input className="input" type="number" min="4" max="52" value={weeks} onChange={(e) => setWeeks(Number(e.target.value) || 12)} /></Field>
      </div>
      {!cropId ? <Alert type="info">{t('Pick a crop to see its prices.')}</Alert> : res.loading ? <Spinner /> : res.error ? <Alert>{res.error}</Alert> : (
        <>
          <div className="grid grid-cols-2 gap-3">
            <Stat label={t('Latest shop selling price')} value={d.latestRetail == null ? '—' : rwf(d.latestRetail)} sub={`${d.cropName} / ${d.unit}`} />
            <Stat label={t('Latest agreed order price')} value={d.latestAgreed == null ? '—' : rwf(d.latestAgreed)} sub={`${d.cropName} / ${d.unit}`} />
          </div>
          <div className="card mt-4 h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chart} margin={{ top: 8, right: 8, left: -4, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="week" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => fmt(v)} />
                <Tooltip formatter={(v) => rwf(v)} />
                <Legend />
                <Line type="monotone" dataKey="retail" name={t('Shop selling price')} stroke="#15803d" strokeWidth={2} dot={false} connectNulls />
                <Line type="monotone" dataKey="agreed" name={t('Agreed order price')} stroke="#f59e0b" strokeWidth={2} dot={false} connectNulls />
              </LineChart>
            </ResponsiveContainer>
          </div>
          {d.latestRetail == null && d.latestAgreed == null && <div className="mt-3"><Alert type="info">{t('No price data for this crop yet.')}</Alert></div>}
        </>
      )}
    </>
  );
}
