// ── Scenario metadata (from GET /api/scenarios) ──────────────────────────────

export interface ScenarioMetadata {
  id: string;
  name: string;
  description: string;
  baselineCode: string;
  optimizedCode: string;
  whyExplanation: string;
}

// ── Metrics (from POST /api/test/run) ─────────────────────────────────────────

export interface MetricsSnapshot {
  heapUsedMb: number;
  gcPauseMs: number;
  gcCount: number;
  allocationRateMbPerSec: number;
  sqlQueryCount: number;
  elapsedMs: number;
}

export interface MetricsDiff {
  heapUsedMbDelta: number;
  gcPauseMsDelta: number;
  gcCountDelta: number;
  allocationRateMbPerSecDelta: number;
  sqlQueryCountDelta: number;
  elapsedMsDelta: number;
}

export interface ScenarioResult {
  scenarioId: string;
  baseline: MetricsSnapshot;
  optimized: MetricsSnapshot;
  diff: MetricsDiff;
}

export interface TestRunResult {
  mode: string;
  results: ScenarioResult[];
}

// ── Request ───────────────────────────────────────────────────────────────────

export type TestMode = 'QUICK' | 'LOAD';

export interface TestRunRequest {
  scenarioIds: string[];
  mode: TestMode;
}
