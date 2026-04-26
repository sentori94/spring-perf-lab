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
import java.util.function.Supplier;
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

    private static final long LOAD_TARGET_MS = 5_000;

    private ScenarioResultDto runOne(String scenarioId, String mode) {
        PerfScenario scenario = scenariosById.get(scenarioId);
        if (scenario == null) {
            throw new ScenarioExecutionException(scenarioId,
                    "Unknown scenario id '%s'".formatted(scenarioId));
        }

        log.info("Running scenario '{}' in {} mode", scenarioId, mode);

        if ("LOAD".equalsIgnoreCase(mode)) {
            return runLoad(scenarioId, scenario);
        }

        MetricsSnapshot baseline  = scenario.runBaseline();
        MetricsSnapshot optimized = scenario.runOptimized();

        return new ScenarioResultDto(
                scenarioId,
                baseline,
                optimized,
                optimized.diffFrom(baseline)
        );
    }

    /**
     * LOAD mode: runs baseline freely for ~5 s and counts the real iteration count N,
     * then runs optimized for exactly N iterations.
     *
     * Baseline fills 5 s of real work → N iterations, elapsed measured via nanoTime.
     * Optimized does the same N iterations → elapsed reflects the true speedup.
     * Using System.nanoTime() ensures sub-millisecond runs are timed accurately
     * (System.currentTimeMillis() rounds to ~1 ms and returns 0 for fast runs).
     */
    private ScenarioResultDto runLoad(String scenarioId, PerfScenario scenario) {
        long deadline = System.currentTimeMillis() + LOAD_TARGET_MS;
        long tStart = System.nanoTime();
        long gcPauseSum = 0, gcCountSum = 0, sqlSum = 0;
        double lastHeap = 0, lastAllocRate = 0;
        int n = 0;

        do {
            MetricsSnapshot s = scenario.runBaseline();
            gcPauseSum   += s.gcPauseMs();
            gcCountSum   += s.gcCount();
            sqlSum       += s.sqlQueryCount();
            lastHeap      = s.heapUsedMb();
            lastAllocRate = s.allocationRateMbPerSec();
            n++;
        } while (System.currentTimeMillis() < deadline);

        long baselineElapsedMs = (System.nanoTime() - tStart) / 1_000_000;
        MetricsSnapshot baseline = new MetricsSnapshot(lastHeap, gcPauseSum, gcCountSum, lastAllocRate, sqlSum, baselineElapsedMs);

        log.info("LOAD baseline for '{}': {} real iterations in {}ms", scenarioId, n, baselineElapsedMs);

        MetricsSnapshot optimized = runNTimes(scenario::runOptimized, n);

        return new ScenarioResultDto(
                scenarioId,
                baseline,
                optimized,
                optimized.diffFrom(baseline)
        );
    }

    /**
     * Runs {@code runner} exactly {@code n} times and returns an aggregated snapshot.
     * Uses System.nanoTime() for accurate total elapsed time regardless of per-run duration.
     */
    private MetricsSnapshot runNTimes(Supplier<MetricsSnapshot> runner, int n) {
        long tStart = System.nanoTime();
        long gcPauseSum = 0, gcCountSum = 0, sqlSum = 0;
        double lastHeap = 0, lastAllocRate = 0;
        for (int i = 0; i < n; i++) {
            MetricsSnapshot s = runner.get();
            gcPauseSum   += s.gcPauseMs();
            gcCountSum   += s.gcCount();
            sqlSum       += s.sqlQueryCount();
            lastHeap      = s.heapUsedMb();
            lastAllocRate = s.allocationRateMbPerSec();
        }
        long elapsedMs = (System.nanoTime() - tStart) / 1_000_000;
        return new MetricsSnapshot(lastHeap, gcPauseSum, gcCountSum, lastAllocRate, sqlSum, elapsedMs);
    }
}

