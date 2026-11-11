package com.shoplab.product;

import java.math.BigDecimal;

/** All search/filter parameters of GET /api/products, grouped in one object. */
public record ProductSearch(
        String q,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        Boolean inStock,
        String sort,
        int page,
        int size
) {
}
