package com.sentori.spring_perf_lab.scenarios.nplus1;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    // BASELINE: chargement lazy — déclenchera N requêtes supplémentaires pour les books
    // (méthode findAll() héritée de JpaRepository)

    // OPTIMIZED: JOIN FETCH charge auteurs + livres en une seule requête
    @Query("SELECT DISTINCT a FROM Author a JOIN FETCH a.books")
    List<Author> findAllWithBooks();
}

