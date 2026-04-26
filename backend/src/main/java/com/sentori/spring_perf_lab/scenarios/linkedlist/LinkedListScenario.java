package com.sentori.spring_perf_lab.scenarios.linkedlist;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * ArrayList vs LinkedList — accès aléatoire par index.
 *
 * - BASELINE  : LinkedList.get(i) — parcourt les maillons depuis la tête jusqu'à l'index i
 *               → O(n) par accès, O(n²) total sur N accès aléatoires
 * - OPTIMIZED : ArrayList.get(i) — accès direct dans le tableau sous-jacent
 *               → O(1) par accès, O(n) total
 *
 * LinkedList est souvent choisie "par réflexe" pour des listes modifiables,
 * mais ses performances d'accès par index sont désastreuses à grande échelle.
 */
@Component
public class LinkedListScenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(LinkedListScenario.class);

    // 5 000 éléments, 10 000 accès aléatoires par index
    // → LinkedList : ~2 500 traversées en moyenne par accès = 25M ops (~1-2s)
    // → ArrayList  : 10 000 accès directs O(1) (~1ms)
    private static final int LIST_SIZE    = 5_000;
    private static final int ACCESS_COUNT = 10_000;

    // Séquence d'index aléatoires précalculée — même séquence pour baseline et optimized
    private static final int[] RANDOM_INDICES;

    static {
        RANDOM_INDICES = new int[ACCESS_COUNT];
        // Pseudo-random déterministe (pas de Random pour reproduire le même workload)
        for (int i = 0; i < ACCESS_COUNT; i++) {
            RANDOM_INDICES[i] = (int) ((i * 2_654_435_761L) >>> 17) % LIST_SIZE;
        }
    }

    private final MetricsCollector metricsCollector;

    public LinkedListScenario(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override public String getId()     { return "linked-list"; }
    @Override public String getImpact() { return "HIGH"; }
    @Override public String getName() { return "LinkedList O(n) vs ArrayList O(1)"; }

    @Override
    public String getDescription() {
        return "100 000 accès par index sur une liste de 10 000 éléments : " +
               "LinkedList.get(i) traverse les maillons un par un (O(n)), " +
               "ArrayList.get(i) accède directement au tableau sous-jacent (O(1)).";
    }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: LinkedList.get(i) — traversée de maillons
        try {
            RunStart runStart = metricsCollector.start();

            List<Integer> list = new LinkedList<>();
            for (int i = 0; i < LIST_SIZE; i++) list.add(i);

            long sum = 0;
            for (int idx : RANDOM_INDICES) {
                sum += list.get(idx); // parcourt jusqu'à idx maillons depuis la tête
            }

            log.debug("[linked-list] baseline done, sum={}", sum);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: ArrayList.get(i) — accès direct O(1)
        try {
            RunStart runStart = metricsCollector.start();

            List<Integer> list = new ArrayList<>(LIST_SIZE);
            for (int i = 0; i < LIST_SIZE; i++) list.add(i);

            long sum = 0;
            for (int idx : RANDOM_INDICES) {
                sum += list.get(idx); // array[idx] — une seule opération mémoire
            }

            log.debug("[linked-list] optimized done, sum={}", sum);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — LinkedList.get(i) : traversée O(n)
                // LinkedList est une liste doublement chaînée : chaque élément (nœud)
                // contient la valeur + un pointeur vers le nœud précédent et suivant.
                // get(i) doit partir de la tête et avancer maillon par maillon jusqu'à i.

                List<Integer> list = new LinkedList<>();
                for (int i = 0; i < 10_000; i++) list.add(i);

                long sum = 0;
                for (int idx : randomIndices) {       // 100 000 accès
                    sum += list.get(idx);
                    // Chemin interne de LinkedList.get(idx) :
                    //   Node<E> x = first;
                    //   for (int i = 0; i < idx; i++)  ← parcourt jusqu'à idx maillons
                    //       x = x.next;
                    //   return x.item;
                    // En moyenne : idx/2 sauts → ~2 500 sauts par accès
                }
                // Total : 100 000 × 2 500 = 250 millions de sauts de pointeurs
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — ArrayList.get(i) : accès direct O(1)
                // ArrayList stocke ses éléments dans un tableau Object[] contigu en mémoire.
                // get(i) retourne directement elementData[i] — une seule instruction.

                List<Integer> list = new ArrayList<>(10_000);
                for (int i = 0; i < 10_000; i++) list.add(i);

                long sum = 0;
                for (int idx : randomIndices) {       // 100 000 accès
                    sum += list.get(idx);
                    // Chemin interne de ArrayList.get(idx) :
                    //   return (E) elementData[idx];  ← accès direct au tableau
                    // Quelle que soit la taille de la liste : 1 seule opération
                }
                // Total : 100 000 × 1 = 100 000 accès directs
                // + avantage cache CPU : les données sont contiguës en mémoire (cache-friendly)
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                La différence fondamentale est la structure de données sous-jacente :

                ArrayList  → tableau Object[] contigu en mémoire
                  • get(i) : elementData[i] — O(1), indépendant de la taille
                  • Très cache-friendly : les éléments sont adjacents en mémoire (prefetcher CPU)
                  • Redimensionnement automatique par x1.5 lors des add() (coût amorti)

                LinkedList → liste doublement chaînée (nœuds éparpillés sur le heap)
                  • get(i) : parcours depuis la tête ou la queue — O(n/2) en moyenne
                  • Cache-hostile : chaque nœud est un objet distinct, potentiellement loin
                    de ses voisins en mémoire → nombreux cache misses
                  • Overhead mémoire : chaque nœud alloue prev + next + valeur (~48 octets
                    au lieu de 4 pour un int dans un tableau)

                Quand LinkedList est pertinente :
                  ✅ Insertions/suppressions fréquentes en tête ou en queue (O(1) vs O(n))
                  ✅ Implémentation de Deque / Queue avec poll() / offer()
                  ✅ Taille inconnue avec beaucoup de modifications structurelles

                Quand ArrayList est toujours meilleure :
                  ✅ Accès aléatoire par index (get, set)
                  ✅ Itération séquentielle (meilleure localité cache)
                  ✅ La grande majorité des cas d'utilisation réels

                Règle pratique : utiliser ArrayList par défaut, LinkedList uniquement
                quand des insertions/suppressions en tête sont le cas d'utilisation dominant.
                """;
    }
}
