package com.shoplab.tests.api.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * JSON bodies of the catalog endpoints.
 * Money and ratings are {@link BigDecimal}, never double: 0.1 + 0.2 is 0.30000000000000004 in double.
 */
public final class ProductModels {

    private ProductModels() {
    }

    public record Product(
            Long id,
            String sku,
            String title,
            String brand,
            String category,
            String description,
            BigDecimal price,
            BigDecimal rating,
            int reviewsCount,
            int stock,
            boolean inStock,
            String emoji,
            String color
    ) {
    }

    /** One page of search results. Pages start at 1. */
    public record ProductPage(
            List<Product> items,
            int page,
            int size,
            long totalItems,
            int totalPages
    ) {
    }
}
