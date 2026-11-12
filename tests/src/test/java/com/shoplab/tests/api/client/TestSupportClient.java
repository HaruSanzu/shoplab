package com.shoplab.tests.api.client;

import com.shoplab.tests.api.Specs;
import com.shoplab.tests.config.TestConfig;

import java.time.Duration;
import java.time.Instant;

import static io.restassured.RestAssured.given;

/** Helpers that prepare the environment: wait for the API, reset the database. */
public class TestSupportClient {

    /**
     * Waits until GET /actuator/health answers 200 (backend up and connected to the DB).
     * Right after "docker compose up" the backend needs a few seconds to start;
     * without this wait the first tests would fail with "Connection refused".
     */
    public void waitUntilApiIsHealthy() {
        Instant deadline = Instant.now().plusSeconds(TestConfig.apiWaitSeconds());
        Exception lastError = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                int status = given().spec(Specs.technical()).when().get("/actuator/health").statusCode();
                if (status == 200) {
                    return;
                }
            } catch (Exception e) { // connection refused: the server is not listening yet
                lastError = e;
            }
            pause(Duration.ofSeconds(1));
        }
        throw new IllegalStateException("The API at " + TestConfig.apiUrl() + " is not healthy after "
                + TestConfig.apiWaitSeconds() + " s. Is the shop running? Start it with: docker compose up -d", lastError);
    }

    /**
     * POST /api/test/reset: deletes ALL data and loads the original test data again
     * (25 products with original stock, 3 built-in users, no carts, no orders).
     */
    public void resetDatabase() {
        given()
                .spec(Specs.technical())
                .when()
                .post("/api/test/reset")
                .then()
                .statusCode(200);
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
