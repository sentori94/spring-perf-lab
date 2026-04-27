package com.sentori.spring_perf_lab.metrics;

import com.sentori.spring_perf_lab.metrics.MetricsDiff;

/**
 * Immutable snapshot of performance metrics captured during a scenario run.
 *
 * @param heapUsedMb             Heap memory used at the end of the run (MB)
 * @param gcPauseMs              Total GC pause time during the run (ms)
 * @param gcCount                Number of GC collections during the run
 * @param allocationRateMbPerSec Estimated allocation rate (MB/s)
 * @param sqlQueryCount          Number of SQL queries executed during the run
 * @param elapsedMs              Wall-clock duration of the run (ms)
 * @param cpuTimeMs              CPU time consumed by the run thread (ms)
 */
public record MetricsSnapshot(
        double heapUsedMb,
        long gcPauseMs,
        long gcCount,
        double allocationRateMbPerSec,
        long sqlQueryCount,
        long elapsedMs,
        long cpuTimeMs
) {
    /**
     * Computes the diff between this snapshot (optimized) and a baseline snapshot.
     * Negative values mean the optimized version consumed less.
     */
    public MetricsDiff diffFrom(MetricsSnapshot baseline) {
        return new MetricsDiff(
                this.heapUsedMb - baseline.heapUsedMb,
                this.gcPauseMs - baseline.gcPauseMs,
                this.gcCount - baseline.gcCount,
                this.allocationRateMbPerSec - baseline.allocationRateMbPerSec,
                this.sqlQueryCount - baseline.sqlQueryCount,
                this.elapsedMs - baseline.elapsedMs,
                this.cpuTimeMs - baseline.cpuTimeMs
        );
    }
}
