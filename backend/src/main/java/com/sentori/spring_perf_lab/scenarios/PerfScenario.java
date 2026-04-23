package com.sentori.spring_perf_lab.scenarios;

import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;

/**
 * Contract that every optimization scenario must implement.
 * Each scenario provides a baseline (unoptimized) and an optimized run,
 * both returning a {@link MetricsSnapshot} for comparison.
 */
public interface PerfScenario {

    /**
     * Unique identifier for this scenario (e.g. "n-plus-1").
     */
    String getId();

    /**
     * Human-readable name (e.g. "N+1 Query Fix").
     */
    String getName();

    /**
     * Short description of what this scenario demonstrates.
     */
    String getDescription();

    // ── Code explanation ──────────────────────────────────────────────────────

    /**
     * Returns the code for the baseline (unoptimized) version as a string.
     */
    String getBaselineCode();

    /**
     * Returns the code for the optimized version as a string.
     */
    String getOptimizedCode();

    /**
     * Provides an explanation of why the optimized version is expected to perform better.
     */
    String getWhyExplanation();

    // ── Runs ──────────────────────────────────────────────────────────────────

    /**
     * Runs the intentionally unoptimized implementation and returns its metrics.
     */
    MetricsSnapshot runBaseline();

    /**
     * Runs the optimized implementation and returns its metrics.
     */
    MetricsSnapshot runOptimized();
}
