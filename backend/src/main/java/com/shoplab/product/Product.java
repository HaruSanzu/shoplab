package com.shoplab.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * An ENTITY: one object of this class = one row of the "products" table.
 * Hibernate reads and writes rows for us, so we don't write SQL by hand.
 * Money uses BigDecimal, never double: 0.1 + 0.2 is not exactly 0.3 with double.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private String title;
    private String brand;
    private String category;
    private String description;
    private BigDecimal price;
    private BigDecimal rating;

    @Column(name = "reviews_count")
    private int reviewsCount;

    private int stock;
    private String emoji;
    private String color;

    protected Product() {
        // required by JPA
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getTitle() { return title; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getRating() { return rating; }
    public int getReviewsCount() { return reviewsCount; }
    public int getStock() { return stock; }
    public String getEmoji() { return emoji; }
    public String getColor() { return color; }

    public void decreaseStock(int quantity) {
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }
}
