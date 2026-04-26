package com.sentori.spring_perf_lab.api.dto;

/**
 * Metadata describing a scenario, returned by GET /api/scenarios.
 *
 * @param id          Unique identifier (e.g. "n-plus-1")
 * @param name        Human-readable name (e.g. "N+1 Query Fix")
 * @param description Short description of what the scenario demonstrates
 * @param impact      Expected impact level: HIGH, MEDIUM, or LOW
 */
public record ScenarioMetadataDto(
        String id,
        String name,
        String description,
        String baselineCode,
        String optimizedCode,
        String whyExplanation,
        String impact
) {}
