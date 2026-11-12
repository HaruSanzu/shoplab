package com.shoplab.tests.db;

import com.shoplab.tests.api.client.AuthClient;
import com.shoplab.tests.api.model.AuthModels.AuthResponse;
import com.shoplab.tests.api.model.AuthModels.RegisterRequest;
import com.shoplab.tests.data.TestData;
import com.shoplab.tests.data.TestData.Users;
import com.shoplab.tests.data.TestUsers;
import com.shoplab.tests.data.TestUsers.LoggedInUser;
import com.shoplab.tests.db.DbRows.UserRow;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Feature("Users in the database")
@DisplayName("Users and tokens (DB)")
class UsersDbTest extends BaseDbTest {

    private final AuthClient auth = new AuthClient();

    @Test
    @Tag("smoke")
    @DisplayName("Registration creates one row in users with the given name and email")
    void registrationCreatesUserRow() {
        Instant before = Instant.now();
        LoggedInUser user = TestUsers.registerNew();

        UserRow row = ShopDb.user(user.email());

        assertThat(row.email()).isEqualTo(user.email());
        assertThat(row.name()).isEqualTo(user.name());
        assertThat(row.locked()).as("new users are not locked").isFalse();
        assertThat(row.slowLogin()).isFalse();
        // created_at is filled by the database (DEFAULT now()). Allow a few seconds for clock differences.
        assertThat(row.createdAt()).isCloseTo(before, within(Duration.ofSeconds(10)));
    }

    @Test
    @Tag("smoke")
    @DisplayName("Password is stored as a BCrypt hash, never as plain text")
    void passwordIsHashed() {
        LoggedInUser user = TestUsers.registerNew();

        String hash = ShopDb.user(user.email()).passwordHash();

        assertThat(hash)
                .isNotEqualTo(user.password())
                .doesNotContain(user.password())
                // BCrypt format: $2a$10$ + 53 characters of salt and hash = 60 characters
                .matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }

    @Test
    @DisplayName("Same password gives a different hash for each user (salt)")
    void samePasswordDifferentHashes() {
        LoggedInUser first = TestUsers.registerNew();
        LoggedInUser second = TestUsers.registerNew();
        assertThat(first.password()).isEqualTo(second.password());

        assertThat(ShopDb.user(first.email()).passwordHash())
                .isNotEqualTo(ShopDb.user(second.email()).passwordHash());
    }

    @Test
    @DisplayName("Spaces around the name are not stored")
    void nameIsStoredTrimmed() {
        String email = TestUsers.uniqueEmail();
        auth.register(new RegisterRequest("  Jane  ", email, TestData.PASSWORD)).then().statusCode(201);

        assertThat(ShopDb.user(email).name()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("The id returned by the API is the id of the row in the database")
    void apiIdMatchesDatabaseId() {
        String email = TestUsers.uniqueEmail();

        AuthResponse response = expect(auth.register(new RegisterRequest("Jane", email, TestData.PASSWORD)),
                201, AuthResponse.class);

        assertThat(response.user().id()).isEqualTo(ShopDb.user(email).id());
    }

    @Test
    @DisplayName("Rejected registration (duplicate email) adds no row")
    void duplicateRegistrationAddsNoRow() {
        long usersBefore = ShopDb.countUsers();

        auth.register(new RegisterRequest("Copy", Users.STANDARD.toUpperCase(), TestData.PASSWORD)).then().statusCode(409);

        assertThat(ShopDb.countUsersWithEmail(Users.STANDARD)).isEqualTo(1);
        assertThat(ShopDb.countUsers()).isEqualTo(usersBefore);
    }

    @Test
    @DisplayName("Rejected registration (invalid password) adds no row")
    void invalidRegistrationAddsNoRow() {
        String email = TestUsers.uniqueEmail();

        auth.register(new RegisterRequest("Jane", email, "short")).then().statusCode(400);

        assertThat(ShopDb.findUser(email)).isEmpty();
    }

    @Test
    @DisplayName("Built-in test users have the expected flags")
    void builtInUsers() {
        assertThat(ShopDb.user(Users.STANDARD).locked()).isFalse();
        assertThat(ShopDb.user(Users.LOCKED).locked()).isTrue();
        assertThat(ShopDb.user(Users.SLOW).slowLogin()).isTrue();
    }

    // ---------- tokens ----------

    @Test
    @DisplayName("Login stores the token in auth_tokens, logout deletes it")
    void loginAndLogoutTokenRows() {
        LoggedInUser user = TestUsers.registerNew();
        String token = expect(auth.login(user.email(), user.password()), 200, AuthResponse.class).token();
        assertThat(ShopDb.tokenExists(token)).as("token row after login").isTrue();

        auth.logout(token).then().statusCode(204);

        assertThat(ShopDb.tokenExists(token)).as("token row after logout").isFalse();
    }

    @Test
    @DisplayName("Each login adds a token row; logout removes only its own")
    void tokensArePerSession() {
        LoggedInUser user = TestUsers.registerNew();                // registration = token 1
        auth.login(user.email(), user.password()).then().statusCode(200); // token 2
        assertThat(ShopDb.countTokensOf(user.email())).isEqualTo(2);

        auth.logout(user.token()).then().statusCode(204);

        assertThat(ShopDb.countTokensOf(user.email())).isEqualTo(1);
    }

    @Test
    @DisplayName("Failed login (wrong password, locked user) creates no token")
    void failedLoginCreatesNoToken() {
        long lockedTokensBefore = ShopDb.countTokensOf(Users.LOCKED);
        long standardTokensBefore = ShopDb.countTokensOf(Users.STANDARD);

        auth.login(Users.LOCKED, TestData.PASSWORD).then().statusCode(403);
        auth.login(Users.STANDARD, "WrongPassword1").then().statusCode(401);

        assertThat(ShopDb.countTokensOf(Users.LOCKED)).isEqualTo(lockedTokensBefore);
        assertThat(ShopDb.countTokensOf(Users.STANDARD)).isEqualTo(standardTokensBefore);
    }
}
