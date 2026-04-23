package com.sentori.spring_perf_lab.runner;

import com.sentori.spring_perf_lab.api.dto.ScenarioResultDto;
import com.sentori.spring_perf_lab.api.dto.TestRunRequestDto;
import com.sentori.spring_perf_lab.api.dto.TestRunResultDto;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the execution of one or more scenarios based on the requested mode.
 * Keeps all business logic out of the controller.
 */
@Service
public class TestRunnerService {

    private static final Logger log = LoggerFactory.getLogger(TestRunnerService.class);

    private final Map<String, PerfScenario> scenariosById;

    public TestRunnerService(List<PerfScenario> scenarios) {
        this.scenariosById = scenarios.stream()
                .collect(Collectors.toMap(PerfScenario::getId, Function.identity()));
    }

    public TestRunResultDto run(TestRunRequestDto request) {
        List<ScenarioResultDto> results = request.scenarioIds().stream()
                .map(id -> runOne(id, request.mode()))
                .toList();

        return new TestRunResultDto(request.mode(), results);
    }

    public List<PerfScenario> listAll() {
        return List.copyOf(scenariosById.values());
    }

    private ScenarioResultDto runOne(String scenarioId, String mode) {
        PerfScenario scenario = scenariosById.get(scenarioId);
        if (scenario == null) {
            throw new ScenarioExecutionException(scenarioId,
                    "Unknown scenario id '%s'".formatted(scenarioId));
        }

        log.info("Running scenario '{}' in {} mode", scenarioId, mode);

        MetricsSnapshot baseline  = scenario.runBaseline();
        MetricsSnapshot optimized = scenario.runOptimized();

        return new ScenarioResultDto(
                scenarioId,
                baseline,
                optimized,
                optimized.diffFrom(baseline)
        );
    }
}

