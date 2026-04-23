package com.sentori.spring_perf_lab.scenarios.nplus1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the database with test data for the N+1 scenario.
 * Creates 50 authors with 20 books each = 1 000 books total.
 */
@Component
public class NPlus1DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NPlus1DataInitializer.class);

    private static final int AUTHOR_COUNT = 50;
    private static final int BOOKS_PER_AUTHOR = 20;

    private final AuthorRepository authorRepository;

    public NPlus1DataInitializer(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (authorRepository.count() > 0) {
            log.info("N+1 test data already present, skipping initialization.");
            return;
        }

        log.info("Initializing N+1 test data: {} authors x {} books...", AUTHOR_COUNT, BOOKS_PER_AUTHOR);

        for (int i = 1; i <= AUTHOR_COUNT; i++) {
            Author author = new Author("Author " + i);
            for (int j = 1; j <= BOOKS_PER_AUTHOR; j++) {
                author.getBooks().add(new Book("Book " + j + " by Author " + i, author));
            }
            authorRepository.save(author);
        }

        log.info("N+1 test data ready: {} authors, {} books total.",
                AUTHOR_COUNT, AUTHOR_COUNT * BOOKS_PER_AUTHOR);
    }
}

