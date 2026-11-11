package com.shoplab.auth;

import com.shoplab.common.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request and response bodies for authentication. The annotations are validation rules:
 * if one fails, the request is rejected with 400 and the message below, before our code runs.
 * The messages are the same as in the frontend forms.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 100, message = "Name must be at least 2 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Size(max = 254, message = "Email is too long")
            @Pattern(regexp = Validation.EMAIL, message = "Enter a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            @Size(max = 72, message = "Password is too long")
            @Pattern(regexp = Validation.PASSWORD,
                    message = "Password must be at least 8 characters and contain an uppercase letter and a digit")
            String password
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Pattern(regexp = Validation.EMAIL, message = "Enter a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {
    }

    public record UserDto(Long id, String email, String name) {
        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getEmail(), u.getName());
        }
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
