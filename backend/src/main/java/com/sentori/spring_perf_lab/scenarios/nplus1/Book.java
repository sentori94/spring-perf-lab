package com.sentori.spring_perf_lab.scenarios.nplus1;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    protected Book() {}

    public Book(String title, Author author) {
        this.title  = title;
        this.author = author;
    }

    public Long getId()      { return id; }
    public String getTitle() { return title; }
    public Author getAuthor(){ return author; }
}

