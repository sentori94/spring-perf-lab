package com.sentori.spring_perf_lab.api.dto;

import java.util.List;

/**
 * Request body for POST /api/test/run.
 *
 * @param scenarioIds List of scenario IDs to run (e.g. ["n-plus-1"])
 * @param mode        "QUICK" (10 000 iterations in-process) or "LOAD" (Gatling)
 */
public record TestRunRequestDto(
        List<String> scenarioIds,
        String mode
) {}

