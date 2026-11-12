package com.shoplab.tests.api;

import com.shoplab.tests.BaseTest;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.Tag;

/**
 * Parent class of every API test class.
 * <ul>
 *   <li>{@code @Tag("api")}: run only API tests with {@code mvn test -Dgroups=api}.
 *       Tags are inherited by subclasses.</li>
 *   <li>{@code @Epic("API")}: groups these tests in the Allure report.</li>
 * </ul>
 * Database reset and the expect(...) helpers come from {@link BaseTest}.
 */
@Tag("api")
@Epic("API")
public abstract class BaseApiTest extends BaseTest {
}
