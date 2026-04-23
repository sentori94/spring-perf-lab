package com.sentori.spring_perf_lab.metrics;

import org.springframework.stereotype.Component;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

/**
 * Captures JVM performance metrics during a scenario run.
 *
 * Usage pattern:
 * <pre>
 *   RunStart start = metricsCollector.start();   // before the code to measure
 *   // ... code ...
 *   MetricsSnapshot snap = metricsCollector.snapshot(start, sqlQueryCount);
 * </pre>
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
     * Captures the JVM state before a run starts.
     * Call this immediately before the code you want to measure.
     */
    public RunStart start() {
        return new RunStart(
                currentHeapMb(),
                System.currentTimeMillis(),
                currentGcCount(),
                currentGcPauseMs()
        );
    }

    /**
     * Captures a snapshot of metrics at the end of a run, using deltas from {@link RunStart}.
     *
     * @param runStart      The state captured before the run
     * @param sqlQueryCount Number of SQL queries executed during the run
     */
    public MetricsSnapshot snapshot(RunStart runStart, long sqlQueryCount) {
        long elapsedMs       = System.currentTimeMillis() - runStart.startTimeMs();
        double endHeapMb     = currentHeapMb();
        long gcCountDelta    = currentGcCount()    - runStart.startGcCount();
        long gcPauseMsDelta  = currentGcPauseMs()  - runStart.startGcPauseMs();

        // Allocation rate = heap allouée pendant le run / durée en secondes
        // On utilise max(0) car le GC peut avoir libéré de la mémoire entre les deux mesures
        double allocatedMb   = Math.max(0, endHeapMb - runStart.startHeapMb());
        double elapsedSec    = elapsedMs / 1000.0;
        double allocationRateMbPerSec = elapsedSec > 0 ? allocatedMb / elapsedSec : 0.0;

        return new MetricsSnapshot(
                endHeapMb,
                gcPauseMsDelta,
                gcCountDelta,
                allocationRateMbPerSec,
                sqlQueryCount,
                elapsedMs
        );
    }

    private double currentHeapMb() {
        return memoryMXBean.getHeapMemoryUsage().getUsed() / (1024.0 * 1024.0);
    }

    public long currentGcCount() {
        return gcBeans.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(c -> c >= 0)
                .sum();
    }

    public long currentGcPauseMs() {
        return gcBeans.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .filter(t -> t >= 0)
                .sum();
    }
}
