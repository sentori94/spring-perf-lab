package com.sentori.spring_perf_lab.api;

import com.sentori.spring_perf_lab.api.dto.LiveMetricsDto;
import com.sentori.spring_perf_lab.api.dto.ScenarioMetadataDto;
import com.sentori.spring_perf_lab.api.dto.TestRunRequestDto;
import com.sentori.spring_perf_lab.api.dto.TestRunResultDto;
import com.sentori.spring_perf_lab.metrics.MicrometerCollector;
import com.sentori.spring_perf_lab.runner.TestRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PerfLabController {

    private final TestRunnerService testRunnerService;
    private final MicrometerCollector micrometerCollector;

    public PerfLabController(TestRunnerService testRunnerService,
                             MicrometerCollector micrometerCollector) {
        this.testRunnerService   = testRunnerService;
        this.micrometerCollector = micrometerCollector;
    }

    @GetMapping("/scenarios")
    public ResponseEntity<List<ScenarioMetadataDto>> getScenarios() {
        List<ScenarioMetadataDto> metadata = testRunnerService.listAll().stream()
                .map(s -> new ScenarioMetadataDto(
                        s.getId(), s.getName(), s.getDescription(),
                        s.getBaselineCode(), s.getOptimizedCode(),
                        s.getWhyExplanation(), s.getImpact()
                ))
                .toList();
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/test/run")
    public ResponseEntity<TestRunResultDto> runTest(@RequestBody TestRunRequestDto request) {
        return ResponseEntity.ok(testRunnerService.run(request));
    }

    /** Retourne un snapshot des métriques JVM courantes via Micrometer. */
    @GetMapping("/metrics/live")
    public ResponseEntity<LiveMetricsDto> getLiveMetrics() {
        return ResponseEntity.ok(micrometerCollector.collectLive());
    }
}
