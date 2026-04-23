package com.sentori.spring_perf_lab.scenarios.caffeine;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service exposant les produits par catégorie.
 * La méthode getByCategory() existe en deux versions :
 * - sans cache : frappe la BDD à chaque appel
 * - avec cache  : Caffeine sert la réponse depuis la mémoire dès le 2ème appel
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // BASELINE — aucun cache, chaque appel génère une requête SQL
    public List<Product> getByCategoryNoCache(String category) {
        return productRepository.findByCategory(category);
    }

    // OPTIMIZED — Caffeine met le résultat en cache après le 1er appel
    @Cacheable(value = "productsByCategory", key = "#category")
    public List<Product> getByCategoryWithCache(String category) {
        return productRepository.findByCategory(category);
    }

    // Permet de vider le cache entre les runs pour garantir des mesures propres
    @CacheEvict(value = "productsByCategory", allEntries = true)
    public void evictCache() {}
}

