package com.shoplab.tests.db;

import com.shoplab.tests.BaseTest;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

/**
 * Parent class of every database test class.
 * <p>
 * A database test usually has three parts:
 * <ol>
 *   <li>Act through the API, like a real user (register, add to cart, place an order).</li>
 *   <li>Look into the database with SQL ({@link ShopDb}).</li>
 *   <li>Check that the stored data is right: what the API said happened really happened.</li>
 * </ol>
 * JUnit runs the parent's @BeforeAll first (wait for API + reset DB), then this one.
 */
@Tag("db")
@Epic("Database")
public abstract class BaseDbTest extends BaseTest {

    @BeforeAll
    static void checkDatabaseIsReachable() {
        Database.checkConnection();
    }
}
