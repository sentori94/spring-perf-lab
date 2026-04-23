package com.sentori.spring_perf_lab.scenarios.nplus1;

import com.sentori.spring_perf_lab.metrics.MetricsCollector;
import com.sentori.spring_perf_lab.metrics.MetricsSnapshot;
import com.sentori.spring_perf_lab.metrics.RunStart;
import com.sentori.spring_perf_lab.scenarios.PerfScenario;
import com.sentori.spring_perf_lab.scenarios.ScenarioExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // BASELINE: findAll() + N requêtes lazy
        try {
            RunStart runStart = metricsCollector.start();

            List<Author> authors = authorRepository.findAll();
            authors.forEach(author -> author.getBooks().size());

            long queryCount = authors.size() + 1L;
            log.debug("[n-plus-1] baseline: {} authors, ~{} SQL queries", authors.size(), queryCount);

            return metricsCollector.snapshot(runStart, queryCount);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Baseline run failed", e);
        }
    }

    @Override
    @Transactional
    public MetricsSnapshot runOptimized() {
        // OPTIMIZED: JOIN FETCH — une seule requête SQL
        try {
            RunStart runStart = metricsCollector.start();

            List<Author> authors = authorRepository.findAllWithBooks();
            authors.forEach(author -> author.getBooks().size());

            log.debug("[n-plus-1] optimized: {} authors, 1 SQL query", authors.size());

            return metricsCollector.snapshot(runStart, 1);
        } catch (Exception e) {
            throw new ScenarioExecutionException(getId(), "Optimized run failed", e);
        }
    }

    @Override
    public String getBaselineCode() {
        return """
                // BASELINE — findAll() charge les auteurs sans leurs livres
                List<Author> authors = authorRepository.findAll();

                // Pour chaque auteur, Hibernate exécute UNE requête SQL supplémentaire
                // afin de charger la collection books (lazy loading)
                // → 1 requête initiale + N requêtes = N+1 au total
                authors.forEach(author -> {
                    int size = author.getBooks().size(); // déclenche SELECT * FROM books WHERE author_id = ?
                });
                """;
    }

    @Override
    public String getOptimizedCode() {
        return """
                // OPTIMIZED — JOIN FETCH charge auteurs ET livres en une seule requête SQL
                // @Query("SELECT DISTINCT a FROM Author a JOIN FETCH a.books")
                List<Author> authors = authorRepository.findAllWithBooks();

                // Les books sont déjà en mémoire — aucune requête supplémentaire déclenchée
                authors.forEach(author -> {
                    int size = author.getBooks().size(); // aucun SELECT supplémentaire
                });
                """;
    }

    @Override
    public String getWhyExplanation() {
        return """
                Hibernate utilise le chargement "lazy" par défaut pour les relations @OneToMany.
                Cela signifie que les livres d'un auteur ne sont chargés en base qu'au moment
                où on y accède dans le code Java.

                Si on charge 50 auteurs puis qu'on accède aux livres de chacun, Hibernate génère :
                  1 requête → SELECT * FROM authors
                  50 requêtes → SELECT * FROM books WHERE author_id = ? (une par auteur)
                = 51 requêtes au total (le problème N+1)

                Avec JOIN FETCH, une seule requête SQL avec une jointure charge tout d'un coup :
                  SELECT DISTINCT a FROM Author a JOIN FETCH a.books
                = 1 seule requête, peu importe le nombre d'auteurs.

                L'impact est exponentiel : plus il y a d'entités, plus l'écart se creuse.
                """;
    }
}
