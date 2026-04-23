# Copilot Instructions — spring-perf-lab

## Project overview

`spring-perf-lab` is a Spring Boot benchmarking tool that demonstrates the measurable impact of Java and Spring performance optimizations. Each optimization has a **baseline** (intentionally unoptimized) and an **optimized** implementation. A test runner measures both and produces a structured metrics diff.

The frontend (Angular) lets users select optimizations, trigger tests, and see before/after results with code explanations.

---

## Stack

- **Backend**: Java 21, Spring Boot 4.x, Micrometer, Gatling, HikariCP, Caffeine, PostgreSQL
- **Frontend**: Angular 17+, RxJS, ngx-charts
- **Infra**: Docker Compose, AWS EC2 t3.micro, Terraform, GitHub Actions

---

## Backend conventions

### Scenario pattern

Every optimization scenario implements `PerfScenario`:

```java
@Component
public class NPlus1Scenario implements PerfScenario {

    @Override
    public String getId() { return "n-plus-1"; }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: lazy loading triggers N+1 queries
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: JOIN FETCH eliminates N+1
    }
}
```

- One class per optimization in `scenarios/`
- Always comment `// BASELINE:` or `// OPTIMIZED:` at the top of each method
- No shared mutable state between scenarios
- Wrap all exceptions in `ScenarioExecutionException`

### Metrics

Use `MetricsCollector.snapshot()` before and after each run. `MetricsSnapshot` fields: `heapUsedMb`, `gcPauseMs`, `gcCount`, `allocationRateMbPerSec`, `sqlQueryCount`, `elapsedMs`.

### REST API

- `POST /api/test/run` → `{ scenarioIds, mode: "QUICK"|"LOAD" }`
- `GET /api/scenarios` → scenario metadata list
- Controllers must be thin — no business logic
- Error handling via `@RestControllerAdvice` only
- Always `application/json`, never plain text

### Code style

- Constructor injection only — never `@Autowired` on fields
- SLF4J for logging — never `System.out.println`
- No hardcoded metric thresholds — values come from test results

---

## Frontend conventions

- Standalone Angular components throughout — no NgModules
- All HTTP calls go through `PerfLabApiService` only
- State via RxJS `BehaviorSubject` — no NgRx
- Impact levels: `HIGH` = green, `MEDIUM` = amber, `LOW` = gray

---

## Current implementation order

1. `NPlus1Scenario`
2. `CaffeineCacheScenario`
3. `ZgcVsG1Scenario`
4. `AutoboxingScenario`
5. `VirtualThreadsScenario`
