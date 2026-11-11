package com.shoplab.product;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Small reusable WHERE conditions. The service combines only the ones the user asked for,
 * e.g. "category = Books AND price <= 30".
 */
final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Text appears in title, brand or category (case-insensitive), like the frontend search. */
    static Specification<Product> matchesText(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("brand")), pattern),
                cb.like(cb.lower(root.get("category")), pattern));
    }

    static Specification<Product> inCategory(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    static Specification<Product> ratingAtLeast(BigDecimal rating) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), rating);
    }

    static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
    }
}
