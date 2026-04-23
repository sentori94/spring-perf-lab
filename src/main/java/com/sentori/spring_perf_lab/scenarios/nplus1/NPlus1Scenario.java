package com.sentori.spring_perf_lab.scenarios.nplus1;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class NPlus1Scenario implements PerfScenario {

    private static final Logger log = LoggerFactory.getLogger(NPlus1Scenario.class);

    private final AuthorRepository authorRepository;
    private final MetricsCollector metricsCollector;

    public NPlus1Scenario(AuthorRepository authorRepository, MetricsCollector metricsCollector) {
        this.authorRepository = authorRepository;
        this.metricsCollector  = metricsCollector;
    }

    @Override
    public String getId() { return "n-plus-1"; }

    @Override
    public String getName() { return "N+1 Query Fix"; }

    @Override
    public String getDescription() {
        return "Demonstrates how lazy-loading triggers one SQL query per author (N+1), " +
               "vs a single JOIN FETCH that loads everything at once.";
    }

    @Override
    @Transactional
    public MetricsSnapshot runBaseline() {
        // BASELINE: findAll() charge les auteurs, puis Hibernate exécute
        // une requête SQL supplémentaire PAR auteur pour charger ses livres → N+1
        try {
            AtomicLong queryCount = new AtomicLong(0);
            long start = System.currentTimeMillis();

            List<Author> authors = authorRepository.findAll();
            // Forcer le chargement lazy de chaque collection de books
            authors.forEach(author -> {
                int size = author.getBooks().size(); // déclenche la requête lazy
                queryCount.incrementAndGet();        // 1 requête par auteur
            });
            queryCount.incrementAndGet(); // + la requête initiale findAll

            long elapsed = System.currentTimeMillis() - start;
            log.debug("[n-plus-1] baseline: {} authors, ~{} SQL queries, {}ms",
                    authors.size(), queryCount.get(), elapsed);

            return metricsCollector.snapshot(elapsed, queryCount.get(), elapsed);

        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    @Transactional
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: JOIN FETCH charge auteurs + livres en une seule requête SQL
        try {
            long start = System.currentTimeMillis();

            List<Author> authors = authorRepository.findAllWithBooks();
            // Les books sont déjà en mémoire — aucune requête supplémentaire
            authors.forEach(author -> author.getBooks().size());

            long elapsed = System.currentTimeMillis() - start;
            log.debug("[n-plus-1] optimized: {} authors, 1 SQL query, {}ms",
                    authors.size(), elapsed);

            return metricsCollector.snapshot(elapsed, 1, elapsed);

        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }
}

