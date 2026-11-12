package com.shoplab.tests.data;

import com.shoplab.tests.api.model.OrderModels.Address;
import com.shoplab.tests.api.model.OrderModels.Card;
import com.shoplab.tests.api.model.OrderModels.OrderRequest;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * The known test data of the shop (loaded by Flyway migration V2, see CLAUDE.md "Test data").
 * Named constants instead of "magic values": {@code Products.OUT_OF_STOCK} says more than {@code 3}.
 */
public final class TestData {

    private TestData() {
    }

    public static final String PASSWORD = "Password123";

    public static final class Users {
        public static final String STANDARD = "standard@shoplab.test";
        public static final String LOCKED = "locked@shoplab.test";
        public static final String SLOW = "slow@shoplab.test";

        private Users() {
        }
    }

    /** Product ids with their price/stock from the seed data, so expected totals are easy to read. */
    public static final class Products {
        /** Wireless Headphones X200, $79.99, stock 15. */
        public static final long HEADPHONES = 1;
        /** 4K Action Camera, stock 0. */
        public static final long OUT_OF_STOCK = 3;
        /** USB-C Charger 65W, $29.99, stock 100: for "max 10 per item" tests. */
        public static final long CHARGER = 6;
        /** Selenium in Practice, $39.99, stock 25. */
        public static final long SELENIUM_BOOK = 7;
        /** Learning Java Step by Step, $24.99, stock 30. */
        public static final long JAVA_BOOK = 8;
        /** BDD with Gherkin, $19.99, stock 18. */
        public static final long GHERKIN_BOOK = 9;
        /** Coffee Maker Brew 12, $59.99, stock 20. */
        public static final long COFFEE_MAKER = 12;
        /** Ceramic Mug Set, $18.00, stock 50. */
        public static final long MUG_SET = 14;
        /** Robot Vacuum R5, $249.99, only 2 in stock. */
        public static final long LOW_STOCK = 15;
        /** Remote Control Car, $54.99, stock 11. */
        public static final long RC_CAR = 21;
        /** Leather Wallet, $32.00, stock 27. */
        public static final long WALLET = 23;
        public static final long NOT_EXISTING = 999;

        public static final int TOTAL = 25;

        private Products() {
        }
    }

    public static final class Cards {
        public static final String VALID = "4242 4242 4242 4242";
        public static final String DECLINED = "4000 0000 0000 0002";

        /**
         * Always 2 years in the future, e.g. "11/28". A hard-coded "12/30" would make the tests
         * fail by themselves in 2031: a classic "time bomb" in test data.
         */
        public static String futureExpiry() {
            return YearMonth.now().plusYears(2).format(DateTimeFormatter.ofPattern("MM/yy"));
        }

        public static Card valid() {
            return new Card(VALID, futureExpiry(), "123");
        }

        public static Card declined() {
            return new Card(DECLINED, futureExpiry(), "123");
        }

        private Cards() {
        }
    }

    public static Address validAddress() {
        return new Address("Test Customer", "1 Main St", "Austin", "73301", "United States", "+1 555 123 4567");
    }

    /** A valid order: standard delivery, paid by the test card that always succeeds. */
    public static OrderRequest cardOrder() {
        return new OrderRequest("STANDARD", "CARD", validAddress(), Cards.valid());
    }

    /** A valid order paid cash on delivery: no card needed. */
    public static OrderRequest cashOnDeliveryOrder() {
        return new OrderRequest("STANDARD", "COD", validAddress(), null);
    }
}
