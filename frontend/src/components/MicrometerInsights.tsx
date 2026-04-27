import type { MicrometerMetrics } from '../types';

interface Props {
  baseline:  MicrometerMetrics | null;
  optimized: MicrometerMetrics | null;
}

interface Row {
  label: string;
  baseline:  string | null;
  optimized: string | null;
  highlight?: boolean;
}

function fmt(v: number | null | undefined, decimals = 1, suffix = ''): string | null {
  if (v === null || v === undefined) return null;
  return `${v.toFixed(decimals)}${suffix}`;
}

function buildRows(b: MicrometerMetrics | null, o: MicrometerMetrics | null): Row[] {
  const rows: Row[] = [];

  if (b?.cacheHitRate !== undefined || o?.cacheHitRate !== undefined) {
    rows.push({
      label:    'Cache hit rate',
      baseline:  fmt(b?.cacheHitRate != null ? b.cacheHitRate * 100 : null, 1, '%'),
      optimized: fmt(o?.cacheHitRate != null ? o.cacheHitRate * 100 : null, 1, '%'),
      highlight: (o?.cacheHitRate ?? 0) > 0.8,
    });
  }
  if (b?.cacheHitCount !== undefined || o?.cacheHitCount !== undefined) {
    rows.push({
      label:    'Cache hits',
      baseline:  fmt(b?.cacheHitCount, 0),
      optimized: fmt(o?.cacheHitCount, 0),
    });
  }
  if (b?.cacheMissCount !== undefined || o?.cacheMissCount !== undefined) {
    rows.push({
      label:    'Cache misses',
      baseline:  fmt(b?.cacheMissCount, 0),
      optimized: fmt(o?.cacheMissCount, 0),
    });
  }
  if (b?.gcPauseMeanMs !== undefined || o?.gcPauseMeanMs !== undefined) {
    rows.push({
      label:    'GC pause mean',
      baseline:  fmt(b?.gcPauseMeanMs, 2, ' ms'),
      optimized: fmt(o?.gcPauseMeanMs, 2, ' ms'),
    });
  }
  if (b?.gcPauseMaxMs !== undefined || o?.gcPauseMaxMs !== undefined) {
    rows.push({
      label:    'GC pause max',
      baseline:  fmt(b?.gcPauseMaxMs, 2, ' ms'),
      optimized: fmt(o?.gcPauseMaxMs, 2, ' ms'),
    });
  }
  if (b?.threadsLive !== undefined || o?.threadsLive !== undefined) {
    rows.push({
      label:    'Threads live',
      baseline:  fmt(b?.threadsLive, 0),
      optimized: fmt(o?.threadsLive, 0),
    });
  }
  if (b?.threadsPeak !== undefined || o?.threadsPeak !== undefined) {
    rows.push({
      label:    'Threads peak',
      baseline:  fmt(b?.threadsPeak, 0),
      optimized: fmt(o?.threadsPeak, 0),
    });
  }

  return rows.filter(r => r.baseline !== null || r.optimized !== null);
}

export default function MicrometerInsights({ baseline, optimized }: Props) {
  const rows = buildRows(baseline, optimized);
  if (rows.length === 0) return null;

  return (
    <div className="micrometer-insights">
      <h4>JVM Insights <span className="micrometer-tag">Micrometer</span></h4>
      <table className="micrometer-table">
        <thead>
          <tr>
            <th>Metric</th>
            <th>Baseline</th>
            <th>Optimized</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(row => (
            <tr key={row.label} className={row.highlight ? 'row-highlight' : ''}>
              <td>{row.label}</td>
              <td>{row.baseline ?? '—'}</td>
              <td className={row.highlight ? 'cell-good' : ''}>{row.optimized ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

