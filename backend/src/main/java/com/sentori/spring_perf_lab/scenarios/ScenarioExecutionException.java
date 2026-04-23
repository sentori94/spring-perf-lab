package com.sentori.spring_perf_lab.scenarios;

/**
 * Wraps any exception thrown during a scenario run (baseline or optimized).
 * All {@link PerfScenario} implementations must catch and rethrow as this exception.
 */
public class ScenarioExecutionException extends RuntimeException {

    private final String scenarioId;

    public ScenarioExecutionException(String scenarioId, String message, Throwable cause) {
        super("[%s] %s".formatted(scenarioId, message), cause);
        this.scenarioId = scenarioId;
    }

    public ScenarioExecutionException(String scenarioId, String message) {
        super("[%s] %s".formatted(scenarioId, message));
        this.scenarioId = scenarioId;
    }

    public String getScenarioId() {
        return scenarioId;
    }
}

