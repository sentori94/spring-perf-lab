package com.sentori.spring_perf_lab.scenarios.zgcvsg1;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * ZGC vs G1GC — allocation pressure demo.
 *
 * Puisqu'on ne peut pas changer de GC à l'exécution, ce scénario illustre
 * le pattern d'allocation qui explique POURQUOI ZGC a été conçu :
 *
 * - BASELINE  : beaucoup d'objets court-lived → nombreux cycles GC, longues pauses G1
 * - OPTIMIZED : réutilisation d'un buffer unique → quasi-zéro pression GC (ce que ZGC favorise)
 */
@Component
public class ZgcVsG1Scenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(ZgcVsG1Scenario.class);

    // 12 000 allocations × 10 Ko = ~120 Mo alloués/libérés dans la baseline
    private static final int ITERATIONS   = 12_000;
    private static final int CHUNK_BYTES  = 10 * 1024; // 10 Ko

    private final MetricsCollector metricsCollector;

    public ZgcVsG1Scenario(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override public String getId()   { return "zgc-vs-g1"; }
    @Override public String getName() { return "ZGC vs G1 — Allocation Pressure"; }

    @Override
    public String getDescription() {
        return "Illustre l'impact du pattern d'allocation sur le GC : " +
               "beaucoup d'objets court-lived (G1 peine) vs réutilisation de buffer (ZGC excelle).";
    }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: crée un nouveau tableau de bytes à chaque itération — tous deviennent du garbage
        try {
            RunStart runStart = metricsCollector.start();

            long checksum = 0;
            for (int i = 0; i < ITERATIONS; i++) {
                byte[] chunk = new byte[CHUNK_BYTES]; // nouvelle allocation à chaque tour
                for (int j = 0; j < chunk.length; j += 64) {
                    chunk[j] = (byte) (i ^ j);
                }
                checksum += chunk[0]; // empêche le JIT d'éliminer la boucle
            }

            log.debug("[zgc-vs-g1] baseline done, checksum={}", checksum);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: un seul buffer alloué, réutilisé à chaque itération — GC quasi inactif
        try {
            RunStart runStart = metricsCollector.start();

            byte[] reusableBuffer = new byte[CHUNK_BYTES]; // une seule allocation
            long checksum = 0;
            for (int i = 0; i < ITERATIONS; i++) {
                Arrays.fill(reusableBuffer, (byte) 0); // reset sans allouer
                for (int j = 0; j < reusableBuffer.length; j += 64) {
                    reusableBuffer[j] = (byte) (i ^ j);
                }
                checksum += reusableBuffer[0];
            }

            log.debug("[zgc-vs-g1] optimized done, checksum={}", checksum);
            return metricsCollector.snapshot(runStart, 0);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — nouvelle allocation à chaque itération
                // ~120 Mo alloués puis abandonnés → oblige le GC à intervenir fréquemment
                // G1GC doit compacter la heap : stop-the-world pauses visibles

                for (int i = 0; i < 12_000; i++) {
                    byte[] chunk = new byte[10 * 1024]; // ← nouvelle allocation
                    for (int j = 0; j < chunk.length; j += 64) {
                        chunk[j] = (byte) (i ^ j);
                    }
                    process(chunk);
                    // chunk sort de scope → devient du garbage immédiatement
                }
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — buffer alloué une seule fois, réutilisé en boucle
                // ZGC est conçu pour ce type de code : peu d'objets à tracer,
                // pauses < 1 ms même sous forte charge

                byte[] reusableBuffer = new byte[10 * 1024]; // ← une seule allocation

                for (int i = 0; i < 12_000; i++) {
                    Arrays.fill(reusableBuffer, (byte) 0); // reset propre, pas d'allocation
                    for (int j = 0; j < reusableBuffer.length; j += 64) {
                        reusableBuffer[j] = (byte) (i ^ j);
                    }
                    process(reusableBuffer);
                }
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                G1GC divise la heap en régions et utilise des "stop-the-world" pauses
                pour compacter les régions pleines. Plus il y a d'objets court-lived,
                plus G1 doit travailler : beaucoup de régions Eden remplies rapidement,
                minor GC fréquents, et parfois des Full GC coûteux.

                ZGC (disponible depuis Java 11, stable depuis Java 15) utilise des
                barrières de lecture colorées (colored pointers) pour faire la quasi-totalité
                du travail de GC de façon concurrente, sans arrêter les threads applicatifs.
                Ses pauses sont < 1 ms quelle que soit la taille du heap.

                Ce scénario illustre le pattern qui justifie ZGC :
                  BASELINE  : 12 000 × 10 Ko = ~120 Mo alloués puis abandonnés
                              → G1 enchaîne les minor GC, gcCount et gcPauseMs augmentent
                  OPTIMIZED : 1 seul buffer réutilisé 12 000 fois
                              → quasi-zéro GC activity, heapUsedMb stable

                Pour activer ZGC sur votre JVM :
                  java -XX:+UseZGC -jar app.jar

                Le pattern de réutilisation de buffer fonctionne avec n'importe quel GC,
                mais c'est précisément ce que ZGC rend moins critique grâce à ses pauses
                ultra-courtes.
                """;
    }
}
