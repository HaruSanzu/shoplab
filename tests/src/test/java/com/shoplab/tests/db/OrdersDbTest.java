package com.shoplab.tests.db;

import com.shoplab.tests.api.client.CartClient;
import com.shoplab.tests.api.client.OrdersClient;
import com.shoplab.tests.api.model.OrderModels.Address;
import com.shoplab.tests.api.model.OrderModels.Order;
import com.shoplab.tests.data.TestData;
import com.shoplab.tests.data.TestData.Cards;
import com.shoplab.tests.data.TestData.Products;
import com.shoplab.tests.data.TestUsers;
import com.shoplab.tests.data.TestUsers.LoggedInUser;
import com.shoplab.tests.db.DbRows.OrderItemRow;
import com.shoplab.tests.db.DbRows.OrderRow;
import com.shoplab.tests.db.DbRows.ShippingRow;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Orders in the database")
@DisplayName("Orders (DB)")
class OrdersDbTest extends BaseDbTest {

    private LoggedInUser user;
    private CartClient cart;
    private OrdersClient orders;

    @BeforeEach
    void newUser() {
        user = TestUsers.registerNew();
        cart = new CartClient(user.token());
        orders = new OrdersClient(user.token());
    }

    // ---------- the order row ----------

    @Test
    @Tag("smoke")
    @DisplayName("Placed order is stored with the same data the API returned")
    void orderRowMatchesApiResponse() {
        cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);
        cart.applyPromo("QA10").then().statusCode(200);

        Order api = placeOrder();
        OrderRow row = ShopDb.order(api.orderNumber());

        assertThat(row.userId()).as("order belongs to the user").isEqualTo(ShopDb.user(user.email()).id());
        assertThat(row.status()).isEqualTo("PROCESSING");
        assertThat(row.deliveryMethod()).isEqualTo("STANDARD");
        assertThat(row.paymentMethod()).isEqualTo("CARD");
        assertThat(row.promoCode()).isEqualTo("QA10");
        assertThat(row.subtotal()).isEqualByComparingTo(api.subtotal());
        assertThat(row.discount()).isEqualByComparingTo(api.discount());
        assertThat(row.shippingCost()).isEqualByComparingTo(api.shipping());
        assertThat(row.total()).isEqualByComparingTo(api.total());
        // Checks the bug fixed in step 3: the API and the database must show the same moment
        assertThat(row.createdAt()).isEqualTo(Instant.parse(api.createdAt()));
    }

    @Test
    @DisplayName("Totals in the row are consistent: total = subtotal - discount + shipping")
    void orderTotalsAreConsistent() {
        cart.addItem(Products.HEADPHONES, 1).then().statusCode(200);
        cart.addItem(Products.GHERKIN_BOOK, 3).then().statusCode(200);
        cart.applyPromo("QA10").then().statusCode(200);

        OrderRow row = ShopDb.order(placeOrder().orderNumber());

        assertThat(row.total()).isEqualByComparingTo(row.subtotal().subtract(row.discount()).add(row.shippingCost()));
        // subtotal = sum of all line totals
        BigDecimal sumOfLines = ShopDb.orderItems(row.orderNumber()).stream()
                .map(OrderItemRow::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(row.subtotal()).isEqualByComparingTo(sumOfLines);
    }

    @Test
    @Tag("smoke")
    @DisplayName("Only the last 4 card digits are stored")
    void onlyLastFourCardDigitsStored() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        OrderRow row = ShopDb.order(placeOrder().orderNumber());

        assertThat(row.cardLast4()).isEqualTo("4242");
        // The full number must not be stored anywhere in the orders row
        long rowsWithFullNumber = Database.count(
                "SELECT count(*) FROM orders o WHERE o.order_number = ? AND o::text LIKE '%4242424242424242%'",
                row.orderNumber());
        assertThat(rowsWithFullNumber).isZero();
    }

    @Test
    @DisplayName("Cash on delivery order has no card digits (NULL)")
    void cashOnDeliveryHasNoCard() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        Order api = expect(orders.place(TestData.cashOnDeliveryOrder()), 201, Order.class);

        OrderRow row = ShopDb.order(api.orderNumber());
        assertThat(row.paymentMethod()).isEqualTo("COD");
        assertThat(row.cardLast4()).isNull();
    }

    @Test
    @DisplayName("Shipping address is stored in the ship_* columns")
    void shippingAddressStored() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        ShippingRow ship = ShopDb.shippingOf(placeOrder().orderNumber());

        Address sent = TestData.validAddress();
        assertThat(ship).isEqualTo(new ShippingRow(sent.fullName(), sent.address(), sent.city(),
                sent.zip(), sent.country(), sent.phone()));
    }

    // ---------- order lines ----------

    @Test
    @Tag("smoke")
    @DisplayName("One order_items row per cart line, with title and price copied from the product")
    void orderItemsRows() {
        cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);
        cart.addItem(Products.JAVA_BOOK, 1).then().statusCode(200);

        List<OrderItemRow> items = ShopDb.orderItems(placeOrder().orderNumber());

        assertThat(items).containsExactlyInAnyOrder(
                new OrderItemRow(Products.SELENIUM_BOOK, "Selenium in Practice", new BigDecimal("39.99"), 2, new BigDecimal("79.98")),
                new OrderItemRow(Products.JAVA_BOOK, "Learning Java Step by Step", new BigDecimal("24.99"), 1, new BigDecimal("24.99")));
    }

    @Test
    @DisplayName("Changing a product price later does not change existing orders")
    void orderKeepsPriceAtPurchaseTime() {
        cart.addItem(Products.WALLET, 1).then().statusCode(200);
        String orderNumber = placeOrder().orderNumber();
        BigDecimal originalPrice = ShopDb.priceOf(Products.WALLET);

        try {
            // ARRANGE THROUGH THE DATABASE: the API has no "change price" endpoint,
            // so the test changes the price directly with SQL.
            Database.update("UPDATE products SET price = ? WHERE id = ?", new BigDecimal("99.00"), Products.WALLET);

            assertThat(ShopDb.orderItems(orderNumber)).singleElement()
                    .extracting(OrderItemRow::unitPrice).isEqualTo(new BigDecimal("32.00"));
            Order viaApi = expect(orders.get(orderNumber), 200, Order.class);
            assertThat(viaApi.items().getFirst().unitPrice()).isEqualByComparingTo("32.00");
            assertThat(viaApi.total()).isEqualByComparingTo(ShopDb.order(orderNumber).total());
        } finally {
            // Always put the data back, even if an assertion above failed: other tests use this product
            Database.update("UPDATE products SET price = ? WHERE id = ?", originalPrice, Products.WALLET);
        }
    }

    // ---------- side effects: stock and cart ----------

    @Test
    @Tag("smoke")
    @DisplayName("Placing an order decreases products.stock by the ordered quantity")
    void orderDecreasesStock() {
        int stockBefore = ShopDb.stockOf(Products.COFFEE_MAKER);
        cart.addItem(Products.COFFEE_MAKER, 3).then().statusCode(200);

        placeOrder();

        assertThat(ShopDb.stockOf(Products.COFFEE_MAKER)).isEqualTo(stockBefore - 3);
    }

    @Test
    @DisplayName("Cancelling sets status CANCELLED and restores products.stock")
    void cancelRestoresStock() {
        int stockBefore = ShopDb.stockOf(Products.COFFEE_MAKER);
        cart.addItem(Products.COFFEE_MAKER, 2).then().statusCode(200);
        String orderNumber = placeOrder().orderNumber();

        orders.cancel(orderNumber).then().statusCode(200);

        assertThat(ShopDb.order(orderNumber).status()).isEqualTo("CANCELLED");
        assertThat(ShopDb.stockOf(Products.COFFEE_MAKER)).isEqualTo(stockBefore);
        // The order and its lines are kept (history), only the status changes
        assertThat(ShopDb.orderItems(orderNumber)).hasSize(1);
    }

    @Test
    @DisplayName("Ordering the whole stock leaves stock = 0, and the product shows as out of stock")
    void orderWholeStock() {
        cart.addItem(Products.LOW_STOCK, 2).then().statusCode(200);

        placeOrder();

        assertThat(ShopDb.stockOf(Products.LOW_STOCK)).isZero();
    }

    @Test
    @Tag("smoke")
    @DisplayName("After an order the cart rows are deleted and the promo code is cleared")
    void orderEmptiesCart() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);
        cart.applyPromo("QA10").then().statusCode(200);

        placeOrder();

        assertThat(ShopDb.cartItems(user.email())).isEmpty();
        assertThat(ShopDb.findCart(user.email())).get().extracting(DbRows.CartRow::promoCode).isNull();
    }

    // ---------- transactions: all or nothing ----------

    @Test
    @Tag("smoke")
    @DisplayName("Declined card: no order row, stock and cart unchanged (transaction rolled back)")
    void declinedCardChangesNothing() {
        cart.addItem(Products.SELENIUM_BOOK, 2).then().statusCode(200);
        int stockBefore = ShopDb.stockOf(Products.SELENIUM_BOOK);

        orders.place(TestData.cardOrder().withCard(Cards.declined())).then().statusCode(402);

        assertThat(ShopDb.countOrdersOf(user.email())).isZero();
        assertThat(ShopDb.stockOf(Products.SELENIUM_BOOK)).isEqualTo(stockBefore);
        assertThat(ShopDb.cartItems(user.email())).hasSize(1);
    }

    @Test
    @DisplayName("Not enough stock for one line: no product loses stock, not even the other lines")
    void notEnoughStockChangesNothing() {
        // A different low-stock product than orderWholeStock uses: tests of one class share the stock,
        // and JUnit does not promise any order between test methods.
        // Our cart: 1 headphones + all 3 copies of the low-stock book
        cart.addItem(Products.HEADPHONES, 1).then().statusCode(200);
        cart.addItem(Products.LOW_STOCK_BOOK, 3).then().statusCode(200);
        // Someone else buys 1 copy first: only 2 left, our order needs 3
        LoggedInUser other = TestUsers.registerNew();
        new CartClient(other.token()).addItem(Products.LOW_STOCK_BOOK, 1).then().statusCode(200);
        new OrdersClient(other.token()).place(TestData.cardOrder()).then().statusCode(201);
        int headphonesBefore = ShopDb.stockOf(Products.HEADPHONES);
        int booksBefore = ShopDb.stockOf(Products.LOW_STOCK_BOOK);

        orders.place(TestData.cardOrder()).then().statusCode(409);

        // Headphones had enough stock, but they must NOT be taken either: all or nothing
        assertThat(ShopDb.stockOf(Products.HEADPHONES)).as("headphones").isEqualTo(headphonesBefore);
        assertThat(ShopDb.stockOf(Products.LOW_STOCK_BOOK)).as("books").isEqualTo(booksBefore);
        assertThat(ShopDb.countOrdersOf(user.email())).isZero();
    }

    @Test
    @DisplayName("Rejected order (validation error) creates no rows")
    void invalidOrderCreatesNoRows() {
        cart.addItem(Products.SELENIUM_BOOK, 1).then().statusCode(200);

        orders.place(TestData.cardOrder().withAddress(TestData.validAddress().withZip("12"))).then().statusCode(400);

        assertThat(ShopDb.countOrdersOf(user.email())).isZero();
        assertThat(ShopDb.cartItems(user.email())).hasSize(1);
    }

    private Order placeOrder() {
        return expect(orders.place(TestData.cardOrder()), 201, Order.class);
    }
}
