package com.sentori.spring_perf_lab.metrics;

/**
 * Captures the JVM state at the start of a scenario run.
 * Used in conjunction with {@link MetricsCollector#snapshot} to compute deltas.
 *
 * @param startHeapMb        Heap used at the start of the run (MB)
 * @param startTimeMs        Wall-clock time at the start of the run (ms)
 * @param startGcCount       Total GC collection count at the start
 * @param startGcPauseMs     Total GC pause time at the start (ms)
 * @param startAllocatedBytes Bytes allocated by the calling thread since JVM start
 *                            (via ThreadMXBean — monotonically increasing, GC-agnostic)
 */
public record RunStart(
        double startHeapMb,
        long startTimeMs,
        long startGcCount,
        long startGcPauseMs,
        long startAllocatedBytes
) {}

