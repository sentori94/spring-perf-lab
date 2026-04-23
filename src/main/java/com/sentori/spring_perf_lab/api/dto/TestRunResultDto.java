package com.sentori.spring_perf_lab.api.dto;

import java.util.List;

/**
 * Response body for POST /api/test/run.
 *
 * @param mode    The mode that was used ("QUICK" or "LOAD")
 * @param results One result per scenario that was run
 */
public record TestRunResultDto(
        String mode,
        List<ScenarioResultDto> results
) {}

