package com.shoplab.tests.api.model;

import java.math.BigDecimal;
import java.util.List;

/** JSON bodies of the cart endpoints. */
public final class CartModels {

    private CartModels() {
    }

    /** Integer / Long (not int / long) so a test can send null to check "is required" errors. */
    public record AddItemRequest(Long productId, Integer quantity) {
    }

    public record QuantityRequest(Integer quantity) {
    }

    public record PromoRequest(String code) {
    }

    /** Body of POST /api/cart/preview: a guest's cart (from the browser) to be priced by the server. */
    public record PreviewRequest(List<AddItemRequest> items, String promoCode, String deliveryMethod) {
    }

    public record CartItem(
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

    public record Cart(
            List<CartItem> items,
            int itemCount,
            String promoCode,
            String promoDescription,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shipping,
            BigDecimal total,
            BigDecimal amountToFreeShipping
    ) {
        /** Finds the line of one product, or fails the test with a clear message. */
        public CartItem item(long productId) {
            return items.stream()
                    .filter(i -> i.productId() == productId)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Product " + productId + " is not in the cart: " + items));
        }
    }
}
