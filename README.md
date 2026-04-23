# spring-perf-lab

An interactive Spring Boot performance benchmarking tool that demonstrates the real impact of Java and Spring optimizations — with live metrics, before/after comparisons, and code explanations.

## What it does

You select one or more optimizations from a checklist, launch a test, and instantly see the performance delta vs a non-optimized baseline:

- Response time (elapsed ms)
- Heap memory used (MB)
- GC pause count and total duration
- Allocation rate (MB/s)
- SQL query count (N+1 detection)

Each optimization includes a **Before / After / Why** panel with annotated code snippets explaining the underlying mechanism.

## Optimization scenarios

| ID | Name | Category |
|----|------|----------|
| `n-plus-1` | N+1 Query Fix | Database |
| `caffeine-cache` | Caffeine L2 Cache | Database |
| `zgc-vs-g1` | ZGC vs G1GC | JVM & GC |
| `autoboxing` | Autoboxing Avoidance | Memory |
| `virtual-threads` | Virtual Threads (Java 21) | Concurrency |

## Test modes

- **QUICK** — runs both baseline and optimized in-process, results in seconds
- **LOAD** — Gatling simulates concurrent users over 30 seconds *(coming soon)*

## Tech stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 4.x |
| Metrics | Micrometer, Spring Actuator |
| Cache | Caffeine |
| Database | H2 (dev) / PostgreSQL (prod) |
| Frontend | React 18+, TypeScript |
| Charts | Recharts |
| HTTP client | Axios |
| Infra | Docker Compose, AWS EC2, GitHub Actions |

## Project structure

```
spring-perf-lab/
└── src/main/java/com/sentori/spring_perf_lab/
    ├── api/
    │   ├── dto/                   # Request & response records (DTOs)
    │   ├── PerfLabController.java # GET /api/scenarios, POST /api/test/run
    │   └── GlobalExceptionHandler.java
    ├── metrics/
    │   ├── MetricsCollector.java  # Reads JVM MXBeans
    │   ├── MetricsSnapshot.java   # Immutable metrics record
    │   └── MetricsDiff.java       # Delta baseline → optimized
    ├── runner/
    │   └── TestRunnerService.java # Orchestrates scenario execution
    └── scenarios/
        ├── PerfScenario.java              # Interface contract
        ├── ScenarioExecutionException.java
        └── nplus1/
            ├── Author.java
            ├── Book.java
            ├── AuthorRepository.java
            ├── NPlus1DataInitializer.java
            └── NPlus1Scenario.java
```

## REST API

### `GET /api/scenarios`
Returns the list of available scenarios.

```json
[
  {
    "id": "n-plus-1",
    "name": "N+1 Query Fix",
    "description": "Demonstrates how lazy-loading triggers one SQL query per author..."
  }
]
```

### `POST /api/test/run`
Runs one or more scenarios and returns before/after metrics.

**Request:**
```json
{
  "scenarioIds": ["n-plus-1"],
  "mode": "QUICK"
}
```

**Response:**
```json
{
  "mode": "QUICK",
  "results": [
    {
      "scenarioId": "n-plus-1",
      "baseline":  { "heapUsedMb": 45.2, "gcPauseMs": 12, "gcCount": 1, "sqlQueryCount": 51, "elapsedMs": 340 },
      "optimized": { "heapUsedMb": 41.1, "gcPauseMs":  8, "gcCount": 1, "sqlQueryCount":  1, "elapsedMs":  85 },
      "diff":      { "heapUsedMbDelta": -4.1, "gcPauseMsDelta": -4, "sqlQueryCountDelta": -50, "elapsedMsDelta": -255 }
    }
  ]
}
```

## Getting started

```bash
# Clone
git clone https://github.com/sentori94/spring-perf-lab.git
cd spring-perf-lab
```

### Backend (Java 21 required)

```powershell
# Windows (PowerShell) — set JAVA_HOME to JDK 21 first
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
.\mvnw.cmd spring-boot:run
```

Backend available at `http://localhost:8080`
H2 Console available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:perflab`)

### Frontend *(coming soon)*

```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at `http://localhost:5173`

## Requirements

- Java 21+
- Node.js 20+ *(frontend)*
- Docker & Docker Compose *(optional, for PostgreSQL in prod)*

## License

MIT
