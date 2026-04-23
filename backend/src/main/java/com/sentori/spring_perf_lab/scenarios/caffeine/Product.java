package com.sentori.spring_perf_lab.scenarios.caffeine;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;
    private double price;

    protected Product() {}

    public Product(String name, String category, double price) {
        this.name     = name;
        this.category = category;
        this.price    = price;
    }

    public Long getId()        { return id; }
    public String getName()    { return name; }
    public String getCategory(){ return category; }
    public double getPrice()   { return price; }
}

