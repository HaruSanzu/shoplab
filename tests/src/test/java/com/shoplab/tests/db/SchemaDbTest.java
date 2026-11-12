package com.shoplab.tests.db;

import com.shoplab.tests.api.client.CartClient;
import com.shoplab.tests.api.client.OrdersClient;
import com.shoplab.tests.api.model.OrderModels.Order;
import com.shoplab.tests.data.TestData;
import com.shoplab.tests.data.TestData.Products;
import com.shoplab.tests.data.TestData.Users;
import com.shoplab.tests.data.TestUsers;
import com.shoplab.tests.data.TestUsers.LoggedInUser;
import com.shoplab.tests.db.DbRows.MigrationRow;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Checks of the database ITSELF: migrations, seed data and constraints.
 * <p>
 * Constraints (CHECK, UNIQUE, FOREIGN KEY) are the last line of defence: even if the Java code
 * had a bug, the database refuses impossible data such as a negative stock.
 * These tests try to write bad data directly with SQL and expect the database to say no.
 * The bad data never gets in, so there is nothing to clean up.
 */
@Feature("Schema and constraints")
@DisplayName("Schema, seed data and constraints (DB)")
class SchemaDbTest extends BaseDbTest {

    // ---------- migrations and seed data ----------

    @Test
    @DisplayName("Flyway ran both migrations successfully")
    void migrationsApplied() {
        assertThat(ShopDb.migrations())
                .extracting(MigrationRow::version, MigrationRow::success)
                .containsExactly(
                        tuple("1", true),
                        tuple("2", true));
    }

    @Test
    @DisplayName("After a reset the database has the original seed data")
    void seedData() {
        assertThat(ShopDb.countProducts()).isEqualTo(Products.TOTAL);
        assertThat(ShopDb.countUsers()).isGreaterThanOrEqualTo(3);
        assertThat(ShopDb.findUser(Users.STANDARD)).isPresent();
        assertThat(ShopDb.stockOf(Products.OUT_OF_STOCK)).isZero();
        assertThat(ShopDb.stockOf(Products.LOW_STOCK)).isEqualTo(2);
        assertThat(ShopDb.priceOf(Products.SELENIUM_BOOK)).isEqualByComparingTo("39.99");
    }

    @Test
    @DisplayName("Every product has a unique SKU")
    void skusAreUnique() {
        long distinctSkus = Database.count("SELECT count(DISTINCT sku) FROM products");

        assertThat(distinctSkus).isEqualTo(ShopDb.countProducts());
    }

    // ---------- constraints ----------

    @Test
    @DisplayName("CHECK: stock can never be negative")
    void stockCannotBeNegative() {
        assertThatThrownBy(() -> Database.update("UPDATE products SET stock = -1 WHERE id = ?", Products.SELENIUM_BOOK))
                .isInstanceOf(DbException.class)
                .hasFieldOrPropertyWithValue("sqlState", DbException.CHECK_VIOLATION)
                .hasMessageContaining("products_stock_check");

        assertThat(ShopDb.stockOf(Products.SELENIUM_BOOK)).isPositive();
    }

    @Test
    @DisplayName("CHECK: price can never be negative")
    void priceCannotBeNegative() {
        assertThatThrownBy(() -> Database.update("UPDATE products SET price = -0.01 WHERE id = ?", Products.SELENIUM_BOOK))
                .isInstanceOf(DbException.class)
                .hasFieldOrPropertyWithValue("sqlState", DbException.CHECK_VIOLATION);
    }

    @Test
    @DisplayName("CHECK: cart quantity must stay between 1 and 10")
    void cartQuantityLimitedInDatabase() {
        LoggedInUser user = TestUsers.registerNew();
        new CartClient(user.token()).addItem(Products.CHARGER, 10).then().statusCode(200);
        long cartId = ShopDb.findCart(user.email()).orElseThrow().id();

        assertThatThrownBy(() -> Database.update(
                "UPDATE cart_items SET quantity = 11 WHERE cart_id = ?", cartId))
                .isInstanceOf(DbException.class)
                .hasFieldOrPropertyWithValue("sqlState", DbException.CHECK_VIOLATION);
    }

    @Test
    @DisplayName("UNIQUE: the same email in another letter case cannot be inserted")
    void emailUniqueIgnoringCase() {
        assertThatThrownBy(() -> Database.update(
                "INSERT INTO users (email, password_hash, name) VALUES (?, 'x', 'Copy')", Users.STANDARD.toUpperCase()))
                .isInstanceOf(DbException.class)
                .hasFieldOrPropertyWithValue("sqlState", DbException.UNIQUE_VIOLATION)
                .hasMessageContaining("ux_users_email_lower");
    }

    @Test
    @DisplayName("CHECK: order status must be PROCESSING or CANCELLED")
    void orderStatusRestricted() {
        // An UPDATE needs a row to update: with no orders it would change 0 rows and raise no error at all
        LoggedInUser user = TestUsers.registerNew();
        new CartClient(user.token()).addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        String orderNumber = expect(new OrdersClient(user.token()).place(TestData.cardOrder()), 201, Order.class)
                .orderNumber();

        assertThatThrownBy(() -> Database.update("UPDATE orders SET status = 'SHIPPED' WHERE order_number = ?", orderNumber))
                .isInstanceOf(DbException.class)
                .hasFieldOrPropertyWithValue("sqlState", DbException.CHECK_VIOLATION);
    }

    @Test
    @DisplayName("FOREIGN KEY: a cart line cannot point to a product that does not exist")
    void cartItemNeedsExistingProduct() {
        LoggedInUser user = TestUsers.registerNew();
        new CartClient(user.token()).get().then().statusCode(200); // GET creates the empty cart row
        long cartId = ShopDb.findCart(user.email()).orElseThrow().id();

        assertThatThrownBy(() -> Database.update(
                "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, 1)", cartId, Products.NOT_EXISTING))
                .isInstanceOf(DbException.class)
                .hasFieldOrPropertyWithValue("sqlState", DbException.FOREIGN_KEY_VIOLATION);
    }
}
