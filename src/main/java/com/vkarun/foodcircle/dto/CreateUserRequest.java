package com.vkarun.foodcircle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// The JSON body of POST /api/users. The limits match the users table (username 30, display name 60).
public record CreateUserRequest(
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "may only contain letters, digits and underscore")
        String username,

        @NotBlank @Size(max = 60)
        String displayName) {
}
