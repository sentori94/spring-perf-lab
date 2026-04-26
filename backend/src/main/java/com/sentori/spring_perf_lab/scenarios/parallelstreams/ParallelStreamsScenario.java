package com.sentori.spring_perf_lab.scenarios.parallelstreams;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Parallel Streams — sequential vs parallel pour workload CPU-bound.
 *
 * - BASELINE  : stream() séquentiel — un seul thread traite les éléments un à un
 * - OPTIMIZED : parallelStream() — le ForkJoinPool commun distribue le travail
 *               sur tous les cœurs disponibles → gain ≈ nombre de cœurs
 *
 * La tâche est délibérément CPU-bound (calcul mathématique pur) pour que
 * parallelStream() soit pertinent. Pour les tâches I/O-bound, préférer Virtual Threads.
 */
@Component
public class ParallelStreamsScenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(ParallelStreamsScenario.class);

    // 2 000 éléments avec un calcul ~coûteux par élément
    private static final int ELEMENT_COUNT  = 2_000;
    // Nombre d'itérations de la série de Leibniz par élément (approximation de π)
    private static final int LEIBNIZ_TERMS  = 500_000;

    // Données immuables partagées — pas d'état mutable, parallelStream() safe
    private static final List<Integer> DATA =
            IntStream.range(0, ELEMENT_COUNT).boxed().toList();

    private final MetricsCollector metricsCollector;

    public ParallelStreamsScenario(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override public String getId()     { return "parallel-streams"; }
    @Override public String getImpact() { return "MEDIUM"; }
    @Override public String getName() { return "Parallel Streams (CPU-bound)"; }

    @Override
    public String getDescription() {
        return "Compare stream() séquentiel vs parallelStream() sur un calcul CPU-intensif : " +
               "approximation de π par la série de Leibniz. Le gain est proportionnel " +
               "au nombre de cœurs disponibles.";
    }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: stream() — un seul thread, traitement séquentiel
        try {
            RunStart runStart = metricsCollector.start();

            double sum = DATA.stream()
                    .mapToDouble(i -> computeLeibniz(i, LEIBNIZ_TERMS))
                    .sum();

            log.debug("[parallel-streams] baseline done, sum={}", sum);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: parallelStream() — ForkJoinPool commun, tous les cœurs utilisés
        try {
            RunStart runStart = metricsCollector.start();

            double sum = DATA.parallelStream()
                    .mapToDouble(i -> computeLeibniz(i, LEIBNIZ_TERMS))
                    .sum();

            log.debug("[parallel-streams] optimized done, sum={}", sum);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    /**
     * Calcule N termes de la série de Leibniz (approximation de π/4) à partir d'un offset.
     * Opération purement CPU-bound, sans I/O ni allocation significative.
     */
    private double computeLeibniz(int offset, int terms) {
        double result = 0.0;
        for (int k = 0; k < terms; k++) {
            long n = (long) offset * terms + k;
            result += (n % 2 == 0 ? 1.0 : -1.0) / (2 * n + 1);
        }
        return result;
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — stream() séquentiel
                // Un seul thread du pool applicatif exécute chaque calcul l'un après l'autre
                // Aucun parallélisme : les autres cœurs CPU restent inactifs

                List<Integer> data = IntStream.range(0, 2_000).boxed().toList();

                double sum = data.stream()               // ← séquentiel
                        .mapToDouble(i -> computePi(i))  // exécuté par 1 thread
                        .sum();

                // Temps total ≈ N × temps_par_tâche
                // Tous les cœurs sauf 1 sont au repos
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — parallelStream() distribué sur le ForkJoinPool commun
                // Java découpe automatiquement la liste en sous-tâches (fork)
                // puis agrège les résultats (join) — pattern MapReduce natif

                List<Integer> data = IntStream.range(0, 2_000).boxed().toList();

                double sum = data.parallelStream()       // ← parallèle
                        .mapToDouble(i -> computePi(i))  // distribué sur N cœurs
                        .sum();                          // réduction thread-safe

                // Temps total ≈ N × temps_par_tâche / nb_cœurs
                // Gain typique : ×2 sur dual-core, ×4 sur quad-core, etc.

                // IMPORTANT : valide uniquement pour workload CPU-bound et sans état partagé mutable
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                parallelStream() s'appuie sur le ForkJoinPool.commonPool() de la JVM,
                qui crée par défaut autant de threads que de cœurs CPU disponibles
                (Runtime.getRuntime().availableProcessors() - 1).

                Le framework Fork/Join découpe récursivement la source de données
                (ici une List) en sous-listes jusqu'à une taille seuil, distribue
                chaque sous-tâche à un thread disponible (work-stealing), puis
                fusionne les résultats partiels.

                Quand utiliser parallelStream() :
                  ✅ Tâches CPU-bound (calcul mathématique, compression, chiffrement)
                  ✅ Collections de grande taille (> quelques milliers d'éléments)
                  ✅ Fonctions sans état partagé mutable (lambda pures)
                  ✅ Opérations indépendantes (pas d'ordre requis)

                Quand NE PAS utiliser parallelStream() :
                  ❌ Tâches I/O-bound (attentes réseau, JDBC) → préférer Virtual Threads
                  ❌ Petites collections (overhead > gain)
                  ❌ Opérations avec effets de bord (concurrences sur des variables partagées)
                  ❌ Code dans un contexte transactionnel Spring (contexte non propagé aux threads FJ)

                Sur ce serveur, le gain mesuré dépend du nombre de vCPU disponibles.
                Dans un environnement cloud (t3.micro = 2 vCPU), le gain attendu est ≈ ×2.
                """;
    }
}
