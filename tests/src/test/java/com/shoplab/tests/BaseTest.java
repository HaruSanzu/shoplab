package com.shoplab.tests;

import com.shoplab.tests.api.client.TestSupportClient;
import com.shoplab.tests.api.model.ErrorResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Common parent of ALL test classes (API tests, database tests, later UI tests).
 * <ul>
 *   <li>Before the first test of EACH class: wait for the API and reset the database,
 *       so every class starts from the same known data (original stock, no orders).</li>
 *   <li>Small helpers to check API responses.</li>
 * </ul>
 * It has no @Tag on purpose: the subclasses BaseApiTest (@Tag("api")) and BaseDbTest (@Tag("db"))
 * add their own tag, so "-Dgroups=api" and "-Dgroups=db" select different tests.
 */
public abstract class BaseTest {

    /** @BeforeAll = runs once before all tests of the class (must be static). */
    @BeforeAll
    static void prepareEnvironment() {
        TestSupportClient support = new TestSupportClient();
        support.waitUntilApiIsHealthy();
        support.resetDatabase();
    }

    /**
     * Checks the status code and converts the JSON body into a Java object.
     * Example: {@code Cart cart = expect(cartClient.get(), 200, Cart.class);}
     */
    protected static <T> T expect(Response response, int expectedStatus, Class<T> bodyType) {
        return response.then()
                .statusCode(expectedStatus)
                .extract()
                .as(bodyType);
    }

    /**
     * For negative tests: checks the status code and the common error format, returns the error
     * so the test can check the message. Example:
     * {@code assertThat(expectError(response, 404).message()).isEqualTo("Product with id 999 not found");}
     */
    protected static ErrorResponse expectError(Response response, int expectedStatus) {
        ErrorResponse error = expect(response, expectedStatus, ErrorResponse.class);
        assertThat(error.status()).as("status inside the error body").isEqualTo(expectedStatus);
        assertThat(error.timestamp()).as("timestamp").isNotBlank();
        assertThat(error.path()).as("path").startsWith("/api/");
        return error;
    }
}
