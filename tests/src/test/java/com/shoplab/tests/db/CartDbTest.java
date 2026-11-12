package com.shoplab.tests.db;

import com.shoplab.tests.api.client.CartClient;
import com.shoplab.tests.api.model.CartModels.AddItemRequest;
import com.shoplab.tests.api.model.CartModels.PreviewRequest;
import com.shoplab.tests.data.TestData.Products;
import com.shoplab.tests.data.TestUsers;
import com.shoplab.tests.data.TestUsers.LoggedInUser;
import com.shoplab.tests.db.DbRows.CartItemRow;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Cart in the database")
@DisplayName("Cart (DB)")
class CartDbTest extends BaseDbTest {

    private LoggedInUser user;
    private CartClient cart;

    @BeforeEach
    void newUser() {
        user = TestUsers.registerNew();
        cart = new CartClient(user.token());
    }

    @Test
    @Tag("smoke")
    @DisplayName("Adding a product stores one cart_items row with the quantity")
    void addItemStoresRow() {
        cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);

        assertThat(ShopDb.cartItems(user.email()))
                .containsExactly(new CartItemRow(Products.SELENIUM_BOOK, 2));
        // The cart row belongs to this user
        assertThat(ShopDb.findCart(user.email())).get()
                .extracting(DbRows.CartRow::userId)
                .isEqualTo(ShopDb.user(user.email()).id());
    }

    @Test
    @DisplayName("Adding the same product twice updates the row, it doesn't add a second one")
    void addSameProductUpdatesRow() {
        cart.addItem(Products.JAVA_BOOK, 2).then().statusCode(200);
        cart.addItem(Products.JAVA_BOOK, 3).then().statusCode(200);

        assertThat(ShopDb.cartItems(user.email()))
                .containsExactly(new CartItemRow(Products.JAVA_BOOK, 5));
    }

    @Test
    @DisplayName("Changing the quantity updates the row")
    void updateQuantityUpdatesRow() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        cart.updateQuantity(Products.SELENIUM_BOOK, 4).then().statusCode(200);

        assertThat(ShopDb.cartItems(user.email()))
                .containsExactly(new CartItemRow(Products.SELENIUM_BOOK, 4));
    }

    @Test
    @DisplayName("Removing a product deletes only its row")
    void removeItemDeletesRow() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        cart.addItem(Products.JAVA_BOOK, 1).then().statusCode(200);

        cart.removeItem(Products.SELENIUM_BOOK).then().statusCode(200);

        assertThat(ShopDb.cartItems(user.email()))
                .extracting(CartItemRow::productId)
                .containsExactly(Products.JAVA_BOOK);
    }

    @Test
    @DisplayName("Emptying the cart deletes all rows and the promo code")
    void clearDeletesRowsAndPromo() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        cart.addItem(Products.JAVA_BOOK, 1).then().statusCode(200);
        cart.applyPromo("QA10").then().statusCode(200);

        cart.clear().then().statusCode(200);

        assertThat(ShopDb.cartItems(user.email())).isEmpty();
        assertThat(ShopDb.findCart(user.email())).get().extracting(DbRows.CartRow::promoCode).isNull();
    }

    @Test
    @DisplayName("Promo code is stored normalized (trimmed, upper case)")
    void promoStoredNormalized() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        cart.applyPromo("  qa10 ").then().statusCode(200);

        assertThat(ShopDb.findCart(user.email())).get().extracting(DbRows.CartRow::promoCode).isEqualTo("QA10");
    }

    @Test
    @DisplayName("Removing the promo code sets promo_code to NULL")
    void removePromoClearsColumn() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        cart.applyPromo("FREESHIP").then().statusCode(200);

        cart.removePromo().then().statusCode(200);

        assertThat(ShopDb.findCart(user.email())).get().extracting(DbRows.CartRow::promoCode).isNull();
    }

    @Test
    @DisplayName("Rejected requests (out of stock, too many, bad promo) change nothing")
    void rejectedRequestsChangeNothing() {
        cart.addItem(Products.LOW_STOCK, 1).then().statusCode(200);
        List<CartItemRow> before = ShopDb.cartItems(user.email());

        cart.addItem(Products.OUT_OF_STOCK, 1).then().statusCode(409);
        cart.addItem(Products.LOW_STOCK, 5).then().statusCode(409);
        cart.applyPromo("NOPE").then().statusCode(400);

        assertThat(ShopDb.cartItems(user.email())).isEqualTo(before);
        assertThat(ShopDb.findCart(user.email())).get().extracting(DbRows.CartRow::promoCode).isNull();
    }

    @Test
    @DisplayName("Adding to the cart does not reserve stock (stock changes only when ordering)")
    void addToCartDoesNotChangeStock() {
        int stockBefore = ShopDb.stockOf(Products.COFFEE_MAKER);

        cart.addItem(Products.COFFEE_MAKER, 3).then().statusCode(200);

        assertThat(ShopDb.stockOf(Products.COFFEE_MAKER)).isEqualTo(stockBefore);
    }

    @Test
    @DisplayName("Guest cart preview saves nothing in the database")
    void previewSavesNothing() {
        long cartsBefore = ShopDb.countAllCarts();
        long itemsBefore = ShopDb.countAllCartItems();

        CartClient.preview(new PreviewRequest(List.of(new AddItemRequest(Products.SELENIUM_BOOK, 2)), "QA10", null))
                .then().statusCode(200);

        assertThat(ShopDb.countAllCarts()).isEqualTo(cartsBefore);
        assertThat(ShopDb.countAllCartItems()).isEqualTo(itemsBefore);
    }
}
