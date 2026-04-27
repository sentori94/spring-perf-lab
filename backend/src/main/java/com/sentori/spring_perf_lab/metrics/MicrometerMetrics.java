package com.sentori.spring_perf_lab.metrics;

/**
 * Micrometer-sourced JVM and application metrics captured after a scenario run.
 * Fields are nullable — not all metrics are relevant for every scenario.
 *
 * @param cacheHitRate      Caffeine cache hit rate (0.0–1.0), null if not applicable
 * @param cacheHitCount     Total cache hits, null if not applicable
 * @param cacheMissCount    Total cache misses, null if not applicable
 * @param gcPauseMeanMs     Mean GC pause duration (ms), null if no GC occurred
 * @param gcPauseMaxMs      Max GC pause duration (ms), null if no GC occurred
 * @param threadsLive       Number of live threads at the end of the run
 * @param threadsPeak       Peak thread count since JVM start
 */
public record MicrometerMetrics(
        Double cacheHitRate,
        Long cacheHitCount,
        Long cacheMissCount,
        Double gcPauseMeanMs,
        Double gcPauseMaxMs,
        Long threadsLive,
        Long threadsPeak
) {}

