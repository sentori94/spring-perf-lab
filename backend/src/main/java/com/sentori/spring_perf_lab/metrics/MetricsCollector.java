package com.sentori.spring_perf_lab.metrics;

import com.sun.management.ThreadMXBean;
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
    private final ThreadMXBean threadMXBean;

    public MetricsCollector() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.threadMXBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
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
                currentGcPauseMs(),
                threadMXBean.getThreadAllocatedBytes(Thread.currentThread().threadId())
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

        // Allocation rate via ThreadMXBean.getThreadAllocatedBytes() — compteur monotone
        // cumulant tous les octets alloués par le thread appelant depuis le démarrage de la JVM.
        // Contrairement à endHeap - startHeap, cette valeur est GC-agnostic : elle ne diminue
        // jamais, même si le GC libère de la mémoire pendant le run.
        long endAllocatedBytes   = threadMXBean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        double allocatedMb       = Math.max(0, endAllocatedBytes - runStart.startAllocatedBytes()) / (1024.0 * 1024.0);
        double elapsedSec        = elapsedMs / 1000.0;
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
