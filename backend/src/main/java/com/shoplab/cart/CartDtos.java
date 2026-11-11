package com.shoplab.cart;

import com.shoplab.pricing.DeliveryMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    /**
     * Body of POST /api/cart/preview: "calculate a cart for me" without saving anything.
     * Used by the website for guests (not logged in), whose cart lives in the browser.
     */
    public record PreviewRequest(
            @NotNull(message = "Items are required")
            @Size(max = 50, message = "Too many items")
            List<@Valid PreviewItem> items,

            String promoCode,

            DeliveryMethod deliveryMethod
    ) {
    }

    public record PreviewItem(
            @NotNull(message = "Product id is required")
            Long productId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be between 1 and 10")
            @Max(value = 10, message = "Quantity must be between 1 and 10")
            Integer quantity
    ) {
    }

    public record CartItemDto(
            Long productId,
            String title,
            String emoji,
            String color,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal,
            int stock
    ) {
    }

    /**
     * Totals are for the requested delivery method (STANDARD by default).
     * amountToFreeShipping: how much more to spend for free standard shipping (0 = already free).
     */
    public record CartDto(
            List<CartItemDto> items,
            int itemCount,
            String promoCode,
            String promoDescription,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shipping,
            BigDecimal total,
            BigDecimal amountToFreeShipping
    ) {
    }
}
