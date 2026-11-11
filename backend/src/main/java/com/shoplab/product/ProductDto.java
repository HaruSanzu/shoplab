package com.shoplab.product;

import java.math.BigDecimal;

/**
 * DTO = "Data Transfer Object": exactly what the API sends back as JSON.
 * We never return entities directly, so the database structure and the API contract
 * can change independently.
 */
public record ProductDto(
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
    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getSku(), p.getTitle(), p.getBrand(), p.getCategory(),
                p.getDescription(), p.getPrice(), p.getRating(), p.getReviewsCount(), p.getStock(),
                p.getStock() > 0, p.getEmoji(), p.getColor());
    }
}
