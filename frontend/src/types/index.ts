// ── Scenario metadata (from GET /api/scenarios) ──────────────────────────────

export interface ScenarioMetadata {
  id: string;
  name: string;
  description: string;
  baselineCode: string;
  optimizedCode: string;
  whyExplanation: string;
  impact: 'HIGH' | 'MEDIUM' | 'LOW';
}

// ── Metrics (from POST /api/test/run) ─────────────────────────────────────────

export interface MetricsSnapshot {
  heapUsedMb: number;
  gcPauseMs: number;
  gcCount: number;
  allocationRateMbPerSec: number;
  sqlQueryCount: number;
  elapsedMs: number;
  cpuTimeMs: number;
}

export interface MetricsDiff {
  heapUsedMbDelta: number;
  gcPauseMsDelta: number;
  gcCountDelta: number;
  allocationRateMbPerSecDelta: number;
  sqlQueryCountDelta: number;
  elapsedMsDelta: number;
  cpuTimeMsDelta: number;
}

export interface MicrometerMetrics {
  cacheHitRate:   number | null;
  cacheHitCount:  number | null;
  cacheMissCount: number | null;
  gcPauseMeanMs:  number | null;
  gcPauseMaxMs:   number | null;
  threadsLive:    number | null;
  threadsPeak:    number | null;
}

export interface ScenarioResult {
  scenarioId:          string;
  baseline:            MetricsSnapshot;
  optimized:           MetricsSnapshot;
  diff:                MetricsDiff;
  baselineMicrometer:  MicrometerMetrics | null;
  optimizedMicrometer: MicrometerMetrics | null;
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

export interface LiveMetrics {
  heapUsedMb:    number | null;
  heapMaxMb:     number | null;
  gcPauseMeanMs: number | null;
  gcPauseMaxMs:  number | null;
  threadsLive:   number | null;
  threadsPeak:   number | null;
}
