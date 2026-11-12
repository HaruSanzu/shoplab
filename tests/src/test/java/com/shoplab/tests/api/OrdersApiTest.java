package com.shoplab.tests.api;

import com.shoplab.tests.api.client.CartClient;
import com.shoplab.tests.api.client.OrdersClient;
import com.shoplab.tests.api.client.ProductsClient;
import com.shoplab.tests.api.model.CartModels.Cart;
import com.shoplab.tests.api.model.ErrorResponse;
import com.shoplab.tests.api.model.OrderModels.Card;
import com.shoplab.tests.api.model.OrderModels.Order;
import com.shoplab.tests.api.model.OrderModels.OrderItem;
import com.shoplab.tests.api.model.OrderModels.OrderRequest;
import com.shoplab.tests.api.model.ProductModels.Product;
import com.shoplab.tests.data.TestData;
import com.shoplab.tests.data.TestData.Cards;
import com.shoplab.tests.data.TestData.Products;
import com.shoplab.tests.data.TestUsers;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Orders")
@DisplayName("Orders API")
class OrdersApiTest extends BaseApiTest {

    private final ProductsClient products = new ProductsClient();
    private CartClient cart;
    private OrdersClient orders;

    @BeforeEach
    void newUser() {
        String token = TestUsers.registerNew().token();
        cart = new CartClient(token);
        orders = new OrdersClient(token);
    }

    // ---------- placing an order ----------

    @Test
    @Tag("smoke")
    @DisplayName("Order paid by card is created from the cart")
    void placeOrderWithCard() {
        cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);

        Order order = expect(orders.place(TestData.cardOrder()), 201, Order.class);

        assertThat(order.orderNumber()).matches("^SL-\\d{6}$");
        assertThat(order.status()).isEqualTo("PROCESSING");
        assertThat(order.createdAt()).isNotBlank();
        assertThat(order.deliveryMethod()).isEqualTo("STANDARD");
        assertThat(order.paymentMethod()).isEqualTo("CARD");
        // Only the last 4 digits are kept: never store or return the full card number
        assertThat(order.cardLast4()).isEqualTo("4242");

        assertThat(order.items()).hasSize(1);
        OrderItem item = order.items().getFirst();
        assertThat(item.productId()).isEqualTo(Products.SELENIUM_BOOK);
        assertThat(item.title()).isEqualTo("Selenium in Practice");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.lineTotal()).isEqualByComparingTo("79.98");

        assertThat(order.subtotal()).isEqualByComparingTo("79.98");
        assertThat(order.discount()).isEqualByComparingTo("0");
        assertThat(order.shipping()).isEqualByComparingTo("0");
        assertThat(order.total()).isEqualByComparingTo("79.98");
        assertThat(order.shippingAddress()).isEqualTo(TestData.validAddress());
    }

    @Test
    @DisplayName("Cash on delivery order needs no card")
    void placeOrderCashOnDelivery() {
        cart.addItem(Products.JAVA_BOOK, 1).then().statusCode(200);

        Order order = expect(orders.place(TestData.cashOnDeliveryOrder()), 201, Order.class);

        assertThat(order.paymentMethod()).isEqualTo("COD");
        assertThat(order.cardLast4()).isNull();
        assertThat(order.shipping()).isEqualByComparingTo("5.99");
        assertThat(order.total()).isEqualByComparingTo("30.98");
    }

    @Test
    @DisplayName("Express delivery adds $9.99 to the order")
    void placeOrderExpress() {
        cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);

        Order order = expect(orders.place(TestData.cardOrder().withDeliveryMethod("EXPRESS")), 201, Order.class);

        assertThat(order.deliveryMethod()).isEqualTo("EXPRESS");
        assertThat(order.shipping()).isEqualByComparingTo("9.99");
        assertThat(order.total()).isEqualByComparingTo("89.97");
    }

    @Test
    @DisplayName("Promo code of the cart is applied to the order")
    void promoCodeIsAppliedToOrder() {
        cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);
        cart.applyPromo("QA10").then().statusCode(200);

        Order order = expect(orders.place(TestData.cardOrder()), 201, Order.class);

        assertThat(order.promoCode()).isEqualTo("QA10");
        assertThat(order.discount()).isEqualByComparingTo("8.00");
        assertThat(order.total()).isEqualByComparingTo("71.98");
    }

    @Test
    @DisplayName("Order totals are the same as the cart totals just before checkout")
    void orderTotalsMatchCart() {
        cart.addItem(Products.HEADPHONES, 1).then().statusCode(200);
        cart.addItem(Products.GHERKIN_BOOK, 3).then().statusCode(200);
        Cart before = expect(cart.applyPromo("QA10"), 200, Cart.class);

        Order order = expect(orders.place(TestData.cardOrder()), 201, Order.class);

        assertThat(order.subtotal()).isEqualByComparingTo(before.subtotal());
        assertThat(order.discount()).isEqualByComparingTo(before.discount());
        assertThat(order.shipping()).isEqualByComparingTo(before.shipping());
        assertThat(order.total()).isEqualByComparingTo(before.total());
    }

    @Test
    @Tag("smoke")
    @DisplayName("Cart is empty after a successful order")
    void cartIsEmptiedAfterOrder() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        cart.applyPromo("QA10").then().statusCode(200);

        orders.place(TestData.cardOrder()).then().statusCode(201);

        Cart after = expect(cart.get(), 200, Cart.class);
        assertThat(after.items()).isEmpty();
        assertThat(after.promoCode()).isNull();
    }

    @Test
    @DisplayName("Placing an order decreases the product stock")
    void orderDecreasesStock() {
        int stockBefore = stockOf(Products.COFFEE_MAKER);
        cart.addItem(Products.COFFEE_MAKER, 3).then().statusCode(200);

        orders.place(TestData.cardOrder()).then().statusCode(201);

        // Compare with the value BEFORE, not with a fixed number: other tests may have bought it too
        assertThat(stockOf(Products.COFFEE_MAKER)).isEqualTo(stockBefore - 3);
    }

    @Test
    @DisplayName("Order fails (409) if someone else bought the stock after it was added to the cart")
    void stockGoneBeforeCheckout() {
        // Two users put the last 2 robot vacuums into their carts
        cart.addItem(Products.LOW_STOCK, 2).then().statusCode(200);
        String otherToken = TestUsers.registerNew().token();
        new CartClient(otherToken).addItem(Products.LOW_STOCK, 2).then().statusCode(200);

        // The other user checks out first and gets them
        new OrdersClient(otherToken).place(TestData.cardOrder()).then().statusCode(201);

        // Now our order must fail, and our cart must stay as it was
        assertThat(expectError(orders.place(TestData.cardOrder()), 409).message())
                .isEqualTo("Only 0 item(s) of \"Robot Vacuum R5\" available in stock");
        assertThat(expect(cart.get(), 200, Cart.class).items()).hasSize(1);
    }

    // ---------- order history ----------

    @Test
    @DisplayName("Order can be read by its number")
    void getOrderByNumber() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        Order placed = expect(orders.place(TestData.cardOrder()), 201, Order.class);

        Order loaded = expect(orders.get(placed.orderNumber()), 200, Order.class);

        // Records compare all fields: the order read later is exactly the order that was placed
        assertThat(loaded).isEqualTo(placed);
    }

    @Test
    @DisplayName("Order list shows the user's orders, newest first")
    void listOrdersNewestFirst() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        String first = expect(orders.place(TestData.cardOrder()), 201, Order.class).orderNumber();
        cart.addItem(Products.JAVA_BOOK, 1).then().statusCode(200);
        String second = expect(orders.place(TestData.cardOrder()), 201, Order.class).orderNumber();

        List<Order> list = List.of(expect(orders.list(), 200, Order[].class));

        assertThat(list).extracting(Order::orderNumber).containsExactly(second, first);
    }

    @Test
    @DisplayName("New user has no orders")
    void newUserHasNoOrders() {
        assertThat(expect(orders.list(), 200, Order[].class)).isEmpty();
    }

    @Test
    @DisplayName("Another user's order is not visible (404, not 403)")
    void otherUsersOrderIsHidden() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        String orderNumber = expect(orders.place(TestData.cardOrder()), 201, Order.class).orderNumber();
        OrdersClient otherUser = new OrdersClient(TestUsers.registerNew().token());

        // 404 instead of 403: we don't even reveal that this order exists
        assertThat(expectError(otherUser.get(orderNumber), 404).message())
                .isEqualTo("Order " + orderNumber + " not found");
        expectError(otherUser.cancel(orderNumber), 404);
        assertThat(expect(otherUser.list(), 200, Order[].class)).isEmpty();
    }

    @Test
    @DisplayName("Unknown order number gives 404")
    void unknownOrder() {
        assertThat(expectError(orders.get("SL-000000"), 404).message()).isEqualTo("Order SL-000000 not found");
    }

    // ---------- cancelling ----------

    @Test
    @Tag("smoke")
    @DisplayName("Cancelling an order sets status CANCELLED and returns the items to stock")
    void cancelOrder() {
        int stockBefore = stockOf(Products.COFFEE_MAKER);
        cart.addItem(Products.COFFEE_MAKER, 2).then().statusCode(200);
        String orderNumber = expect(orders.place(TestData.cardOrder()), 201, Order.class).orderNumber();
        assertThat(stockOf(Products.COFFEE_MAKER)).isEqualTo(stockBefore - 2);

        Order cancelled = expect(orders.cancel(orderNumber), 200, Order.class);

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(stockOf(Products.COFFEE_MAKER)).isEqualTo(stockBefore);
        assertThat(expect(orders.get(orderNumber), 200, Order.class).status()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Cancelling the same order twice gives 409")
    void cancelTwice() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        String orderNumber = expect(orders.place(TestData.cardOrder()), 201, Order.class).orderNumber();
        orders.cancel(orderNumber).then().statusCode(200);

        assertThat(expectError(orders.cancel(orderNumber), 409).message())
                .isEqualTo("Only orders in PROCESSING status can be cancelled");
    }

    // ---------- payment ----------

    @Test
    @Tag("smoke")
    @DisplayName("Declined card gives 402 and nothing changes")
    void declinedCard() {
        int stockBefore = stockOf(Products.SELENIUM_BOOK);
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        ErrorResponse error = expectError(orders.place(TestData.cardOrder().withCard(Cards.declined())), 402);

        assertThat(error.message()).isEqualTo("Payment declined. Please use a different card.");
        // A failed payment must not create an order, empty the cart or take stock
        assertThat(expect(orders.list(), 200, Order[].class)).isEmpty();
        assertThat(expect(cart.get(), 200, Cart.class).items()).hasSize(1);
        assertThat(stockOf(Products.SELENIUM_BOOK)).isEqualTo(stockBefore);
    }

    @Test
    @DisplayName("Card number may be written with dashes or without spaces")
    void cardNumberFormats() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        Card card = new Card("4242-4242-4242-4242", Cards.futureExpiry(), "123");

        Order order = expect(orders.place(TestData.cardOrder().withCard(card)), 201, Order.class);

        assertThat(order.cardLast4()).isEqualTo("4242");
    }

    @Test
    @DisplayName("Card payment without card details gives 400")
    void cardPaymentWithoutCard() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        ErrorResponse error = expectError(orders.place(TestData.cardOrder().withCard(null)), 400);

        assertThat(error.fieldErrors()).containsEntry("card", "Card details are required for card payment");
    }

    @ParameterizedTest(name = "number=\"{0}\", expiry=\"{1}\", cvv=\"{2}\" -> {3}")
    @DisplayName("Invalid card details give 400 with a field error")
    @CsvSource(delimiter = '|', value = {
            // number               | expiry | cvv  | field       | message
            "4242 4242 4242         | 12/99  | 123  | card.number | Card number must be 16 digits",
            "4242 4242 4242 424a    | 12/99  | 123  | card.number | Card number must be 16 digits",
            "4242 4242 4242 4242    | 13/99  | 123  | card.expiry | Use the format MM/YY",
            "4242 4242 4242 4242    | 1299   | 123  | card.expiry | Use the format MM/YY",
            "4242 4242 4242 4242    | 01/20  | 123  | card.expiry | Your card has expired",
            "4242 4242 4242 4242    | 12/99  | 12   | card.cvv    | CVV must be 3 digits",
            "4242 4242 4242 4242    | 12/99  | abc  | card.cvv    | CVV must be 3 digits",
    })
    void invalidCard(String number, String expiry, String cvv, String field, String message) {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        ErrorResponse error = expectError(orders.place(TestData.cardOrder().withCard(new Card(number, expiry, cvv))), 400);

        assertThat(error.message()).isEqualTo("Validation failed");
        assertThat(error.fieldErrors()).containsEntry(field, message);
    }

    // ---------- other validation ----------

    @Test
    @DisplayName("Order with an empty cart gives 400")
    void emptyCart() {
        assertThat(expectError(orders.place(TestData.cardOrder()), 400).message()).isEqualTo("Your cart is empty");
    }

    @Nested
    @DisplayName("Shipping address validation")
    class AddressValidation {

        @BeforeEach
        void productInCart() {
            cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        }

        @ParameterizedTest(name = "zip = \"{0}\"")
        @DisplayName("ZIP code must be 5 digits")
        @CsvSource({"1234", "123456", "abcde"})
        void invalidZip(String zip) {
            OrderRequest request = TestData.cardOrder().withAddress(TestData.validAddress().withZip(zip));

            assertThat(expectError(orders.place(request), 400).fieldErrors())
                    .containsEntry("shippingAddress.zip", "ZIP code must be 5 digits");
        }

        @ParameterizedTest(name = "phone = \"{0}\"")
        @DisplayName("Phone must be 7-15 digits, spaces or dashes")
        @CsvSource({"12345", "phone-number", "+1 555 123 4567 8901"})
        void invalidPhone(String phone) {
            OrderRequest request = TestData.cardOrder().withAddress(TestData.validAddress().withPhone(phone));

            assertThat(expectError(orders.place(request), 400).fieldErrors())
                    .containsEntry("shippingAddress.phone", "Enter a valid phone number");
        }

        @Test
        @DisplayName("Full name is required")
        void missingFullName() {
            OrderRequest request = TestData.cardOrder().withAddress(TestData.validAddress().withFullName(null));

            assertThat(expectError(orders.place(request), 400).fieldErrors())
                    .containsEntry("shippingAddress.fullName", "Please enter your full name");
        }

        @Test
        @DisplayName("Missing shipping address gives 400")
        void missingAddress() {
            assertThat(expectError(orders.place(TestData.cardOrder().withAddress(null)), 400).fieldErrors())
                    .containsEntry("shippingAddress", "Shipping address is required");
        }

        @Test
        @DisplayName("Unknown payment method gives 400")
        void unknownPaymentMethod() {
            assertThat(expectError(orders.place(TestData.cardOrder().withPaymentMethod("BITCOIN")), 400).message())
                    .isEqualTo("Unknown payment method 'BITCOIN'. Allowed: CARD, COD");
        }

        @Test
        @DisplayName("Missing delivery and payment methods give 400")
        void missingMethods() {
            OrderRequest request = new OrderRequest(null, null, TestData.validAddress(), Cards.valid());

            assertThat(expectError(orders.place(request), 400).fieldErrors())
                    .containsEntry("deliveryMethod", "Delivery method is required")
                    .containsEntry("paymentMethod", "Payment method is required");
        }
    }

    @Test
    @DisplayName("Order endpoints require login (401 without a token)")
    void ordersRequireLogin() {
        OrdersClient guest = new OrdersClient(null);

        expectError(guest.list(), 401);
        expectError(guest.place(TestData.cardOrder()), 401);
        expectError(guest.get("SL-000000"), 401);
    }

    private int stockOf(long productId) {
        return expect(products.getById(productId), 200, Product.class).stock();
    }
}
