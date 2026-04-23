# spring-perf-lab

An interactive Spring Boot performance benchmarking tool that demonstrates the real impact of Java and Spring optimizations — with live metrics, before/after comparisons, and code explanations.

## What it does

You select one or more optimizations from a checklist, launch a load test, and instantly see the performance delta vs a non-optimized baseline:

- Response time (avg, p95, p99)
- Throughput (req/s)
- Heap memory used (MB)
- GC pause count and total duration
- Allocation rate (MB/s)
- SQL query count (N+1 detection)
- Cache hit rate

Each optimization includes a **Before / After / Why** panel with annotated code snippets explaining the underlying mechanism.

## Optimization categories

- **Database** — N+1 JPA fix, Caffeine L2 cache, DTO projections, batch inserts, HikariCP tuning
- **JVM & GC** — ZGC vs G1GC, heap sizing, string deduplication, AlwaysPreTouch
- **Memory** — autoboxing avoidance, object pooling, off-heap buffers, weak/soft references
- **Collections** — ArrayList vs LinkedList, EnumMap/EnumSet, initial capacity tuning
- **Concurrency** — Virtual threads (Java 21), @Async + ThreadPoolTaskExecutor, parallel streams
- **HTTP** — GZIP compression, HTTP/2, Jackson tuning, pagination

## Test modes

- **Quick mode** — runs a tight loop (10,000 iterations) in-process, results in ~2–3 seconds
- **Load test mode** — Gatling simulates 50 virtual users over 30 seconds, realistic throughput measurement

## Tech stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.x |
| Metrics | Micrometer, Spring Actuator |
| Load testing | Gatling (programmatic) |
| Cache | Caffeine |
| Database | PostgreSQL + HikariCP |
| Frontend | Angular 17+ |
| Charts | ngx-charts |
| Infra | Docker Compose, AWS EC2, GitHub Actions |

## Project structure

```
spring-perf-lab/
├── backend/
│   ├── src/main/java/
│   │   ├── scenarios/         # One class per optimization (baseline + optimized)
│   │   ├── metrics/           # Micrometer capture and comparison logic
│   │   ├── runner/            # Test orchestration (quick + Gatling modes)
│   │   └── api/               # REST endpoints consumed by frontend
│   └── src/test/
│       └── gatling/           # Load test simulations
├── frontend/
│   └── src/app/
│       ├── checklist/         # Optimization selector component
│       ├── dashboard/         # Metrics comparison dashboard
│       └── detail/            # Before/After/Why panel
├── infra/
│   ├── docker-compose.yml
│   └── terraform/             # AWS EC2 provisioning
└── .github/
    └── workflows/             # CI/CD pipeline
```

## Getting started

```bash
# Clone
git clone https://github.com/sentori94/spring-perf-lab.git
cd spring-perf-lab

# Run locally with Docker Compose
docker compose up

# Backend available at http://localhost:8080
# Frontend available at http://localhost:4200
```

## Running a benchmark

1. Open the app at `http://localhost:4200`
2. Select one or more optimizations from the sidebar
3. Choose **Quick** or **Load test** mode
4. Click **Lancer le test**
5. View the before/after comparison table and memory charts
6. Click any row to open the code explanation panel

## Requirements

- Java 21+
- Docker & Docker Compose
- Node.js 20+ (frontend dev)

## License

MIT
