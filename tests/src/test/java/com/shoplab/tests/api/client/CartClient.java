package com.shoplab.tests.api.client;

import com.shoplab.tests.api.Specs;
import com.shoplab.tests.api.model.CartModels.AddItemRequest;
import com.shoplab.tests.api.model.CartModels.PreviewRequest;
import com.shoplab.tests.api.model.CartModels.PromoRequest;
import com.shoplab.tests.api.model.CartModels.QuantityRequest;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * The cart of ONE user. The token is given once, in the constructor, so tests read naturally:
 * <pre>
 *   CartClient cart = new CartClient(token);
 *   cart.addItem(7, 2);
 *   cart.applyPromo("QA10");
 * </pre>
 * Like a Page Object that is created for one browser session.
 */
public class CartClient {

    private final String token;

    /** token = null: a "not logged in" client, for 401 tests. */
    public CartClient(String token) {
        this.token = token;
    }

    public Response get() {
        return given()
                .spec(Specs.withToken(token))
                .when()
                .get("/api/cart");
    }

    public Response get(String deliveryMethod) {
        return given()
                .spec(Specs.withToken(token))
                .queryParam("deliveryMethod", deliveryMethod)
                .when()
                .get("/api/cart");
    }

    public Response addItem(long productId, int quantity) {
        return addItem(new AddItemRequest(productId, quantity));
    }

    /** Takes the whole body, so a negative test can send nulls. */
    public Response addItem(AddItemRequest body) {
        return given()
                .spec(Specs.withToken(token))
                .body(body)
                .when()
                .post("/api/cart/items");
    }

    public Response updateQuantity(long productId, Integer quantity) {
        return given()
                .spec(Specs.withToken(token))
                .pathParam("productId", productId)
                .body(new QuantityRequest(quantity))
                .when()
                .put("/api/cart/items/{productId}");
    }

    public Response removeItem(long productId) {
        return given()
                .spec(Specs.withToken(token))
                .pathParam("productId", productId)
                .when()
                .delete("/api/cart/items/{productId}");
    }

    public Response clear() {
        return given()
                .spec(Specs.withToken(token))
                .when()
                .delete("/api/cart");
    }

    public Response applyPromo(String code) {
        return given()
                .spec(Specs.withToken(token))
                .body(new PromoRequest(code))
                .when()
                .post("/api/cart/promo");
    }

    public Response removePromo() {
        return given()
                .spec(Specs.withToken(token))
                .when()
                .delete("/api/cart/promo");
    }

    /** Guest cart: no login needed, nothing is saved. Static because it doesn't belong to a user. */
    public static Response preview(PreviewRequest body) {
        return given()
                .spec(Specs.anonymous())
                .body(body)
                .when()
                .post("/api/cart/preview");
    }
}
