package com.shoplab.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(
            @NotNull(message = "Product id is required")
            Long productId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be between 1 and 10")
            @Max(value = 10, message = "Quantity must be between 1 and 10")
            Integer quantity
    ) {
    }

    public record UpdateQuantityRequest(
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be between 1 and 10")
            @Max(value = 10, message = "Quantity must be between 1 and 10")
            Integer quantity
    ) {
    }

    public record PromoRequest(String code) {
    }

    public record CartItemDto(
            Long productId,
            String title,
            String emoji,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal,
            int stock
    ) {
    }

    /** Totals are for STANDARD delivery; checkout recalculates them for the chosen delivery. */
    public record CartDto(
            List<CartItemDto> items,
            int itemCount,
            String promoCode,
            String promoDescription,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shipping,
            BigDecimal total
    ) {
    }
}
