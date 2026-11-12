package com.shoplab.tests.api.client;

import com.shoplab.tests.api.Specs;
import com.shoplab.tests.api.model.AuthModels.LoginRequest;
import com.shoplab.tests.api.model.AuthModels.RegisterRequest;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/** Register, log in, log out, "who am I". See {@link ProductsClient} for what an API client is. */
public class AuthClient {

    public Response register(RegisterRequest body) {
        return given()
                .spec(Specs.anonymous())
                .body(body) // the record is turned into JSON by Jackson
                .when()
                .post("/api/auth/register");
    }

    public Response login(LoginRequest body) {
        return given()
                .spec(Specs.anonymous())
                .body(body)
                .when()
                .post("/api/auth/login");
    }

    public Response login(String email, String password) {
        return login(new LoginRequest(email, password));
    }

    /** token = null sends no Authorization header. */
    public Response logout(String token) {
        return given()
                .spec(Specs.withToken(token))
                .when()
                .post("/api/auth/logout");
    }

    public Response me(String token) {
        return given()
                .spec(Specs.withToken(token))
                .when()
                .get("/api/users/me");
    }
}
