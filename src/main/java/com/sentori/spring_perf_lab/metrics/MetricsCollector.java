package com.sentori.spring_perf_lab.metrics;

import org.springframework.stereotype.Component;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

/**
 * Captures JVM performance metrics at a given point in time.
 * Use {@link #snapshot(long, long, long)} before and after a scenario run,
 * passing the elapsed time and SQL query count measured externally.
 */
@Component
public class MetricsCollector {

    private final MemoryMXBean memoryMXBean;
    private final List<GarbageCollectorMXBean> gcBeans;

    public MetricsCollector() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    /**
     * Captures a snapshot of current JVM metrics.
     *
     * @param elapsedMs      Wall-clock duration of the run in milliseconds
     * @param sqlQueryCount  Number of SQL queries executed during the run
     * @param durationMs     Duration used to compute allocation rate (same as elapsedMs in most cases)
     * @return a fully populated {@link MetricsSnapshot}
     */
    public MetricsSnapshot snapshot(long elapsedMs, long sqlQueryCount, long durationMs) {
        long heapUsedBytes = memoryMXBean.getHeapMemoryUsage().getUsed();
        double heapUsedMb = heapUsedBytes / (1024.0 * 1024.0);

        long totalGcPauseMs = 0;
        long totalGcCount = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long time = gc.getCollectionTime();
            long count = gc.getCollectionCount();
            if (time >= 0) totalGcPauseMs += time;
            if (count >= 0) totalGcCount += count;
        }

        // Allocation rate: heap used / elapsed seconds
        double allocationRateMbPerSec = durationMs > 0
                ? heapUsedMb / (durationMs / 1000.0)
                : 0.0;

        return new MetricsSnapshot(
                heapUsedMb,
                totalGcPauseMs,
                totalGcCount,
                allocationRateMbPerSec,
                sqlQueryCount,
                elapsedMs
        );
    }

    /**
     * Returns the current total GC collection count across all collectors.
     * Useful to compute a delta before/after a run.
     */
    public long currentGcCount() {
        return gcBeans.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(c -> c >= 0)
                .sum();
    }

    /**
     * Returns the current total GC pause time (ms) across all collectors.
     * Useful to compute a delta before/after a run.
     */
    public long currentGcPauseMs() {
        return gcBeans.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .filter(t -> t >= 0)
                .sum();
    }
}

