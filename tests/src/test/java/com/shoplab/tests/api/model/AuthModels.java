package com.shoplab.tests.api.model;

/**
 * JSON bodies of the auth endpoints, written as Java RECORDS.
 * A record is a short way to write a class that only holds data:
 * {@code record LoginRequest(String email, String password)} gives a constructor,
 * getters {@code email()} / {@code password()}, equals() and toString() for free.
 * <p>
 * The tests have their OWN copies of these classes (they don't import the backend's).
 * That is on purpose: the tests check the API from the outside, like any other client.
 * If a developer renames a JSON field by mistake, the tests notice.
 */
public final class AuthModels {

    private AuthModels() {
    }

    public record RegisterRequest(String name, String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record User(Long id, String email, String name) {
    }

    public record AuthResponse(String token, User user) {
    }
}
