package com.shoplab.tests.data;

import com.shoplab.tests.api.client.AuthClient;
import com.shoplab.tests.api.model.AuthModels.AuthResponse;
import com.shoplab.tests.api.model.AuthModels.RegisterRequest;

import java.util.UUID;

/**
 * Creates fresh users for tests.
 * <p>
 * Why not always use standard@shoplab.test? Because tests that share one user also share
 * ONE cart and ONE order history. Test A adds a product, test B expects an empty cart, and
 * B fails or passes depending on the ORDER the tests run in ("flaky" tests).
 * A brand-new user per test means a brand-new empty cart: tests can't disturb each other.
 */
public final class TestUsers {

    private TestUsers() {
    }

    /** A logged-in user: everything a test may need to know about them. */
    public record LoggedInUser(String name, String email, String password, String token) {
    }

    /** An email nobody has used yet, e.g. "user-3f9a1c2b@shoplab.test". */
    public static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@shoplab.test";
    }

    /** Registers a new user through the API (registration logs in right away) and returns them. */
    public static LoggedInUser registerNew() {
        String email = uniqueEmail();
        RegisterRequest request = new RegisterRequest("Test Customer", email, TestData.PASSWORD);

        AuthResponse response = new AuthClient().register(request)
                .then()
                .statusCode(201)
                .extract()
                .as(AuthResponse.class);

        return new LoggedInUser(request.name(), email, request.password(), response.token());
    }
}
