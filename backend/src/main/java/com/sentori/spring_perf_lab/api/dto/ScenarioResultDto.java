package com.sentori.spring_perf_lab.api.dto;

import com.sentori.spring_perf_lab.metrics.MetricsDiff;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.MicrometerMetrics;

/**
 * Result for a single scenario run, returned inside TestRunResultDto.
 *
 * @param scenarioId Scenario identifier
 * @param baseline   Metrics captured during the baseline run
 * @param optimized  Metrics captured during the optimized run
 * @param diff       Delta between optimized and baseline (negative = improvement)
 */
public record ScenarioResultDto(
        String scenarioId,
        MetricsSnapshot baseline,
        MetricsSnapshot optimized,
        MetricsDiff diff,
        MicrometerMetrics baselineMicrometer,
        MicrometerMetrics optimizedMicrometer
) {}
