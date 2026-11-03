package com.vkarun.foodcircle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// The JSON body of POST /api/auth/login. No length rules here: a wrong-length password is simply a wrong password.
public record LoginRequest(
        @NotBlank String username,
        @NotBlank @Schema(format = "password") String password) {

    // Records print every field in toString(); keep the password out of logs.
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
