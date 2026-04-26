package com.sentori.spring_perf_lab.scenarios.autoboxing;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Autoboxing — Integer/Long vs primitives.
 *
 * - BASELINE  : HashMap<Integer, Integer> — chaque put/get implique la création
 *               d'un objet Integer sur le heap (autoboxing silencieux)
 * - OPTIMIZED : tableau int[] comme compteur — zéro boxing, zéro garbage
 */
@Component
public class AutoboxingScenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(AutoboxingScenario.class);

    // 300 000 entrées avec 1 000 clés distinctes (chaque clé mise à jour 300 fois)
    private static final int OPERATIONS = 300_000;
    private static final int KEYS       = 1_000;

    private final MetricsCollector metricsCollector;

    public AutoboxingScenario(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override public String getId()     { return "autoboxing"; }
    @Override public String getImpact() { return "LOW"; }
    @Override public String getName() { return "Autoboxing — Integer vs int"; }

    @Override
    public String getDescription() {
        return "Mesure le coût caché de l'autoboxing Java : " +
               "HashMap<Integer,Integer> génère des milliers d'objets Integer vs " +
               "un simple tableau int[] qui n'en crée aucun.";
    }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: HashMap<Integer, Integer> — chaque opération merge() crée des Integer
        try {
            RunStart runStart = metricsCollector.start();

            Map<Integer, Integer> scoreboard = new HashMap<>(KEYS * 2);
            for (int i = 0; i < OPERATIONS; i++) {
                int key = i % KEYS;
                // merge autoboxe: key → Integer.valueOf(key), 1 → Integer.valueOf(1)
                // + l'ancienne valeur est unboxée, la nouvelle est reboxée
                scoreboard.merge(key, 1, Integer::sum);
            }

            // calcul du total — chaque accès unboxe un Integer
            long total = 0;
            for (int count : scoreboard.values()) {
                total += count;
            }

            log.debug("[autoboxing] baseline done, total={}", total);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: int[] — aucun objet créé, aucun boxing, aucun garbage
        try {
            RunStart runStart = metricsCollector.start();

            int[] scoreboard = new int[KEYS];
            for (int i = 0; i < OPERATIONS; i++) {
                scoreboard[i % KEYS]++; // incrément direct, zéro allocation
            }

            long total = 0;
            for (int count : scoreboard) {
                total += count; // widening int → long, pas de boxing
            }

            log.debug("[autoboxing] optimized done, total={}", total);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — HashMap<Integer, Integer>
                // Java boxe silencieusement int → Integer à chaque opération
                // 300 000 merge() = potentiellement 300 000 objets Integer sur le heap

                Map<Integer, Integer> scoreboard = new HashMap<>();

                for (int i = 0; i < 300_000; i++) {
                    int key = i % 1_000;
                    scoreboard.merge(key, 1, Integer::sum);
                    //              ↑        ↑
                    //        autoboxing   autoboxing — crée new Integer(1) à chaque appel
                }

                long total = 0;
                for (int count : scoreboard.values()) { // unboxing Integer → int à chaque itération
                    total += count;
                }
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — int[] comme table de comptage
                // Aucun objet créé, aucun boxing, accès direct en mémoire contiguë

                int[] scoreboard = new int[1_000]; // une seule allocation

                for (int i = 0; i < 300_000; i++) {
                    scoreboard[i % 1_000]++; // incrément direct — zéro allocation
                }

                long total = 0;
                for (int count : scoreboard) { // widening int → long, pas d'objet créé
                    total += count;
                }
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                En Java, les collections génériques (List<T>, Map<K,V>) ne peuvent pas stocker
                de types primitifs (int, long, double). Le compilateur insère automatiquement
                des appels à Integer.valueOf(i) (boxing) et intValue() (unboxing) — ce qui,
                selon le cas, crée un nouvel objet sur le heap.

                Coût de l'autoboxing :
                  • Allocation d'un objet Integer sur le heap (16 octets au lieu de 4)
                  • Pression accrue sur le GC
                  • Perte de localité cache (les Integer sont éparpillés en mémoire)
                  • Integer.valueOf() utilise un cache pour [-128, 127] mais au-delà,
                    chaque appel crée un nouvel objet

                Dans ce scénario avec 300 000 opérations et 1 000 clés :
                  BASELINE  : HashMap<Integer, Integer>
                              → jusqu'à 300 000 objets Integer alloués
                              → gcCount et heapUsedMb significativement plus élevés

                  OPTIMIZED : int[1000]
                              → un seul objet alloué (le tableau)
                              → aucun GC déclenché, allocationRate quasi nulle

                En production, ce pattern se retrouve souvent dans les boucles intensives,
                les compteurs, les caches d'ID, et les agrégations de métriques.
                """;
    }
}
