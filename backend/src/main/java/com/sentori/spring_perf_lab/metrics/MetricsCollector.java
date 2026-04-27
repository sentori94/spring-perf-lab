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
        long threadId = Thread.currentThread().threadId();
        return new RunStart(
                currentHeapMb(),
                System.currentTimeMillis(),
                currentGcCount(),
                currentGcPauseMs(),
                threadMXBean.getThreadAllocatedBytes(threadId),
                threadMXBean.getThreadCpuTime(threadId)
        );
    }

    /**
     * Captures a snapshot of metrics at the end of a run, using deltas from {@link RunStart}.
     *
     * @param runStart      The state captured before the run
     * @param sqlQueryCount Number of SQL queries executed during the run
     */
    public MetricsSnapshot snapshot(RunStart runStart, long sqlQueryCount) {
        long threadId        = Thread.currentThread().threadId();
        long elapsedMs       = System.currentTimeMillis() - runStart.startTimeMs();
        double endHeapMb     = currentHeapMb();
        long gcCountDelta    = currentGcCount()   - runStart.startGcCount();
        long gcPauseMsDelta  = currentGcPauseMs() - runStart.startGcPauseMs();

        long endAllocatedBytes        = threadMXBean.getThreadAllocatedBytes(threadId);
        double allocatedMb            = Math.max(0, endAllocatedBytes - runStart.startAllocatedBytes()) / (1024.0 * 1024.0);
        double elapsedSec             = elapsedMs / 1000.0;
        double allocationRateMbPerSec = elapsedSec > 0 ? allocatedMb / elapsedSec : 0.0;

        long endCpuTimeNs  = threadMXBean.getThreadCpuTime(threadId);
        long cpuTimeMs     = Math.max(0, endCpuTimeNs - runStart.startCpuTimeNs()) / 1_000_000;

        return new MetricsSnapshot(
                endHeapMb,
                gcPauseMsDelta,
                gcCountDelta,
                allocationRateMbPerSec,
                sqlQueryCount,
                elapsedMs,
                cpuTimeMs
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
