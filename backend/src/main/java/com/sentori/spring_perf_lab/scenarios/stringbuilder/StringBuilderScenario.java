package com.sentori.spring_perf_lab.scenarios.stringbuilder;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * String concatenation — += vs StringBuilder.
 *
 * - BASELINE  : concaténation par += dans une boucle
 *               → chaque += crée un nouveau String sur le heap (String est immuable)
 *               → N itérations = N objets intermédiaires abandonnés immédiatement
 * - OPTIMIZED : StringBuilder.append() réutilise un buffer interne
 *               → un seul objet alloué, aucun garbage intermédiaire
 */
@Component
public class StringBuilderScenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(StringBuilderScenario.class);

    // 8 000 mots de ~10 chars → string finale ~80 Ko
    // suffisant pour que l'écart d'allocation soit visible dans les métriques
    private static final int WORD_COUNT   = 8_000;
    private static final String WORD      = "benchmark";
    private static final String SEPARATOR = ", ";

    private final MetricsCollector metricsCollector;

    public StringBuilderScenario(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override public String getId()     { return "string-builder"; }
    @Override public String getImpact() { return "LOW"; }
    @Override public String getName() { return "String += vs StringBuilder"; }

    @Override
    public String getDescription() {
        return "Mesure l'impact de la concaténation naïve avec += : " +
               "chaque opération crée un nouveau String immuable sur le heap, " +
               "vs StringBuilder qui travaille in-place sans garbage.";
    }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: String += — chaque concaténation alloue un nouvel objet String
        try {
            RunStart runStart = metricsCollector.start();

            String result = "";
            for (int i = 0; i < WORD_COUNT; i++) {
                result += WORD;          // new String(result + WORD) à chaque itération
                if (i < WORD_COUNT - 1) {
                    result += SEPARATOR; // encore une allocation
                }
            }

            log.debug("[string-builder] baseline done, length={}", result.length());
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: StringBuilder — buffer interne redimensionné si nécessaire,
        // aucun objet intermédiaire créé
        try {
            RunStart runStart = metricsCollector.start();

            // pré-capacité estimée pour éviter même les redimensionnements internes
            StringBuilder sb = new StringBuilder((WORD.length() + SEPARATOR.length()) * WORD_COUNT);
            for (int i = 0; i < WORD_COUNT; i++) {
                sb.append(WORD);
                if (i < WORD_COUNT - 1) {
                    sb.append(SEPARATOR);
                }
            }
            String result = sb.toString(); // une seule allocation finale

            log.debug("[string-builder] optimized done, length={}", result.length());
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — String += dans une boucle
                // String est immuable : chaque += compile en new StringBuilder(result).append(word).toString()
                // → crée 2 objets temporaires par itération (StringBuilder + String)
                // → 8 000 itérations = ~16 000 objets abandonnés → pression GC significative

                String result = "";
                for (int i = 0; i < 8_000; i++) {
                    result += "benchmark";   // ← nouvel objet String à chaque tour
                    result += ", ";          // ← encore un nouvel objet String
                }

                // La taille du String croît linéairement → chaque copie est plus longue
                // Complexité temporelle : O(N²) — le pire cas pour la concaténation
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — StringBuilder avec pré-capacité
                // Un seul buffer char[] redimensionnable ; append() écrit dedans sans alloc externe
                // Complexité temporelle : O(N) — linéaire

                StringBuilder sb = new StringBuilder(8_000 * 11); // pré-capacité, 0 resize

                for (int i = 0; i < 8_000; i++) {
                    sb.append("benchmark"); // ← écriture directe dans le buffer
                    sb.append(", ");        // ← idem, aucune allocation
                }

                String result = sb.toString(); // une seule allocation, à la fin seulement
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                En Java, String est une classe immuable : une fois créée, une instance ne peut
                jamais être modifiée. Par conséquent, l'opérateur += sur un String est
                du sucre syntaxique pour :

                  result = new StringBuilder(result).append(word).toString();

                À chaque itération, le compilateur crée un StringBuilder temporaire, y copie
                l'intégralité de result, ajoute le nouveau mot, puis crée un nouveau String.
                L'ancien result et le StringBuilder deviennent immédiatement du garbage.

                Avec N = 8 000 itérations :
                  • Itération 1 : copie 0 chars   → 9 chars
                  • Itération 2 : copie 9 chars   → 20 chars
                  • ...
                  • Itération N : copie ~(N-1)×11 chars
                  Somme totale copiée ≈ N² × longueur_mot / 2 → complexité O(N²)

                StringBuilder maintient un char[] interne. append() écrit à la fin, sans copier
                l'existant. Redimensionnement uniquement si la capacité est dépassée (×2).
                Complexité : O(N).

                Règle pratique :
                  • En dehors des boucles : += est acceptable (le compilateur optimise)
                  • En boucle ou avec un nombre inconnu de concaténations : toujours StringBuilder
                  • Java 21 optimise certains cas avec String Templates, mais la règle reste valable
                """;
    }
}
