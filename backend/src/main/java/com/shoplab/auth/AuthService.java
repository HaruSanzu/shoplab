package com.shoplab.auth;

import com.shoplab.auth.AuthDtos.AuthResponse;
import com.shoplab.auth.AuthDtos.LoginRequest;
import com.shoplab.auth.AuthDtos.RegisterRequest;
import com.shoplab.auth.AuthDtos.UserDto;
import com.shoplab.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;

@Service
public class AuthService {

    static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository users;
    private final AuthTokenRepository tokens;
    /** BCrypt: a slow one-way hash. We can check a password against it, but never get the password back. */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, AuthTokenRepository tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists",
                    Map.of("email", "An account with this email already exists"));
        }
        User user = users.save(new User(email, passwordEncoder.encode(req.password()), req.name().trim()));
        // Like the frontend: after registering you are logged in right away.
        return new AuthResponse(createToken(user), UserDto.from(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);

        if (user != null && user.isSlowLogin()) {
            sleep(3000); // deliberate: lets you practise timeouts and waits
        }
        // Same message for "no such user" and "wrong password", so attackers can't
        // find out which emails are registered.
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (user.isLocked()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Sorry, this account has been locked. Please contact support.");
        }
        return new AuthResponse(createToken(user), UserDto.from(user));
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null || !tokens.existsById(token)) {
            throw ApiException.unauthorized("Authentication required");
        }
        tokens.deleteById(token);
    }

    /** Finds the logged-in user from an "Authorization: Bearer xxx" header, or throws 401. */
    @Transactional(readOnly = true)
    public User authenticate(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
            throw ApiException.unauthorized("Authentication required");
        }
        return tokens.findWithUserByToken(token)
                .map(AuthToken::getUser)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired token"));
    }

    private String createToken(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes); // 64 random hex characters
        tokens.save(new AuthToken(token, user));
        return token;
    }

    private static String extractToken(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
