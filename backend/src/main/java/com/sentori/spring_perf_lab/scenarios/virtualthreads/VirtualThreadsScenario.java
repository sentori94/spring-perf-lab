package com.sentori.spring_perf_lab.scenarios.virtualthreads;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Virtual Threads (Java 21) vs Platform Threads.
 *
 * - BASELINE  : pool de threads plateforme limité (50 threads OS)
 *               → les tâches en attente I/O bloquent les threads du pool
 * - OPTIMIZED : Executors.newVirtualThreadPerTaskExecutor()
 *               → chaque tâche obtient son propre thread virtuel,
 *                 les blocages ne monopolisent plus les threads OS
 */
@Component
public class VirtualThreadsScenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadsScenario.class);

    // 500 tâches simulant chacune 20 ms de latence I/O (appel réseau, base de données, etc.)
    private static final int TASK_COUNT    = 500;
    private static final int IO_LATENCY_MS = 20;

    // Taille du pool de threads plateforme (typique d'un pool HTTP ou JDBC)
    private static final int PLATFORM_POOL_SIZE = 50;

    private final MetricsCollector metricsCollector;

    public VirtualThreadsScenario(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override public String getId()   { return "virtual-threads"; }
    @Override public String getName() { return "Virtual Threads vs Platform Threads"; }

    @Override
    public String getDescription() {
        return "Compare l'exécution de 500 tâches I/O-bound sur un pool de 50 threads OS " +
               "vs les Virtual Threads Java 21 qui ne bloquent pas les threads plateforme.";
    }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: pool de 50 threads plateforme OS
        // 500 tâches / 50 threads = 10 batches de 50, chacun attendant 20 ms
        // → temps total ≈ 10 × 20 ms = ~200 ms
        try {
            RunStart runStart = metricsCollector.start();

            ExecutorService executor = Executors.newFixedThreadPool(PLATFORM_POOL_SIZE);
            try {
                List<Future<Integer>> futures = new ArrayList<>(TASK_COUNT);
                for (int i = 0; i < TASK_COUNT; i++) {
                    final int taskId = i;
                    futures.add(executor.submit(() -> simulateIoWork(taskId)));
                }
                // attendre la complétion de toutes les tâches
                int errorCount = awaitAll(futures);
                log.debug("[virtual-threads] baseline done (platform pool={}), errors={}", PLATFORM_POOL_SIZE, errorCount);
            } finally {
                executor.shutdown();
            }

            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: un thread virtuel par tâche (Java 21)
        // Les 500 tâches sont lancées quasi-simultanément
        // Quand un thread virtuel fait Thread.sleep() / attend I/O, il libère le thread OS
        // → temps total ≈ 20 ms (toutes les tâches s'exécutent en parallèle)
        try {
            RunStart runStart = metricsCollector.start();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<Integer>> futures = new ArrayList<>(TASK_COUNT);
                for (int i = 0; i < TASK_COUNT; i++) {
                    final int taskId = i;
                    futures.add(executor.submit(() -> simulateIoWork(taskId)));
                }
                int errorCount = awaitAll(futures);
                log.debug("[virtual-threads] optimized done (virtual threads), errors={}", errorCount);
            }

            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    /**
     * Simule une opération I/O bloquante (lecture réseau, requête externe, etc.)
     */
    private int simulateIoWork(int taskId) throws InterruptedException {
        Thread.sleep(IO_LATENCY_MS); // bloc le thread → libéré gratuitement par les VT
        return taskId; // retourne un résultat symbolique
    }

    /**
     * Attend tous les Futures et compte les éventuelles erreurs.
     */
    private int awaitAll(List<Future<Integer>> futures) {
        int errors = 0;
        for (Future<Integer> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                errors++;
            } catch (ExecutionException e) {
                errors++;
            }
        }
        return errors;
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — pool de 50 threads plateforme (threads OS)
                // 500 tâches dont chacune bloque 20 ms sur I/O
                // Les 500 tâches se battent pour 50 threads → files d'attente

                ExecutorService executor = Executors.newFixedThreadPool(50);

                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < 500; i++) {
                    futures.add(executor.submit(() -> {
                        Thread.sleep(20); // ← bloque un thread OS pendant 20 ms
                        return doWork();
                    }));
                }
                futures.forEach(f -> f.get()); // attend la complétion de tout
                executor.shutdown();

                // Temps total ≈ ceil(500/50) × 20 ms = ~200 ms
                // Mémoire : 50 threads × ~1 Mo chacun = ~50 Mo de stack
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — Virtual Threads (Java 21) : un thread virtuel par tâche
                // Les VT ne monopolisent pas les threads OS pendant les blocages I/O

                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                    List<Future<?>> futures = new ArrayList<>();
                    for (int i = 0; i < 500; i++) {
                        futures.add(executor.submit(() -> {
                            Thread.sleep(20); // ← libère le thread OS, ne bloque rien
                            return doWork();
                        }));
                    }
                    futures.forEach(f -> f.get());

                } // try-with-resources ferme et attend automatiquement

                // Temps total ≈ 20 ms (500 VT s'exécutent quasi-simultanément)
                // Mémoire : ~100 octets par VT au lieu de ~1 Mo par thread plateforme
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                Un thread plateforme (OS thread) consomme ~1 Mo de stack et est géré par
                le système d'exploitation. Créer 500 threads plateforme coûte ~500 Mo,
                et le scheduler OS passe son temps à les faire et défaire de la CPU.

                Un thread virtuel (Java 21, Project Loom) est géré par la JVM :
                  • Mémoire initiale : ~few hundred bytes (heap, pas stack native)
                  • Quand un VT appelle Thread.sleep(), socket.read(), etc.,
                    la JVM "démontre" (unmounts) le VT du thread OS porteur
                  • Le thread OS est immédiatement réutilisé pour un autre VT
                  • Quand l'attente I/O est terminée, le VT est remonté (remounted)

                Impact sur ce scénario :
                  BASELINE  : 50 threads plateforme pour 500 tâches de 20 ms
                              → 10 batches séquentiels → ~200 ms total
                  OPTIMIZED : 500 virtual threads, tous lancés simultanément
                              → tous bloquent sur I/O en parallèle → ~20 ms total
                              → gain de ~10× sur l'elapsed time

                Virtual Threads n'accélèrent PAS le code CPU-bound (calcul pur),
                ils brillent uniquement sur le code I/O-bound : appels HTTP, JDBC,
                lecture fichier, attentes réseau — ce qui représente 80%+ du code
                d'une application Spring Boot typique.

                Pour l'activer dans Spring Boot (inclus depuis Spring Boot 3.2) :
                  spring.threads.virtual.enabled=true
                """;
    }
}
