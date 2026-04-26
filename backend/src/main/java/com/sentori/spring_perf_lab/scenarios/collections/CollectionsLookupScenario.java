package com.sentori.spring_perf_lab.scenarios.collections;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ArrayList.contains() O(n) vs HashSet.contains() O(1).
 *
 * - BASELINE  : List.contains() parcourt tous les éléments jusqu'à trouver une correspondance
 *               → O(n) par recherche, O(n²) total sur N lookups
 * - OPTIMIZED : HashSet.contains() calcule le hashCode et accède directement au bucket
 *               → O(1) par recherche, O(n) total sur N lookups
 */
@Component
public class CollectionsLookupScenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(CollectionsLookupScenario.class);

    // 50 000 éléments dans la collection, 200 000 lookups (certains trouvés, d'autres non)
    private static final int COLLECTION_SIZE = 50_000;
    private static final int LOOKUP_COUNT    = 200_000;

    // Données construites une seule fois — partagées entre runs, aucun état mutable
    private static final List<Integer> LIST;
    private static final Set<Integer>  SET;
    private static final int[]         LOOKUPS;

    static {
        LIST = new ArrayList<>(COLLECTION_SIZE);
        SET  = new HashSet<>(COLLECTION_SIZE * 2);
        for (int i = 0; i < COLLECTION_SIZE; i++) {
            LIST.add(i);
            SET.add(i);
        }
        // Alternance de valeurs présentes et absentes pour éviter les optimisations JIT
        LOOKUPS = new int[LOOKUP_COUNT];
        for (int i = 0; i < LOOKUP_COUNT; i++) {
            // ~50% présents (0..50k), ~50% absents (50k..100k)
            LOOKUPS[i] = i % (COLLECTION_SIZE * 2);
        }
    }

    private final MetricsCollector metricsCollector;

    public CollectionsLookupScenario(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override public String getId()     { return "collections-lookup"; }
    @Override public String getImpact() { return "HIGH"; }
    @Override public String getName() { return "ArrayList O(n) vs HashSet O(1)"; }

    @Override
    public String getDescription() {
        return "200 000 lookups sur une collection de 50 000 éléments : " +
               "ArrayList.contains() parcourt jusqu'à 50 000 éléments par recherche (O(n)), " +
               "HashSet.contains() répond en temps constant via hashCode (O(1)).";
    }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: List.contains() — scan linéaire à chaque lookup
        try {
            RunStart runStart = metricsCollector.start();

            int found = 0;
            for (int key : LOOKUPS) {
                if (LIST.contains(key)) found++;
            }

            log.debug("[collections-lookup] baseline done, found={}/{}", found, LOOKUP_COUNT);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: HashSet.contains() — O(1) via hashCode + bucket
        try {
            RunStart runStart = metricsCollector.start();

            int found = 0;
            for (int key : LOOKUPS) {
                if (SET.contains(key)) found++;
            }

            log.debug("[collections-lookup] optimized done, found={}/{}", found, LOOKUP_COUNT);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — ArrayList.contains() : scan linéaire O(n)
                // Pour chaque lookup, Java compare l'élément cherché avec chaque élément
                // de la liste jusqu'à trouver une correspondance (ou atteindre la fin)

                List<Integer> list = new ArrayList<>();
                for (int i = 0; i < 50_000; i++) list.add(i);

                int found = 0;
                for (int key : lookupsToPerform) {
                    if (list.contains(key)) found++;
                    //        ↑ parcourt 0, 1, 2, ..., jusqu'à trouver key ou épuiser la liste
                    //          → jusqu'à 50 000 comparaisons par appel
                    //          → 200 000 lookups × ~25 000 comparaisons en moyenne = 5 milliards
                }
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — HashSet.contains() : accès direct O(1)
                // Java calcule hashCode(key), localise le bucket correspondant,
                // et compare uniquement les éléments de ce bucket (1 en moyenne)

                Set<Integer> set = new HashSet<>();
                for (int i = 0; i < 50_000; i++) set.add(i);

                int found = 0;
                for (int key : lookupsToPerform) {
                    if (set.contains(key)) found++;
                    //       ↑ hashCode(key) → index bucket → 1 comparaison
                    //         → temps constant quelle que soit la taille du Set
                    //         → 200 000 lookups × ~1 comparaison = 200 000
                }
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                ArrayList stocke ses éléments dans un tableau contigu en mémoire.
                contains() doit parcourir ce tableau de gauche à droite en comparant
                chaque élément avec la valeur cherchée — c'est O(n).

                HashSet stocke ses éléments dans une table de hachage (tableau de buckets).
                contains() calcule hashCode(valeur), dérive l'index du bucket directement,
                et ne compare que les éléments de ce bucket (idéalement 1) — c'est O(1).

                Complexité totale pour 200 000 lookups sur 50 000 éléments :
                  BASELINE  (ArrayList) : O(n × lookups) = O(50 000 × 200 000) = 10 milliards d'ops
                  OPTIMIZED (HashSet)   : O(lookups)     = O(200 000)           = 200 000 ops

                Facteur théorique : ×50 000 → en pratique atténué par le cache CPU
                (ArrayList est contigu en mémoire, très cache-friendly).

                Règle générale :
                  • Besoin de lookups fréquents          → Set / Map
                  • Besoin d'accès par index (get(i))    → ArrayList
                  • Besoin d'insertions/suppressions fréquentes en début de liste → LinkedList
                """;
    }
}
