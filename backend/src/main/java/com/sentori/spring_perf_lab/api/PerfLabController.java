package com.sentori.spring_perf_lab.api;

import com.sentori.spring_perf_lab.api.dto.ScenarioMetadataDto;
import com.sentori.spring_perf_lab.api.dto.TestRunRequestDto;
import com.sentori.spring_perf_lab.api.dto.TestRunResultDto;
import com.sentori.spring_perf_lab.runner.TestRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PerfLabController {

    private final TestRunnerService testRunnerService;

    public PerfLabController(TestRunnerService testRunnerService) {
        this.testRunnerService = testRunnerService;
    }

    @GetMapping("/scenarios")
    public ResponseEntity<List<ScenarioMetadataDto>> getScenarios() {
        List<ScenarioMetadataDto> metadata = testRunnerService.listAll().stream()
                .map(s -> new ScenarioMetadataDto(
                        s.getId(),
                        s.getName(),
                        s.getDescription(),
                        s.getBaselineCode(),
                        s.getOptimizedCode(),
                        s.getWhyExplanation(),
                        s.getImpact()
                ))
                .toList();
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/test/run")
    public ResponseEntity<TestRunResultDto> runTest(@RequestBody TestRunRequestDto request) {
        return ResponseEntity.ok(testRunnerService.run(request));
    }
}
