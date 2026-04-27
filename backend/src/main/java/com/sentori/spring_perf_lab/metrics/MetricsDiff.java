package com.sentori.spring_perf_lab.metrics;

/**
 * Represents the difference between an optimized and a baseline {@link MetricsSnapshot}.
 * Negative values indicate an improvement (less heap, less GC, fewer queries, faster).
 *
 * @param heapUsedMbDelta            Delta heap used (MB)
 * @param gcPauseMsDelta             Delta total GC pause time (ms)
 * @param gcCountDelta               Delta GC collection count
 * @param allocationRateMbPerSecDelta Delta allocation rate (MB/s)
 * @param sqlQueryCountDelta         Delta SQL query count
 * @param elapsedMsDelta             Delta wall-clock duration (ms)
 * @param cpuTimeMsDelta             Delta CPU time (ms)
 */
public record MetricsDiff(
        double heapUsedMbDelta,
        long gcPauseMsDelta,
        long gcCountDelta,
        double allocationRateMbPerSecDelta,
        long sqlQueryCountDelta,
        long elapsedMsDelta,
        long cpuTimeMsDelta
) {}
