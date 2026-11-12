package com.shoplab.tests.api;

import com.shoplab.tests.api.client.AuthClient;
import com.shoplab.tests.api.model.AuthModels.AuthResponse;
import com.shoplab.tests.api.model.AuthModels.RegisterRequest;
import com.shoplab.tests.api.model.AuthModels.User;
import com.shoplab.tests.api.model.ErrorResponse;
import com.shoplab.tests.data.TestData;
import com.shoplab.tests.data.TestData.Users;
import com.shoplab.tests.data.TestUsers;
import com.shoplab.tests.data.TestUsers.LoggedInUser;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@Feature("Authentication")
@DisplayName("Auth API")
class AuthApiTest extends BaseApiTest {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String PASSWORD_RULE =
            "Password must be at least 8 characters and contain an uppercase letter and a digit";

    private final AuthClient auth = new AuthClient();

    // ---------- login ----------

    @Test
    @Tag("smoke")
    @DisplayName("Standard user can log in and gets a token")
    void loginStandardUser() {
        AuthResponse response = expect(auth.login(Users.STANDARD, TestData.PASSWORD), 200, AuthResponse.class);

        // The token is 32 random bytes written as 64 hex characters
        assertThat(response.token()).matches("^[0-9a-f]{64}$");
        assertThat(response.user().email()).isEqualTo(Users.STANDARD);
        assertThat(response.user().name()).isEqualTo("Standard User");
        assertThat(response.user().id()).isPositive();
    }

    @Test
    @DisplayName("Email at login is case-insensitive")
    void loginEmailIsCaseInsensitive() {
        AuthResponse response = expect(auth.login("STANDARD@ShopLab.test", TestData.PASSWORD), 200, AuthResponse.class);

        assertThat(response.user().email()).isEqualTo(Users.STANDARD);
    }

    @Test
    @DisplayName("Every login gives a new, different token")
    void eachLoginGivesNewToken() {
        String first = expect(auth.login(Users.STANDARD, TestData.PASSWORD), 200, AuthResponse.class).token();
        String second = expect(auth.login(Users.STANDARD, TestData.PASSWORD), 200, AuthResponse.class).token();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Wrong password gives 401")
    void wrongPassword() {
        assertThat(expectError(auth.login(Users.STANDARD, "WrongPassword1"), 401).message())
                .isEqualTo(INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Unknown email gives the SAME 401 as a wrong password")
    void unknownEmail() {
        // Same message on purpose: an attacker must not learn which emails are registered.
        assertThat(expectError(auth.login("nobody@shoplab.test", TestData.PASSWORD), 401).message())
                .isEqualTo(INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Password is case-sensitive")
    void passwordIsCaseSensitive() {
        assertThat(expectError(auth.login(Users.STANDARD, "password123"), 401).message())
                .isEqualTo(INVALID_CREDENTIALS);
    }

    @Test
    @Tag("smoke")
    @DisplayName("Locked user gets 403")
    void lockedUser() {
        assertThat(expectError(auth.login(Users.LOCKED, TestData.PASSWORD), 403).message())
                .isEqualTo("Sorry, this account has been locked. Please contact support.");
    }

    @Test
    @DisplayName("Locked user with a wrong password gets 401, not 403")
    void lockedUserWithWrongPassword() {
        // The password is checked first, so we don't reveal that the account exists and is locked.
        expectError(auth.login(Users.LOCKED, "WrongPassword1"), 401);
    }

    @Test
    @Tag("slow")
    @DisplayName("Slow user logs in successfully, but it takes at least 3 seconds")
    void slowUserLogin() {
        Response response = auth.login(Users.SLOW, TestData.PASSWORD);

        expect(response, 200, AuthResponse.class);
        // REST Assured measures how long the request took
        assertThat(response.timeIn(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(3000);
    }

    @ParameterizedTest(name = "email=\"{0}\", password=\"{1}\"")
    @DisplayName("Login form validation gives 400 with field errors")
    @CsvSource(delimiter = '|', value = {
            // email           | password    | field    | message
            "''                | Password123 | email    | Email is required",
            "not-an-email      | Password123 | email    | Enter a valid email address",
            "standard@shoplab  | Password123 | email    | Enter a valid email address",
            "standard@shoplab.test | ''      | password | Password is required",
    })
    void loginValidation(String email, String password, String field, String message) {
        ErrorResponse error = expectError(auth.login(email, password), 400);

        assertThat(error.message()).isEqualTo("Validation failed");
        assertThat(error.fieldErrors()).containsEntry(field, message);
    }

    // ---------- register ----------

    @Test
    @Tag("smoke")
    @DisplayName("New user can register and is logged in right away")
    void registerNewUser() {
        String email = TestUsers.uniqueEmail();

        Response response = auth.register(new RegisterRequest("Jane Tester", email, "Secret123"));

        AuthResponse body = expect(response, 201, AuthResponse.class);
        assertThat(body.token()).isNotBlank();
        assertThat(body.user().email()).isEqualTo(email);
        assertThat(body.user().name()).isEqualTo("Jane Tester");

        // The token from registration works right away
        User me = expect(auth.me(body.token()), 200, User.class);
        assertThat(me.email()).isEqualTo(email);
    }

    @Test
    @DisplayName("Registered user can log in with the same email and password")
    void registeredUserCanLogIn() {
        LoggedInUser user = TestUsers.registerNew();

        AuthResponse response = expect(auth.login(user.email(), user.password()), 200, AuthResponse.class);

        assertThat(response.user().email()).isEqualTo(user.email());
    }

    @Test
    @DisplayName("Spaces around the name are removed on registration")
    void registerTrimsName() {
        AuthResponse body = expect(auth.register(new RegisterRequest("  Jane  ", TestUsers.uniqueEmail(), "Secret123")),
                201, AuthResponse.class);

        assertThat(body.user().name()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("Email with spaces around it is rejected by the API (the website trims it before sending)")
    void registerEmailWithSpaces() {
        ErrorResponse error = expectError(
                auth.register(new RegisterRequest("Jane", "  " + TestUsers.uniqueEmail() + "  ", "Secret123")), 400);

        assertThat(error.fieldErrors()).containsEntry("email", "Enter a valid email address");
    }

    @Test
    @DisplayName("Registering an existing email gives 409")
    void registerDuplicateEmail() {
        ErrorResponse error = expectError(auth.register(new RegisterRequest("Copy Cat", Users.STANDARD, "Secret123")), 409);

        assertThat(error.message()).isEqualTo("An account with this email already exists");
        assertThat(error.fieldErrors()).containsEntry("email", "An account with this email already exists");
    }

    @Test
    @DisplayName("Duplicate email check is case-insensitive")
    void registerDuplicateEmailDifferentCase() {
        expectError(auth.register(new RegisterRequest("Copy Cat", Users.STANDARD.toUpperCase(), "Secret123")), 409);
    }

    @ParameterizedTest(name = "{3}: name=\"{0}\", email=\"{1}\", password=\"{2}\"")
    @DisplayName("Registration form validation gives 400 with field errors")
    @CsvSource(delimiter = '|', value = {
            // name   | email              | password    | field    | message
            "''       | new@shoplab.test   | Secret123   | name     | Name is required",
            "A        | new@shoplab.test   | Secret123   | name     | Name must be at least 2 characters",
            "Jane     | ''                 | Secret123   | email    | Email is required",
            "Jane     | jane.example.com   | Secret123   | email    | Enter a valid email address",
            "Jane     | new@shoplab.test   | ''          | password | Password is required",
            "Jane     | new@shoplab.test   | Short1      | password | " + PASSWORD_RULE,
            "Jane     | new@shoplab.test   | secret123   | password | " + PASSWORD_RULE,
            "Jane     | new@shoplab.test   | SecretWord  | password | " + PASSWORD_RULE,
    })
    void registerValidation(String name, String email, String password, String field, String message) {
        ErrorResponse error = expectError(auth.register(new RegisterRequest(name, email, password)), 400);

        assertThat(error.message()).isEqualTo("Validation failed");
        assertThat(error.fieldErrors()).containsEntry(field, message);
    }

    @Test
    @DisplayName("Empty registration body reports every missing field")
    void registerEmptyBody() {
        ErrorResponse error = expectError(auth.register(new RegisterRequest(null, null, null)), 400);

        assertThat(error.fieldErrors()).containsOnly(
                entry("name", "Name is required"),
                entry("email", "Email is required"),
                entry("password", "Password is required"));
    }

    // ---------- current user and logout ----------

    @Test
    @DisplayName("/users/me returns the owner of the token")
    void meReturnsCurrentUser() {
        LoggedInUser user = TestUsers.registerNew();

        User me = expect(auth.me(user.token()), 200, User.class);

        assertThat(me.email()).isEqualTo(user.email());
        assertThat(me.name()).isEqualTo(user.name());
    }

    @Test
    @DisplayName("/users/me without a token gives 401")
    void meWithoutToken() {
        assertThat(expectError(auth.me(null), 401).message()).isEqualTo("Authentication required");
    }

    @Test
    @DisplayName("/users/me with an unknown token gives 401")
    void meWithInvalidToken() {
        assertThat(expectError(auth.me("this-token-does-not-exist"), 401).message()).isEqualTo("Invalid or expired token");
    }

    @Test
    @Tag("smoke")
    @DisplayName("After logout the token no longer works")
    void logoutInvalidatesToken() {
        LoggedInUser user = TestUsers.registerNew();

        auth.logout(user.token()).then().statusCode(204);

        assertThat(expectError(auth.me(user.token()), 401).message()).isEqualTo("Invalid or expired token");
    }

    @Test
    @DisplayName("Logout only ends ONE session: other tokens of the same user still work")
    void logoutEndsOnlyOneSession() {
        LoggedInUser user = TestUsers.registerNew();
        String secondToken = expect(auth.login(user.email(), user.password()), 200, AuthResponse.class).token();

        auth.logout(user.token()).then().statusCode(204);

        expect(auth.me(secondToken), 200, User.class);
    }

    @Test
    @DisplayName("Logging out twice with the same token gives 401 the second time")
    void logoutTwice() {
        LoggedInUser user = TestUsers.registerNew();
        auth.logout(user.token()).then().statusCode(204);

        expectError(auth.logout(user.token()), 401);
    }

    @Test
    @DisplayName("Logout without a token gives 401")
    void logoutWithoutToken() {
        expectError(auth.logout(null), 401);
    }
}
