package com.shoplab.auth;

import com.shoplab.auth.AuthDtos.AuthResponse;
import com.shoplab.auth.AuthDtos.LoginRequest;
import com.shoplab.auth.AuthDtos.RegisterRequest;
import com.shoplab.auth.AuthDtos.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth & users", description = "Register, log in, log out, current user")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an account (returns a token: you are logged in right away)")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/api/auth/login")
    @Operation(summary = "Log in and get a token",
            description = "401 = wrong email/password, 403 = locked account. slow@shoplab.test takes 3 seconds.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Log out: the token stops working")
    public void logout(@Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        service.logout(authorization);
    }

    @GetMapping("/api/users/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Who am I? (the user that owns the token)")
    public UserDto me(@CurrentUser User user) {
        return UserDto.from(user);
    }
}
