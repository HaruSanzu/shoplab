package com.shoplab.tests.api.client;

import com.shoplab.tests.api.Specs;
import com.shoplab.tests.api.model.OrderModels.OrderRequest;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/** Checkout and order history of ONE user (see {@link CartClient} for why the token is in the constructor). */
public class OrdersClient {

    private final String token;

    public OrdersClient(String token) {
        this.token = token;
    }

    public Response place(OrderRequest body) {
        return given()
                .spec(Specs.withToken(token))
                .body(body)
                .when()
                .post("/api/orders");
    }

    public Response list() {
        return given()
                .spec(Specs.withToken(token))
                .when()
                .get("/api/orders");
    }

    public Response get(String orderNumber) {
        return given()
                .spec(Specs.withToken(token))
                .pathParam("orderNumber", orderNumber)
                .when()
                .get("/api/orders/{orderNumber}");
    }

    public Response cancel(String orderNumber) {
        return given()
                .spec(Specs.withToken(token))
                .pathParam("orderNumber", orderNumber)
                .when()
                .post("/api/orders/{orderNumber}/cancel");
    }
}
