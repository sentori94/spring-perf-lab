package com.sentori.spring_perf_lab.scenarios.caffeine;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CaffeineCacheScenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(CaffeineCacheScenario.class);

    private static final List<String> CATEGORIES = Arrays.asList(
            "electronics", "books", "clothing", "food", "sports"
    );
    // Nombre de fois qu'on appelle chaque catégorie — simule des requêtes répétées
    private static final int REPEAT = 20;

    private final ProductService productService;
    private final MetricsCollector metricsCollector;

    public CaffeineCacheScenario(ProductService productService, MetricsCollector metricsCollector) {
        this.productService   = productService;
        this.metricsCollector = metricsCollector;
    }

    @Override public String getId()          { return "caffeine-cache"; }    @Override public String getImpact()       { return "HIGH"; }    @Override public String getName()        { return "Caffeine L2 Cache"; }
    @Override public String getDescription() {
        return "Demonstrates how Caffeine caches query results in memory, " +
               "eliminating repeated SQL hits for the same data.";
    }

    @Override
    public MetricsSnapshot runBaseline() {
        // BASELINE: chaque appel frappe la base de données, même pour les mêmes données
        try {
            productService.evictCache();
            RunStart runStart = metricsCollector.start();

            long sqlCount = 0;
            for (int i = 0; i < REPEAT; i++) {
                for (String category : CATEGORIES) {
                    productService.getByCategoryNoCache(category);
                    sqlCount++;
                }
            }

            log.debug("[caffeine] baseline: {} SQL queries", sqlCount);
            return metricsCollector.snapshot(runStart, sqlCount);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: Caffeine sert depuis la mémoire après le 1er appel par catégorie
        try {
            productService.evictCache(); // reset propre avant chaque run
            RunStart runStart = metricsCollector.start();

            // 1er passage : 1 requête SQL par catégorie → mise en cache
            // Passages suivants : 0 requête SQL, réponse depuis Caffeine
            long sqlCount = CATEGORIES.size(); // seulement le 1er passage touche la BDD
            for (int i = 0; i < REPEAT; i++) {
                for (String category : CATEGORIES) {
                    productService.getByCategoryWithCache(category);
                }
            }

            log.debug("[caffeine] optimized: {} SQL queries (cache hits: {})",
                    sqlCount, (long) REPEAT * CATEGORIES.size() - sqlCount);
            return metricsCollector.snapshot(runStart, sqlCount);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — pas de cache, chaque appel génère une requête SQL
                // Avec 5 catégories x 20 répétitions = 100 requêtes SQL

                public List<Product> getByCategoryNoCache(String category) {
                    return productRepository.findByCategory(category); // SELECT à chaque fois
                }

                // Dans le scénario :
                for (int i = 0; i < 20; i++) {
                    for (String category : CATEGORIES) {
                        productService.getByCategoryNoCache(category); // → SQL
                    }
                }
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — @Cacheable met le résultat en mémoire (Caffeine)
                // Seul le 1er appel par catégorie touche la BDD = 5 requêtes SQL au total

                @Cacheable(value = "productsByCategory", key = "#category")
                public List<Product> getByCategoryWithCache(String category) {
                    return productRepository.findByCategory(category); // SQL seulement la 1ère fois
                }

                // Dans le scénario :
                for (int i = 0; i < 20; i++) {
                    for (String category : CATEGORIES) {
                        productService.getByCategoryWithCache(category); // → cache hit dès le 2ème passage
                    }
                }
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                Sans cache, chaque appel à getByCategoryNoCache() génère une requête SQL.
                Avec 5 catégories et 20 répétitions, on cumule 100 requêtes identiques.

                Avec @Cacheable(Caffeine), Spring intercepte l'appel :
                  - 1er appel pour "electronics" → SQL exécuté, résultat mis en cache
                  - 2ème, 3ème... appel pour "electronics" → réponse depuis la mémoire, 0 SQL

                Au total : seulement 5 requêtes SQL (une par catégorie), puis 95 cache hits.

                Caffeine est un cache in-memory (L2) ultra-rapide basé sur W-TinyLFU,
                un algorithme qui maximise le taux de hit en évitant les évictions prématurées.

                Idéal pour des données peu changeantes : catalogues, référentiels, configs.
                À éviter pour des données mises à jour fréquemment sans stratégie d'invalidation.
                """;
    }
}

