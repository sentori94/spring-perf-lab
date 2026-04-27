import type { ScenarioResult, ScenarioMetadata } from '../types';
import MetricsChart from './MetricsChart';
import ScenarioDetail from './ScenarioDetail';
import MicrometerInsights from './MicrometerInsights';

interface Props {
  results: ScenarioResult[];
  mode: string;
  scenarios: ScenarioMetadata[];
}

function DeltaBadge({ value, unit = '' }: { value: number; unit?: string }) {
  const improved = value < 0;
  const neutral = value === 0;
  const label = `${improved ? '' : '+'}${value.toFixed(value % 1 === 0 ? 0 : 1)}${unit}`;
  const cls = neutral ? 'badge neutral' : improved ? 'badge good' : 'badge bad';
  return <span className={cls}>{label}</span>;
}

function MetricsRow({ label, baseline, optimized, delta, unit = '' }: {
  label: string;
  baseline: number;
  optimized: number;
  delta: number;
  unit?: string;
}) {
  return (
    <tr>
      <td>{label}</td>
      <td>{baseline.toFixed(baseline % 1 === 0 ? 0 : 1)}{unit}</td>
      <td>{optimized.toFixed(optimized % 1 === 0 ? 0 : 1)}{unit}</td>
      <td><DeltaBadge value={delta} unit={unit} /></td>
    </tr>
  );
}

export default function ResultsDashboard({ results, mode, scenarios }: Props) {
  if (results.length === 0) return null;

  return (
    <section className="dashboard">
      <h2>Results <span className="mode-tag">{mode}</span></h2>

      {results.map(r => {
        const scenario = scenarios.find(s => s.id === r.scenarioId);
        return (
          <div key={r.scenarioId} className="scenario-result">
            <h3>{scenario?.name ?? r.scenarioId}</h3>

            <table className="metrics-table">
              <thead>
                <tr>
                  <th>Metric</th>
                  <th>Baseline</th>
                  <th>Optimized</th>
                  <th>Delta</th>
                </tr>
              </thead>
              <tbody>
                <MetricsRow label="Elapsed"     baseline={r.baseline.elapsedMs}               optimized={r.optimized.elapsedMs}               delta={r.diff.elapsedMsDelta}               unit=" ms"   />
                <MetricsRow label="CPU time"    baseline={r.baseline.cpuTimeMs}               optimized={r.optimized.cpuTimeMs}               delta={r.diff.cpuTimeMsDelta}               unit=" ms"   />
                <MetricsRow label="Heap used"   baseline={r.baseline.heapUsedMb}              optimized={r.optimized.heapUsedMb}              delta={r.diff.heapUsedMbDelta}              unit=" MB"   />
                <MetricsRow label="GC pause"    baseline={r.baseline.gcPauseMs}               optimized={r.optimized.gcPauseMs}               delta={r.diff.gcPauseMsDelta}               unit=" ms"   />
                <MetricsRow label="GC count"    baseline={r.baseline.gcCount}                 optimized={r.optimized.gcCount}                 delta={r.diff.gcCountDelta}                             />
                <MetricsRow label="SQL queries" baseline={r.baseline.sqlQueryCount}           optimized={r.optimized.sqlQueryCount}           delta={r.diff.sqlQueryCountDelta}                       />
                <MetricsRow label="Alloc rate"  baseline={r.baseline.allocationRateMbPerSec}  optimized={r.optimized.allocationRateMbPerSec}  delta={r.diff.allocationRateMbPerSecDelta}  unit=" MB/s" />
              </tbody>
            </table>

            <MetricsChart result={r} />

            {(r.baselineMicrometer || r.optimizedMicrometer) && (
              <MicrometerInsights
                baseline={r.baselineMicrometer}
                optimized={r.optimizedMicrometer}
              />
            )}

            {scenario && (
              <div className="result-detail">
                <h4>Code explanation</h4>
                <ScenarioDetail scenario={scenario} />
              </div>
            )}
          </div>
        );
      })}
    </section>
  );
}
