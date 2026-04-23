import type { ScenarioMetadata, TestMode } from '../types';

interface Props {
  scenarios: ScenarioMetadata[];
  selectedIds: string[];
  focusedId: string | null;
  mode: TestMode;
  loading: boolean;
  onToggle: (id: string) => void;
  onFocus: (id: string) => void;
  onModeChange: (mode: TestMode) => void;
  onRun: () => void;
}

export default function ScenarioChecklist({
  scenarios,
  selectedIds,
  focusedId,
  mode,
  loading,
  onToggle,
  onFocus,
  onModeChange,
  onRun,
}: Props) {
  return (
    <aside className="checklist">
      <h2>Optimizations</h2>

      <ul>
        {scenarios.map(s => (
          <li key={s.id}>
            <label className={[
              selectedIds.includes(s.id) ? 'selected' : '',
              focusedId === s.id ? 'focused' : '',
            ].join(' ')}>
              <input
                type="checkbox"
                checked={selectedIds.includes(s.id)}
                onChange={() => onToggle(s.id)}
              />
              <span className="scenario-name" onClick={() => onFocus(s.id)}>
                {s.name}
              </span>
              <span className="scenario-desc">{s.description}</span>
            </label>
          </li>
        ))}
      </ul>

      <div className="mode-selector">
        <h3>Mode</h3>
        {(['QUICK', 'LOAD'] as TestMode[]).map(m => (
          <label key={m}>
            <input
              type="radio"
              name="mode"
              value={m}
              checked={mode === m}
              onChange={() => onModeChange(m)}
            />
            {m === 'QUICK' ? 'Quick (~2s)' : 'Load test (Gatling)'}
          </label>
        ))}
      </div>

      <button
        className="run-btn"
        onClick={onRun}
        disabled={selectedIds.length === 0 || loading}
      >
        {loading ? 'Running…' : 'Run benchmark'}
      </button>
    </aside>
  );
}
