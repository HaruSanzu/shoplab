package com.shoplab.tests.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoplab.tests.config.TestConfig;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * A REQUEST SPECIFICATION is a reusable "template" for requests: base URL, content type, headers, filters.
 * Instead of repeating them in every test, each request starts from one of these templates.
 */
public final class Specs {

    private Specs() {
    }

    /**
     * How Java objects become JSON (request bodies) and JSON becomes Java objects (responses):
     * <ul>
     *   <li>NON_NULL: fields that are null are not sent at all ({"code": null} becomes {}).</li>
     *   <li>FAIL_ON_UNKNOWN_PROPERTIES = false: if the API adds a new field to a response,
     *       old tests keep working. They only check the fields they know about.</li>
     * </ul>
     */
    private static final RestAssuredConfig CONFIG = RestAssured.config().objectMapperConfig(
            ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory((type, charset) -> new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)));

    static {
        // If a REST Assured check (.then().statusCode(...)) fails, print the request and response to the console.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /** For public endpoints (catalog, login, register, guest cart preview). */
    public static RequestSpecification anonymous() {
        return new RequestSpecBuilder()
                .addRequestSpecification(technical())
                // Attaches each request and response to the Allure report
                .addFilter(new AllureRestAssured())
                .build();
    }

    /**
     * Same as {@link #anonymous()} but NOT recorded in the Allure report.
     * For housekeeping calls made outside a test (waiting for the API, resetting the DB in @BeforeAll):
     * Allure has no test to attach them to and would log "no test is running".
     */
    public static RequestSpecification technical() {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.apiUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(CONFIG)
                .build();
    }

    /**
     * For endpoints that need a logged-in user: adds "Authorization: Bearer &lt;token&gt;".
     * A null token means "send no Authorization header": used by tests that check the 401 error.
     */
    public static RequestSpecification withToken(String token) {
        RequestSpecBuilder builder = new RequestSpecBuilder().addRequestSpecification(anonymous());
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        return builder.build();
    }
}
