package com.sentori.spring_perf_lab.api;

import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    record ErrorResponse(String scenarioId, String message, Instant timestamp) {}

    @ExceptionHandler(ScenarioExecutionException.class)
    public ResponseEntity<ErrorResponse> handleScenarioException(ScenarioExecutionException ex) {
        log.error("Scenario execution failed: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(ex.getScenarioId(), ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(null, "An unexpected error occurred", Instant.now()));
    }
}

