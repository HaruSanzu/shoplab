package com.shoplab.tests.api.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * JSON bodies of the order endpoints.
 * Delivery and payment methods are plain Strings (not enums) so negative tests can send
 * values the API does not know, like "ROCKET" or "BITCOIN".
 */
public final class OrderModels {

    private OrderModels() {
    }

    public record Address(String fullName, String address, String city, String zip, String country, String phone) {

        /** Records are immutable. "with" methods return a copy with one field changed. */
        public Address withFullName(String value) {
            return new Address(value, address, city, zip, country, phone);
        }

        public Address withZip(String value) {
            return new Address(fullName, address, city, value, country, phone);
        }

        public Address withPhone(String value) {
            return new Address(fullName, address, city, zip, country, value);
        }
    }

    public record Card(String number, String expiry, String cvv) {
    }

    /** Body of POST /api/orders. The products are taken from the user's cart, not from here. */
    public record OrderRequest(String deliveryMethod, String paymentMethod, Address shippingAddress, Card card) {

        public OrderRequest withDeliveryMethod(String value) {
            return new OrderRequest(value, paymentMethod, shippingAddress, card);
        }

        public OrderRequest withPaymentMethod(String value) {
            return new OrderRequest(deliveryMethod, value, shippingAddress, card);
        }

        public OrderRequest withAddress(Address value) {
            return new OrderRequest(deliveryMethod, paymentMethod, value, card);
        }

        public OrderRequest withCard(Card value) {
            return new OrderRequest(deliveryMethod, paymentMethod, shippingAddress, value);
        }
    }

    public record OrderItem(Long productId, String title, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record Order(
            String orderNumber,
            String status,
            String createdAt,
            String deliveryMethod,
            String paymentMethod,
            String cardLast4,
            String promoCode,
            List<OrderItem> items,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shipping,
            BigDecimal total,
            Address shippingAddress
    ) {
    }
}
