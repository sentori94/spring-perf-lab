package com.sentori.spring_perf_lab.scenarios.nplus1;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Book> books = new ArrayList<>();

    protected Author() {}

    public Author(String name) {
        this.name = name;
    }

    public Long getId()          { return id; }
    public String getName()      { return name; }
    public List<Book> getBooks() { return books; }
}

