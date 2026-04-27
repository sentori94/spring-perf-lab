import { useState, useEffect } from 'react';
import type { ScenarioMetadata, ScenarioResult, TestMode } from './types';
import { perfLabApiService } from './services/perfLabApiService';
import ScenarioChecklist from './components/ScenarioChecklist';
import ResultsDashboard from './components/ResultsDashboard';
import ScenarioDetail from './components/ScenarioDetail';
import LiveMetricsPanel from './components/LiveMetricsPanel';
import InfoPage from './components/InfoPage';

export default function App() {
  const [scenarios, setScenarios]     = useState<ScenarioMetadata[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [focusedId, setFocusedId]     = useState<string | null>(null);
  const [mode, setMode]               = useState<TestMode>('QUICK');
  const [results, setResults]         = useState<ScenarioResult[]>([]);
  const [resultMode, setResultMode]   = useState<string>('');
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState<string | null>(null);
  const [showInfo, setShowInfo]       = useState(false);

  useEffect(() => {
    perfLabApiService.getScenarios()
      .then(setScenarios)
      .catch(() => setError('Could not reach the backend. Is it running on port 8080?'));
  }, []);

  function toggleScenario(id: string) {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(s => s !== id) : [...prev, id]
    );
  }

  async function handleRun() {
    setLoading(true);
    setError(null);
    try {
      const data = await perfLabApiService.runTest({ scenarioIds: selectedIds, mode });
      setResults(data.results);
      setResultMode(data.mode);
      setFocusedId(null);
    } catch {
      setError('Benchmark failed. Check the backend logs.');
    } finally {
      setLoading(false);
    }
  }

  const focusedScenario = scenarios.find(s => s.id === focusedId) ?? null;

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header-left">
          <h1>⚡ spring-perf-lab</h1>
          <p>Select optimizations, run a benchmark, and see the before/after metrics.</p>
        </div>
        <button
          className={`info-btn ${showInfo ? 'info-btn--active' : ''}`}
          onClick={() => setShowInfo(v => !v)}
          title="About & Metrics glossary"
        >
          {showInfo ? '✕ Close' : '? About'}
        </button>
      </header>

      <div className="app-body">
        {error && <div className="error-banner">{error}</div>}

        {showInfo ? (
          <InfoPage />
        ) : (
          <>
            <div className="sidebar">
              <ScenarioChecklist
                scenarios={scenarios}
                selectedIds={selectedIds}
                focusedId={focusedId}
                mode={mode}
                loading={loading}
                onToggle={toggleScenario}
                onFocus={id => setFocusedId(prev => prev === id ? null : id)}
                onModeChange={setMode}
                onRun={handleRun}
              />
              <LiveMetricsPanel />
            </div>

            <main className="main-content">
              {/* Panneau Before/After/Why — affiché quand on clique sur un scénario */}
              {focusedScenario && results.length === 0 && (
                <div className="detail-panel">
                  <h2>{focusedScenario.name}</h2>
                  <ScenarioDetail scenario={focusedScenario} />
                </div>
              )}

              {results.length === 0 && !loading && !focusedScenario && (
                <div className="empty-state">
                  <p>Select a scenario to see the code explanation, then click <strong>Run benchmark</strong>.</p>
                </div>
              )}

              {loading && <div className="loading-state">Running benchmark…</div>}

              <ResultsDashboard
                results={results}
                mode={resultMode}
                scenarios={scenarios}
              />
            </main>
          </>
        )}
      </div>
    </div>
  );
}
