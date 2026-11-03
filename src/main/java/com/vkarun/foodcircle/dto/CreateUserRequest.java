package com.vkarun.foodcircle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

// The JSON body of POST /api/users. The limits match the users table (username 30, display name 60).
public record CreateUserRequest(
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "may only contain letters, digits and underscore")
        String username,

        @NotBlank @Size(max = 60)
        String displayName,

        // @Size counts characters. BCrypt's real limit is 72 BYTES, checked below.
        @NotBlank @Size(min = 8, max = 72)
        @Schema(format = "password")
        String password) {

    // BCrypt rejects more than 72 bytes (Spring Security 7.1.1 throws IllegalArgumentException, which would be a 500).
    // In UTF-8 a letter like "é" is 2 bytes and a Telugu letter 3, so 72 characters can be far more than 72 bytes.
    // Null is left to @NotBlank so a missing password is reported once.
    @AssertTrue(message = "must be at most 72 bytes (letters outside English can take 2-3 bytes each)")
    @Schema(hidden = true)
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    // Records generate toString() with every field; this keeps the password out of logs and error messages.
    @Override
    public String toString() {
        return "CreateUserRequest[username=" + username + ", displayName=" + displayName + ", password=***]";
    }
}
