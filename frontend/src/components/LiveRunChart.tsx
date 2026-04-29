import { useState } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer, ReferenceLine,
} from 'recharts';
import type { RecordedPoint } from '../hooks/useRunRecorder';
import type { ScenarioResult } from '../types';

interface Props {
  points:          RecordedPoint[];
  results:         ScenarioResult[];
  isRunning:       boolean;
  runStartEpochMs: number;
  startEpochMs:    number;
}

type Serie = 'heap' | 'gcPause' | 'threads';

const SERIES: { key: Serie; label: string; color: string; unit: string }[] = [
  { key: 'heap',    label: 'Heap (MB)',      color: '#6366f1', unit: ' MB'  },
  { key: 'gcPause', label: 'GC Pause (ms)',  color: '#ef4444', unit: ' ms'  },
  { key: 'threads', label: 'Threads live',   color: '#22d3ee', unit: ''     },
];

export default function LiveRunChart({ points, results, isRunning, runStartEpochMs, startEpochMs }: Props) {
  const [activeSeries, setActiveSeries] = useState<Set<Serie>>(new Set(['heap', 'gcPause']));

  if (points.length === 0 && !isRunning) return null;

  // Normalisation simple : le graphe commence à t=0 sur le premier point reçu.
  // Pour placer les annotations au bon endroit :
  //   - firstPointEpoch = epoch approximatif du premier point = startEpochMs + points[0].t
  //   - annotation_t = backendOffset - (firstPointEpoch - runStartEpochMs)
  const p0 = points.length > 0 ? points[0].t : 0;
  const normalizedPoints = points.map(p => ({ ...p, t: p.t - p0 }));

  const firstPointEpoch = startEpochMs + p0;
  const annotationShift = runStartEpochMs > 0 ? firstPointEpoch - runStartEpochMs : p0;

  const annotations = results.flatMap(r => [
    { t: r.baselineStartOffsetMs  - annotationShift, label: `${r.scenarioId} · Baseline`,  color: '#6366f1' },
    { t: r.optimizedStartOffsetMs - annotationShift, label: `${r.scenarioId} · Optimized`, color: '#22d3ee' },
    { t: r.optimizedEndOffsetMs   - annotationShift, label: `${r.scenarioId} · Fin`,        color: '#22c55e' },
  ]);

  function toggleSerie(key: Serie) {
    setActiveSeries(prev => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  }

  return (
    <div className="live-run-chart">
      <div className="live-run-chart-header">
        <div className="live-run-chart-title">
          {isRunning
            ? <><span className="live-dot" /> JVM en direct</>
            : <>📊 JVM pendant le run</>}
          <span className="micrometer-tag">Micrometer · 100 ms</span>
        </div>
        <div className="live-run-chart-toggles">
          {SERIES.map(s => (
            <button
              key={s.key}
              className={`live-run-toggle ${activeSeries.has(s.key) ? 'active' : ''}`}
              style={{ '--series-color': s.color } as React.CSSProperties}
              onClick={() => toggleSerie(s.key)}
            >
              {s.label}
            </button>
          ))}
        </div>
      </div>

      <ResponsiveContainer width="100%" height={260}>
        <LineChart data={normalizedPoints} margin={{ top: 8, right: 24, left: 0, bottom: 8 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#2a2a3a" />
          <XAxis
            dataKey="t"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={v => `${(v / 1000).toFixed(1)}s`}
            tick={{ fill: '#7878a0', fontSize: 11 }}
          />
          <YAxis tick={{ fill: '#7878a0', fontSize: 11 }} width={48} />
          <Tooltip
            contentStyle={{ background: '#1e1e2e', border: '1px solid #3a3a4a', borderRadius: 8 }}
            labelFormatter={v => `t = ${(Number(v) / 1000).toFixed(2)}s`}
            labelStyle={{ color: '#e0e0f0', marginBottom: 4 }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />

          {annotations.map((a, i) => (
            <ReferenceLine
              key={i}
              x={a.t}
              stroke={a.color}
              strokeDasharray="4 3"
              strokeOpacity={0.7}
              label={{ value: a.label, fill: a.color, fontSize: 10, angle: -90, position: 'insideTopRight' }}
            />
          ))}

          {SERIES.map(s =>
            activeSeries.has(s.key) ? (
              <Line
                key={s.key}
                type="monotone"
                dataKey={s.key}
                name={s.label}
                stroke={s.color}
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
            ) : null
          )}
        </LineChart>
      </ResponsiveContainer>

      {points.length === 0 && isRunning && (
        <p className="live-run-chart-waiting">En attente de données…</p>
      )}
    </div>
  );
}
