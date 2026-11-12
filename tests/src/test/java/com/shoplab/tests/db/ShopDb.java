package com.shoplab.tests.db;

import com.shoplab.tests.db.DbRows.CartItemRow;
import com.shoplab.tests.db.DbRows.CartRow;
import com.shoplab.tests.db.DbRows.MigrationRow;
import com.shoplab.tests.db.DbRows.OrderItemRow;
import com.shoplab.tests.db.DbRows.OrderRow;
import com.shoplab.tests.db.DbRows.ShippingRow;
import com.shoplab.tests.db.DbRows.UserRow;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * All SQL the tests need, in one place, with readable method names.
 * It is the database version of the API clients: tests call {@code ShopDb.stockOf(7)}
 * instead of writing SQL. If a column is renamed, only this class changes.
 * <p>
 * The queries are READ-ONLY on purpose: tests change data through the API (like a real user)
 * and only LOOK into the database to check the result.
 */
public final class ShopDb {

    private ShopDb() {
    }

    // ---------- users and tokens ----------

    /** lower(email) = lower(?): emails are unique ignoring case, exactly like the unique index. */
    public static Optional<UserRow> findUser(String email) {
        return Database.queryOne("""
                SELECT id, email, password_hash, name, locked, slow_login, created_at
                FROM users
                WHERE lower(email) = lower(?)
                """, ShopDb::toUser, email);
    }

    public static UserRow user(String email) {
        return findUser(email).orElseThrow(() -> new AssertionError("No user with email " + email + " in table users"));
    }

    public static long countUsersWithEmail(String email) {
        return Database.count("SELECT count(*) FROM users WHERE lower(email) = lower(?)", email);
    }

    public static long countUsers() {
        return Database.count("SELECT count(*) FROM users");
    }

    public static boolean tokenExists(String token) {
        return Database.count("SELECT count(*) FROM auth_tokens WHERE token = ?", token) == 1;
    }

    public static long countTokensOf(String email) {
        return Database.count("""
                SELECT count(*)
                FROM auth_tokens t
                JOIN users u ON u.id = t.user_id
                WHERE lower(u.email) = lower(?)
                """, email);
    }

    // ---------- cart ----------

    public static Optional<CartRow> findCart(String email) {
        return Database.queryOne("""
                SELECT c.id, c.user_id, c.promo_code
                FROM carts c
                JOIN users u ON u.id = c.user_id
                WHERE lower(u.email) = lower(?)
                """, rs -> new CartRow(rs.getLong("id"), rs.getLong("user_id"), rs.getString("promo_code")), email);
    }

    public static List<CartItemRow> cartItems(String email) {
        return Database.query("""
                SELECT ci.product_id, ci.quantity
                FROM cart_items ci
                JOIN carts c ON c.id = ci.cart_id
                JOIN users u ON u.id = c.user_id
                WHERE lower(u.email) = lower(?)
                ORDER BY ci.product_id
                """, rs -> new CartItemRow(rs.getLong("product_id"), rs.getInt("quantity")), email);
    }

    public static long countAllCartItems() {
        return Database.count("SELECT count(*) FROM cart_items");
    }

    public static long countAllCarts() {
        return Database.count("SELECT count(*) FROM carts");
    }

    // ---------- products ----------

    public static int stockOf(long productId) {
        return Database.queryOne("SELECT stock FROM products WHERE id = ?", rs -> rs.getInt("stock"), productId)
                .orElseThrow(() -> new AssertionError("No product with id " + productId));
    }

    public static BigDecimal priceOf(long productId) {
        return Database.queryOne("SELECT price FROM products WHERE id = ?", rs -> rs.getBigDecimal("price"), productId)
                .orElseThrow(() -> new AssertionError("No product with id " + productId));
    }

    public static long countProducts() {
        return Database.count("SELECT count(*) FROM products");
    }

    // ---------- orders ----------

    public static Optional<OrderRow> findOrder(String orderNumber) {
        return Database.queryOne("""
                SELECT id, order_number, user_id, status, created_at, delivery_method, payment_method,
                       card_last4, promo_code, subtotal, discount, shipping_cost, total
                FROM orders
                WHERE order_number = ?
                """, ShopDb::toOrder, orderNumber);
    }

    public static OrderRow order(String orderNumber) {
        return findOrder(orderNumber).orElseThrow(() -> new AssertionError("No order " + orderNumber + " in table orders"));
    }

    public static ShippingRow shippingOf(String orderNumber) {
        return Database.queryOne("""
                SELECT ship_full_name, ship_address, ship_city, ship_zip, ship_country, ship_phone
                FROM orders
                WHERE order_number = ?
                """, rs -> new ShippingRow(rs.getString("ship_full_name"), rs.getString("ship_address"),
                rs.getString("ship_city"), rs.getString("ship_zip"), rs.getString("ship_country"),
                rs.getString("ship_phone")), orderNumber)
                .orElseThrow(() -> new AssertionError("No order " + orderNumber));
    }

    public static List<OrderItemRow> orderItems(String orderNumber) {
        return Database.query("""
                SELECT oi.product_id, oi.product_title, oi.unit_price, oi.quantity, oi.line_total
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.order_number = ?
                ORDER BY oi.id
                """, rs -> new OrderItemRow(rs.getLong("product_id"), rs.getString("product_title"),
                rs.getBigDecimal("unit_price"), rs.getInt("quantity"), rs.getBigDecimal("line_total")), orderNumber);
    }

    public static long countOrdersOf(String email) {
        return Database.count("""
                SELECT count(*)
                FROM orders o
                JOIN users u ON u.id = o.user_id
                WHERE lower(u.email) = lower(?)
                """, email);
    }

    // ---------- schema ----------

    public static List<MigrationRow> migrations() {
        return Database.query("""
                SELECT version, description, success
                FROM flyway_schema_history
                WHERE version IS NOT NULL
                ORDER BY installed_rank
                """, rs -> new MigrationRow(rs.getString("version"), rs.getString("description"), rs.getBoolean("success")));
    }

    // ---------- row mappers ----------

    private static UserRow toUser(ResultSet rs) throws SQLException {
        return new UserRow(rs.getLong("id"), rs.getString("email"), rs.getString("password_hash"),
                rs.getString("name"), rs.getBoolean("locked"), rs.getBoolean("slow_login"),
                instant(rs, "created_at"));
    }

    private static OrderRow toOrder(ResultSet rs) throws SQLException {
        return new OrderRow(rs.getLong("id"), rs.getString("order_number"), rs.getLong("user_id"),
                rs.getString("status"), instant(rs, "created_at"), rs.getString("delivery_method"),
                rs.getString("payment_method"), rs.getString("card_last4"), rs.getString("promo_code"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("shipping_cost"),
                rs.getBigDecimal("total"));
    }

    /** TIMESTAMPTZ columns are read as OffsetDateTime (a moment + time zone) and turned into an Instant. */
    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
