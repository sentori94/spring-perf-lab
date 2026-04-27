package com.sentori.spring_perf_lab.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MicrometerCollector {

    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;

    public MicrometerCollector(MeterRegistry meterRegistry, CacheManager cacheManager) {
        this.meterRegistry = meterRegistry;
        this.cacheManager  = cacheManager;
    }

    /**
     * Mirrors MetricsCollector.start() — call this just before a scenario run.
     * Records a snapshot of Caffeine stats so we can compute a delta after the run.
     */
    public record CaptureStart(
            com.github.benmanes.caffeine.cache.stats.CacheStats cacheStatsBefore,
            String cacheName
    ) {}

    /** Use for scenarios that involve the cache (e.g. caffeine-cache). */
    public CaptureStart startForCache(String cacheName) {
        return new CaptureStart(snapshotCacheStats(cacheName), cacheName);
    }

    /** Use for scenarios that don't use a cache. */
    public CaptureStart startJvmOnly() {
        return new CaptureStart(null, null);
    }

    /**
     * Call this just after a scenario run.
     * Computes cache-stats deltas from the CaptureStart and adds JVM metrics.
     */
    public MicrometerMetrics collect(CaptureStart capture) {
        Double hitRate = null;
        Long   hits    = null;
        Long   misses  = null;

        if (capture.cacheName() != null && capture.cacheStatsBefore() != null) {
            var current = snapshotCacheStats(capture.cacheName());
            if (current != null) {
                // CacheStats.minus() gives the delta since the snapshot — GC-agnostic
                var delta = current.minus(capture.cacheStatsBefore());
                hits   = delta.hitCount();
                misses = delta.missCount();
                long total = hits + misses;
                hitRate = total > 0 ? (double) hits / total : 0.0;
            }
        }

        return new MicrometerMetrics(
                hitRate, hits, misses,
                getGcPauseMean(),
                getGcPauseMax(),
                getThreadsLive(),
                getThreadsPeak()
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private com.github.benmanes.caffeine.cache.stats.CacheStats snapshotCacheStats(String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                return caffeineCache.getNativeCache().stats();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Double getGcPauseMean() {
        try {
            var timer = meterRegistry.find("jvm.gc.pause").timer();
            if (timer == null || timer.count() == 0) return null;
            return timer.mean(TimeUnit.MILLISECONDS);
        } catch (Exception e) { return null; }
    }

    private Double getGcPauseMax() {
        try {
            var timer = meterRegistry.find("jvm.gc.pause").timer();
            if (timer == null || timer.count() == 0) return null;
            return timer.max(TimeUnit.MILLISECONDS);
        } catch (Exception e) { return null; }
    }

    private Long getThreadsLive() {
        try {
            var gauge = meterRegistry.find("jvm.threads.live").gauge();
            return gauge != null ? (long) gauge.value() : null;
        } catch (Exception e) { return null; }
    }

    private Long getThreadsPeak() {
        try {
            var gauge = meterRegistry.find("jvm.threads.peak").gauge();
            return gauge != null ? (long) gauge.value() : null;
        } catch (Exception e) { return null; }
    }

    private Double getHeapUsedMb() {
        try {
            var gauge = meterRegistry.find("jvm.memory.used").tag("area", "heap").gauge();
            return gauge != null ? gauge.value() / (1024.0 * 1024.0) : null;
        } catch (Exception e) { return null; }
    }

    private Double getHeapMaxMb() {
        try {
            var gauge = meterRegistry.find("jvm.memory.max").tag("area", "heap").gauge();
            return gauge != null ? gauge.value() / (1024.0 * 1024.0) : null;
        } catch (Exception e) { return null; }
    }

    /** Snapshot instantané pour le panel live — heap + GC + threads. */
    public com.sentori.spring_perf_lab.api.dto.LiveMetricsDto collectLive() {
        return new com.sentori.spring_perf_lab.api.dto.LiveMetricsDto(
                getHeapUsedMb(),
                getHeapMaxMb(),
                getGcPauseMean(),
                getGcPauseMax(),
                getThreadsLive(),
                getThreadsPeak()
        );
    }
}
