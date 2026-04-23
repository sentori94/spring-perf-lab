import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import type { ScenarioResult } from '../types';

interface Props {
  result: ScenarioResult;
}

export default function MetricsChart({ result }: Props) {
  const data = [
    {
      metric: 'Elapsed (ms)',
      Baseline: result.baseline.elapsedMs,
      Optimized: result.optimized.elapsedMs,
    },
    {
      metric: 'Heap (MB)',
      Baseline: parseFloat(result.baseline.heapUsedMb.toFixed(1)),
      Optimized: parseFloat(result.optimized.heapUsedMb.toFixed(1)),
    },
    {
      metric: 'GC Pause (ms)',
      Baseline: result.baseline.gcPauseMs,
      Optimized: result.optimized.gcPauseMs,
    },
    {
      metric: 'SQL Queries',
      Baseline: result.baseline.sqlQueryCount,
      Optimized: result.optimized.sqlQueryCount,
    },
  ];

  return (
    <div className="metrics-chart">
      <ResponsiveContainer width="100%" height={260}>
        <BarChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 8 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#2a2a3a" />
          <XAxis dataKey="metric" tick={{ fill: '#a0a0b0', fontSize: 12 }} />
          <YAxis tick={{ fill: '#a0a0b0', fontSize: 12 }} />
          <Tooltip
            contentStyle={{ background: '#1e1e2e', border: '1px solid #3a3a4a', borderRadius: 8 }}
            labelStyle={{ color: '#e0e0f0' }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          <Bar dataKey="Baseline" fill="#6366f1" radius={[4, 4, 0, 0]} />
          <Bar dataKey="Optimized" fill="#22d3ee" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

