package com.shoplab.tests.api.client;

import com.shoplab.tests.api.Specs;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * API CLIENT (also called "Service Object"): the API version of a Page Object.
 * <p>
 * A Page Object hides HOW to click on a page; an API client hides HOW to call an endpoint
 * (URL, HTTP method, headers, body). Tests only say WHAT they want: {@code products.getById(7)}.
 * If a URL changes, only this class changes, not 50 tests.
 * <p>
 * Methods return the raw REST Assured {@link Response}, so a test can check anything:
 * status code, headers, body, response time.
 */
public class ProductsClient {

    /** Search with any combination of filters, e.g. Map.of("category", "Books", "sort", "price-asc"). */
    public Response search(Map<String, ?> queryParams) {
        return given()
                .spec(Specs.anonymous())
                .queryParams(queryParams)
                .when()
                .get("/api/products");
    }

    public Response getAll() {
        return search(Map.of());
    }

    /** Object, not long: lets a negative test pass "abc" as the id. */
    public Response getById(Object id) {
        return given()
                .spec(Specs.anonymous())
                .pathParam("id", id)
                .when()
                .get("/api/products/{id}");
    }

    public Response categories() {
        return given()
                .spec(Specs.anonymous())
                .when()
                .get("/api/categories");
    }
}
