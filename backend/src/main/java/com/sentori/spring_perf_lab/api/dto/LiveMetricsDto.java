package com.sentori.spring_perf_lab.api.dto;

/**
 * Snapshot des métriques JVM en temps réel exposé par GET /api/metrics/live.
 *
 * @param heapUsedMb     Heap utilisée en Mo
 * @param heapMaxMb      Heap maximale configurée en Mo
 * @param gcPauseMeanMs  Moyenne des pauses GC depuis le démarrage (ms), null si aucun GC
 * @param gcPauseMaxMs   Max des pauses GC depuis le démarrage (ms), null si aucun GC
 * @param threadsLive    Nombre de threads vivants actuellement
 * @param threadsPeak    Pic de threads depuis le démarrage
 */
public record LiveMetricsDto(
        Double heapUsedMb,
        Double heapMaxMb,
        Double gcPauseMeanMs,
        Double gcPauseMaxMs,
        Long   threadsLive,
        Long   threadsPeak
) {}

