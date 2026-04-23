package com.sentori.spring_perf_lab.scenarios.caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the database with test data for the Caffeine cache scenario.
 * Creates 200 products across 5 categories.
 */
@Component
public class CaffeineDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CaffeineDataInitializer.class);

    private static final String[] CATEGORIES = {"electronics", "books", "clothing", "food", "sports"};
    private static final int PRODUCTS_PER_CATEGORY = 40;

    private final ProductRepository productRepository;

    public CaffeineDataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            log.info("Caffeine test data already present, skipping initialization.");
            return;
        }

        log.info("Initializing Caffeine test data: {} categories x {} products...",
                CATEGORIES.length, PRODUCTS_PER_CATEGORY);

        for (String category : CATEGORIES) {
            for (int i = 1; i <= PRODUCTS_PER_CATEGORY; i++) {
                productRepository.save(new Product(
                        category + "-product-" + i,
                        category,
                        10.0 + i
                ));
            }
        }

        log.info("Caffeine test data ready: {} products total.",
                CATEGORIES.length * PRODUCTS_PER_CATEGORY);
    }
}

