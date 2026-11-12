package com.shoplab.tests.db;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One record per table row we read. Field names follow the COLUMNS (password_hash → passwordHash),
 * not the JSON of the API: these classes describe what is really stored in the database.
 */
public final class DbRows {

    private DbRows() {
    }

    /** Table users. */
    public record UserRow(long id, String email, String passwordHash, String name,
                          boolean locked, boolean slowLogin, Instant createdAt) {
    }

    /** Table carts. */
    public record CartRow(long id, long userId, String promoCode) {
    }

    /** Table cart_items. */
    public record CartItemRow(long productId, int quantity) {
    }

    /** Table orders (all columns except the shipping address). */
    public record OrderRow(long id, String orderNumber, long userId, String status, Instant createdAt,
                           String deliveryMethod, String paymentMethod, String cardLast4, String promoCode,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal shippingCost, BigDecimal total) {
    }

    /** The ship_* columns of table orders. */
    public record ShippingRow(String fullName, String address, String city, String zip, String country, String phone) {
    }

    /** Table order_items. Title and price are copies made at purchase time. */
    public record OrderItemRow(long productId, String productTitle, BigDecimal unitPrice, int quantity,
                               BigDecimal lineTotal) {
    }

    /** Table flyway_schema_history: one row per migration Flyway has run. */
    public record MigrationRow(String version, String description, boolean success) {
    }
}
