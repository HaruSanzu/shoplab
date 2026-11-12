package com.shoplab.tests.api;

import com.shoplab.tests.api.client.CartClient;
import com.shoplab.tests.api.model.CartModels.AddItemRequest;
import com.shoplab.tests.api.model.CartModels.Cart;
import com.shoplab.tests.api.model.CartModels.CartItem;
import com.shoplab.tests.api.model.CartModels.PreviewRequest;
import com.shoplab.tests.api.model.ErrorResponse;
import com.shoplab.tests.data.TestData.Products;
import com.shoplab.tests.data.TestUsers;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Cart")
@DisplayName("Cart API")
class CartApiTest extends BaseApiTest {

    /** The cart of a brand-new user, created before EACH test (see TestUsers for why). */
    private CartClient cart;

    /** @BeforeEach = runs before every single test of this class. */
    @BeforeEach
    void newUserWithEmptyCart() {
        cart = new CartClient(TestUsers.registerNew().token());
    }

    // ---------- basic operations ----------

    @Test
    @DisplayName("New user has an empty cart with zero totals")
    void newCartIsEmpty() {
        Cart body = expect(cart.get(), 200, Cart.class);

        assertThat(body.items()).isEmpty();
        assertThat(body.itemCount()).isZero();
        assertThat(body.promoCode()).isNull();
        assertThat(body.subtotal()).isEqualByComparingTo("0");
        assertThat(body.shipping()).isEqualByComparingTo("0");
        assertThat(body.total()).isEqualByComparingTo("0");
    }

    @Test
    @Tag("smoke")
    @DisplayName("Adding a product returns the updated cart with correct totals")
    void addItem() {
        Cart body = expect(cart.addItem(Products.SELENIUM_BOOK, 2), 200, Cart.class);

        CartItem item = body.item(Products.SELENIUM_BOOK);
        assertThat(item.title()).isEqualTo("Selenium in Practice");
        assertThat(item.unitPrice()).isEqualByComparingTo("39.99");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.lineTotal()).isEqualByComparingTo("79.98");

        assertThat(body.itemCount()).isEqualTo(2);
        assertThat(body.subtotal()).isEqualByComparingTo("79.98");
        assertThat(body.shipping()).as("free shipping from $50").isEqualByComparingTo("0");
        assertThat(body.total()).isEqualByComparingTo("79.98");
    }

    @Test
    @DisplayName("Cart is saved on the server: GET returns what was added")
    void cartIsPersisted() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        Cart body = expect(cart.get(), 200, Cart.class);

        assertThat(body.items()).extracting(CartItem::productId).containsExactly(Products.SELENIUM_BOOK);
    }

    @Test
    @DisplayName("Adding the same product again increases its quantity (no second line)")
    void addSameProductTwice() {
        cart.addItem(Products.JAVA_BOOK, 2).then().statusCode(200);

        Cart body = expect(cart.addItem(Products.JAVA_BOOK, 3), 200, Cart.class);

        assertThat(body.items()).hasSize(1);
        assertThat(body.item(Products.JAVA_BOOK).quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Item count is the sum of quantities of all lines")
    void itemCountSumsQuantities() {
        cart.addItem(Products.JAVA_BOOK, 2).then().statusCode(200);

        Cart body = expect(cart.addItem(Products.GHERKIN_BOOK, 3), 200, Cart.class);

        assertThat(body.items()).hasSize(2);
        assertThat(body.itemCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Changing the quantity replaces it (does not add)")
    void updateQuantity() {
        cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);

        Cart body = expect(cart.updateQuantity(Products.SELENIUM_BOOK, 5), 200, Cart.class);

        assertThat(body.item(Products.SELENIUM_BOOK).quantity()).isEqualTo(5);
        assertThat(body.subtotal()).isEqualByComparingTo("199.95");
    }

    @Test
    @DisplayName("Removing a product removes only that line")
    void removeItem() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        cart.addItem(Products.JAVA_BOOK, 1).then().statusCode(200);

        Cart body = expect(cart.removeItem(Products.SELENIUM_BOOK), 200, Cart.class);

        assertThat(body.items()).extracting(CartItem::productId).containsExactly(Products.JAVA_BOOK);
    }

    @Test
    @DisplayName("Emptying the cart removes all products and the promo code")
    void clearCart() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        cart.applyPromo("QA10").then().statusCode(200);

        Cart body = expect(cart.clear(), 200, Cart.class);

        assertThat(body.items()).isEmpty();
        assertThat(body.promoCode()).isNull();
        assertThat(body.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Each user has their own cart")
    void cartsAreSeparatePerUser() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        CartClient otherUsersCart = new CartClient(TestUsers.registerNew().token());

        Cart other = expect(otherUsersCart.get(), 200, Cart.class);

        assertThat(other.items()).isEmpty();
    }

    // ---------- shipping rules: boundary values around $50 ----------

    /**
     * BOUNDARY VALUE ANALYSIS: bugs love the edges of a rule.
     * The rule is "free standard shipping if subtotal >= $50", so we test just below, exactly at, and above $50.
     * A @Nested class groups related tests: they appear as one folder in the report.
     */
    @Nested
    @DisplayName("Shipping cost")
    class Shipping {

        @Test
        @DisplayName("$49.98 (just below $50): standard shipping costs $5.99")
        void justBelowFreeShippingLimit() {
            Cart body = expect(cart.addItem(Products.JAVA_BOOK, 2), 200, Cart.class); // 2 x 24.99

            assertThat(body.subtotal()).isEqualByComparingTo("49.98");
            assertThat(body.shipping()).isEqualByComparingTo("5.99");
            assertThat(body.total()).isEqualByComparingTo("55.97");
            assertThat(body.amountToFreeShipping()).isEqualByComparingTo("0.02");
        }

        @Test
        @DisplayName("$50.00 (exactly the limit): standard shipping is free")
        void exactlyAtFreeShippingLimit() {
            cart.addItem(Products.WALLET, 1).then().statusCode(200);               // 32.00
            Cart body = expect(cart.addItem(Products.MUG_SET, 1), 200, Cart.class); // + 18.00

            assertThat(body.subtotal()).isEqualByComparingTo("50.00");
            assertThat(body.shipping()).isEqualByComparingTo("0");
            assertThat(body.total()).isEqualByComparingTo("50.00");
            assertThat(body.amountToFreeShipping()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Express delivery always costs $9.99, even above $50")
        void expressShipping() {
            cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);

            Cart body = expect(cart.get("EXPRESS"), 200, Cart.class);

            assertThat(body.shipping()).isEqualByComparingTo("9.99");
            assertThat(body.total()).isEqualByComparingTo("89.97");
        }

        @Test
        @DisplayName("Delivery method is case-insensitive")
        void deliveryMethodCaseInsensitive() {
            cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

            Cart body = expect(cart.get("express"), 200, Cart.class);

            assertThat(body.shipping()).isEqualByComparingTo("9.99");
        }

        @Test
        @DisplayName("Unknown delivery method gives 400")
        void unknownDeliveryMethod() {
            assertThat(expectError(cart.get("ROCKET"), 400).message())
                    .isEqualTo("Unknown delivery method 'ROCKET'. Allowed: STANDARD, EXPRESS");
        }
    }

    // ---------- promo codes ----------

    @Nested
    @DisplayName("Promo codes")
    class PromoCodes {

        @Test
        @Tag("smoke")
        @DisplayName("QA10 gives 10% off the subtotal")
        void qa10() {
            cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);

            Cart body = expect(cart.applyPromo("QA10"), 200, Cart.class);

            assertThat(body.promoCode()).isEqualTo("QA10");
            assertThat(body.promoDescription()).isEqualTo("10% off your order");
            assertThat(body.subtotal()).isEqualByComparingTo("79.98");
            assertThat(body.discount()).isEqualByComparingTo("8.00");
            assertThat(body.total()).isEqualByComparingTo("71.98");
        }

        @Test
        @DisplayName("QA10 can push the order below $50, then shipping is no longer free")
        void qa10CanRemoveFreeShipping() {
            // 54.99 is above $50, but after 10% off (5.50, rounded half-up from 5.499) it is 49.49
            cart.addItem(Products.RC_CAR, 1).then().statusCode(200);

            Cart body = expect(cart.applyPromo("QA10"), 200, Cart.class);

            assertThat(body.discount()).isEqualByComparingTo("5.50");
            assertThat(body.shipping()).isEqualByComparingTo("5.99");
            assertThat(body.total()).isEqualByComparingTo("55.48");
            assertThat(body.amountToFreeShipping()).isEqualByComparingTo("0.51");
        }

        @Test
        @DisplayName("FREESHIP makes standard shipping free below $50")
        void freeShip() {
            cart.addItem(Products.GHERKIN_BOOK, 1).then().statusCode(200); // 19.99

            Cart body = expect(cart.applyPromo("FREESHIP"), 200, Cart.class);

            assertThat(body.promoDescription()).isEqualTo("Free standard shipping");
            assertThat(body.discount()).isEqualByComparingTo("0");
            assertThat(body.shipping()).isEqualByComparingTo("0");
            assertThat(body.total()).isEqualByComparingTo("19.99");
            assertThat(body.amountToFreeShipping()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("FREESHIP does not make express delivery free")
        void freeShipDoesNotCoverExpress() {
            cart.addItem(Products.GHERKIN_BOOK, 1).then().statusCode(200);
            cart.applyPromo("FREESHIP").then().statusCode(200);

            Cart body = expect(cart.get("EXPRESS"), 200, Cart.class);

            assertThat(body.shipping()).isEqualByComparingTo("9.99");
        }

        @ParameterizedTest(name = "\"{0}\" is accepted as QA10")
        @DisplayName("Promo code is trimmed and case-insensitive")
        @ValueSource(strings = {"qa10", "Qa10", "  QA10  "})
        void promoCodeIsNormalized(String code) {
            cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

            Cart body = expect(cart.applyPromo(code), 200, Cart.class);

            assertThat(body.promoCode()).isEqualTo("QA10");
        }

        @Test
        @DisplayName("Unknown promo code gives 400")
        void unknownPromoCode() {
            assertThat(expectError(cart.applyPromo("hello"), 400).message())
                    .isEqualTo("Promo code \"HELLO\" is not valid");
        }

        @ParameterizedTest(name = "code = \"{0}\"")
        @DisplayName("Empty promo code gives 400")
        @ValueSource(strings = {"", "   "})
        void emptyPromoCode(String code) {
            assertThat(expectError(cart.applyPromo(code), 400).message()).isEqualTo("Please enter a promo code");
        }

        @Test
        @DisplayName("Removing the promo code removes the discount")
        void removePromo() {
            cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);
            cart.applyPromo("QA10").then().statusCode(200);

            Cart body = expect(cart.removePromo(), 200, Cart.class);

            assertThat(body.promoCode()).isNull();
            assertThat(body.discount()).isEqualByComparingTo("0");
            assertThat(body.total()).isEqualByComparingTo("79.98");
        }
    }

    // ---------- stock and quantity limits ----------

    @Nested
    @DisplayName("Quantity and stock limits")
    class Limits {

        @Test
        @DisplayName("Out-of-stock product cannot be added (409)")
        void outOfStock() {
            assertThat(expectError(cart.addItem(Products.OUT_OF_STOCK, 1), 409).message())
                    .isEqualTo("This item is out of stock");
        }

        @Test
        @DisplayName("Cannot add more than the stock (409)")
        void moreThanStock() {
            assertThat(expectError(cart.addItem(Products.LOW_STOCK, 3), 409).message())
                    .isEqualTo("Only 2 item(s) available in stock");
        }

        @Test
        @DisplayName("Can add exactly the whole stock")
        void exactlyTheStock() {
            Cart body = expect(cart.addItem(Products.LOW_STOCK, 2), 200, Cart.class);

            assertThat(body.item(Products.LOW_STOCK).quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("10 of one product is allowed, the 11th is not (409)")
        void maxTenPerProduct() {
            cart.addItem(Products.CHARGER, 6).then().statusCode(200);
            cart.addItem(Products.CHARGER, 4).then().statusCode(200); // 10 in total: still fine

            assertThat(expectError(cart.addItem(Products.CHARGER, 1), 409).message())
                    .isEqualTo("You can buy at most 10 of the same item");
        }

        @Test
        @DisplayName("Changing the quantity above the stock gives 409")
        void updateAboveStock() {
            cart.addItem(Products.LOW_STOCK, 1).then().statusCode(200);

            assertThat(expectError(cart.updateQuantity(Products.LOW_STOCK, 3), 409).message())
                    .isEqualTo("Only 2 item(s) available in stock");
        }

        @ParameterizedTest(name = "quantity = {0}")
        @DisplayName("Quantity outside 1..10 gives 400")
        @ValueSource(ints = {0, -1, 11})
        void quantityOutOfRange(int quantity) {
            ErrorResponse error = expectError(cart.addItem(Products.CHARGER, quantity), 400);

            assertThat(error.fieldErrors()).containsEntry("quantity", "Quantity must be between 1 and 10");
        }

        @Test
        @DisplayName("Missing product id and quantity give 400 with both field errors")
        void missingFields() {
            ErrorResponse error = expectError(cart.addItem(new AddItemRequest(null, null)), 400);

            assertThat(error.fieldErrors())
                    .containsEntry("productId", "Product id is required")
                    .containsEntry("quantity", "Quantity is required");
        }

        @Test
        @DisplayName("Not existing product gives 404")
        void notExistingProduct() {
            assertThat(expectError(cart.addItem(Products.NOT_EXISTING, 1), 404).message())
                    .isEqualTo("Product with id 999 not found");
        }

        @Test
        @DisplayName("Changing or removing a product that is not in the cart gives 404")
        void productNotInCart() {
            assertThat(expectError(cart.updateQuantity(Products.SELENIUM_BOOK, 2), 404).message())
                    .isEqualTo("Product 7 is not in the cart");
            assertThat(expectError(cart.removeItem(Products.SELENIUM_BOOK), 404).message())
                    .isEqualTo("Product 7 is not in the cart");
        }
    }

    // ---------- login required ----------

    @Test
    @DisplayName("Cart endpoints require login (401 without a token)")
    void cartRequiresLogin() {
        CartClient guest = new CartClient(null);

        expectError(guest.get(), 401);
        expectError(guest.addItem(Products.SELENIUM_BOOK, 1), 401);
        expectError(guest.clear(), 401);
        expectError(guest.applyPromo("QA10"), 401);
    }

    // ---------- guest cart preview ----------

    @Nested
    @DisplayName("Guest cart preview (no login)")
    class Preview {

        @Test
        @DisplayName("Preview prices a guest cart with the same rules as a saved cart")
        void previewWithPromo() {
            PreviewRequest request = new PreviewRequest(
                    List.of(new AddItemRequest(Products.SELENIUM_BOOK, 2)), "QA10", null);

            Cart body = expect(CartClient.preview(request), 200, Cart.class);

            assertThat(body.subtotal()).isEqualByComparingTo("79.98");
            assertThat(body.discount()).isEqualByComparingTo("8.00");
            assertThat(body.total()).isEqualByComparingTo("71.98");
        }

        @Test
        @DisplayName("Preview merges the same product listed twice")
        void previewMergesDuplicates() {
            PreviewRequest request = new PreviewRequest(List.of(
                    new AddItemRequest(Products.JAVA_BOOK, 1),
                    new AddItemRequest(Products.JAVA_BOOK, 2)), null, "EXPRESS");

            Cart body = expect(CartClient.preview(request), 200, Cart.class);

            assertThat(body.items()).hasSize(1);
            assertThat(body.item(Products.JAVA_BOOK).quantity()).isEqualTo(3);
            assertThat(body.shipping()).isEqualByComparingTo("9.99");
        }

        @Test
        @DisplayName("Preview with an empty list returns zero totals")
        void previewEmpty() {
            Cart body = expect(CartClient.preview(new PreviewRequest(List.of(), null, null)), 200, Cart.class);

            assertThat(body.total()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Preview with an invalid promo code gives 400")
        void previewInvalidPromo() {
            PreviewRequest request = new PreviewRequest(List.of(new AddItemRequest(Products.SELENIUM_BOOK, 1)), "NOPE", null);

            assertThat(expectError(CartClient.preview(request), 400).message())
                    .isEqualTo("Promo code \"NOPE\" is not valid");
        }

        @Test
        @DisplayName("Preview without the items list gives 400")
        void previewWithoutItems() {
            ErrorResponse error = expectError(CartClient.preview(new PreviewRequest(null, null, null)), 400);

            assertThat(error.fieldErrors()).containsEntry("items", "Items are required");
        }
    }
}
