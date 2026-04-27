import { useLiveMetrics } from '../hooks/useLiveMetrics';

function fmt(v: number | null | undefined, decimals = 1, unit = ''): string {
  return v != null ? `${v.toFixed(decimals)}${unit}` : '—';
}

export default function LiveMetricsPanel() {
  const { metrics, error } = useLiveMetrics();

  const heapPct = metrics?.heapUsedMb != null && metrics?.heapMaxMb
    ? Math.round((metrics.heapUsedMb / metrics.heapMaxMb) * 100)
    : null;

  return (
    <div className="live-panel">
      <div className="live-panel-header">
        <span className="live-dot" />
        <span>JVM Live</span>
        <span className="micrometer-tag">Micrometer</span>
      </div>

      {error && <p className="live-error">Backend unreachable</p>}

      {!error && (
        <div className="live-rows">

          {/* ── Heap ── */}
          <div className="live-section-label">Heap</div>
          <div className="live-row">
            <span className="live-row-label">Used</span>
            <span className="live-row-value">{fmt(metrics?.heapUsedMb, 1, ' MB')}</span>
          </div>
          {heapPct != null && (
            <div className="live-heap-bar">
              <div
                className={`live-heap-fill ${heapPct > 80 ? 'live-heap-warn' : ''}`}
                style={{ width: `${heapPct}%` }}
              />
              <span className="live-heap-pct">{heapPct}%</span>
            </div>
          )}

          {/* ── GC ── */}
          <div className="live-section-label">GC Pauses</div>
          <div className="live-row">
            <span className="live-row-label">Mean</span>
            <span className="live-row-value">{fmt(metrics?.gcPauseMeanMs, 2, ' ms')}</span>
          </div>
          <div className="live-row">
            <span className="live-row-label">Max</span>
            <span className="live-row-value">{fmt(metrics?.gcPauseMaxMs, 2, ' ms')}</span>
          </div>

          {/* ── Threads (secondaire) ── */}
          <div className="live-section-label">Threads</div>
          <div className="live-row">
            <span className="live-row-label">Live</span>
            <span className="live-row-value live-row-value--muted">{fmt(metrics?.threadsLive, 0)}</span>
          </div>
          <div className="live-row">
            <span className="live-row-label">Peak</span>
            <span className="live-row-value live-row-value--muted">{fmt(metrics?.threadsPeak, 0)}</span>
          </div>

        </div>
      )}
    </div>
  );
}
