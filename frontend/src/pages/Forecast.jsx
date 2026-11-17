import { useState } from 'react';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { fmt, useCrops, useLoad } from '../hooks.js';
import { useT } from '../i18n.jsx';
import { Alert, CropSelect, Field, PageHeader, Spinner, Stat } from '../ui.jsx';

export default function Forecast() {
  const { user } = useAuth();
  const { t } = useT();
  const crops = useCrops();
  const [cropId, setCropId] = useState('');
  const [weeks, setWeeks] = useState(12);
  const [win, setWin] = useState(4);
  const [shopId, setShopId] = useState('');
  const res = useLoad(
    () => (cropId ? api(`/api/forecast/crops/${cropId}`, { params: { weeks, window: win, shopId: user.role === 'ADMIN' ? shopId : '' } }) : Promise.resolve(null)),
    [cropId, weeks, win, shopId]
  );
  const d = res.data;
  const chart = d ? d.history.map((h) => ({ week: h.weekStart.slice(5), actual: Number(h.actual), predicted: h.predicted == null ? null : Number(h.predicted) })) : [];
  const better = d ? (d.betterModel === 'moving average' ? t('Moving average') : d.betterModel === 'not enough data yet' ? t('Not enough data yet') : t('Same as last week')) : '';

  return (
    <>
      <PageHeader title={t('Demand forecast')} subtitle={t("Weekly sales with a moving-average forecast, tested against 'same as last week'.")} />
      <div className="card mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Field label={t('Crop')}><CropSelect crops={crops.data} value={cropId} onChange={(e) => setCropId(e.target.value)} /></Field>
        <Field label={t('History (weeks)')}><input className="input" type="number" min="4" max="52" value={weeks} onChange={(e) => setWeeks(Number(e.target.value) || 12)} /></Field>
        <Field label={t('Average window (weeks)')}><input className="input" type="number" min="2" max="8" value={win} onChange={(e) => setWin(Number(e.target.value) || 4)} /></Field>
        {user.role === 'ADMIN' && <Field label={t('Shop profile ID')} hint={t('Blank = all shops')}><input className="input" type="number" min="1" value={shopId} onChange={(e) => setShopId(e.target.value)} /></Field>}
      </div>
      {!cropId ? <Alert type="info">{t('Pick a crop to see its forecast.')}</Alert> : res.loading ? <Spinner /> : res.error ? <Alert>{res.error}</Alert> : (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <Stat label={t('Next week forecast')} value={fmt(d.nextWeekForecast)} sub={d.cropName} />
            <Stat label={t('Avg error (moving avg)')} value={fmt(d.maeMovingAverage)} sub={t('mean absolute error')} />
            <Stat label={t('Avg error (last week)')} value={fmt(d.maeNaive)} sub={t('baseline')} />
            <Stat label={t('Better model')} value={better} sub={d.mapeMovingAverage != null ? `MAPE ${fmt(d.mapeMovingAverage)}%` : ''} />
          </div>
          <div className="card mt-4 h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chart} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="week" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="actual" name={t('Actual sales')} stroke="#15803d" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="predicted" name={t('Predicted')} stroke="#f59e0b" strokeWidth={2} strokeDasharray="5 4" dot={false} connectNulls />
              </LineChart>
            </ResponsiveContainer>
          </div>
          {d.nextWeekForecast == null && <div className="mt-3"><Alert type="info">{t('Not enough weeks of sales yet. Keep recording sales to unlock the forecast.')}</Alert></div>}
        </>
      )}
    </>
  );
}
