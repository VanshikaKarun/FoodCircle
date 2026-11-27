package com.vkarun.foodcircle.common;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// foodcircle.jwt.secret from application.yaml (D38), filled from env FOODCIRCLE_JWT_SECRET.
// Only the secret lives here: it must not be in a file. The token expiry is AppConstants.Auth.TOKEN_EXPIRY.
// Validated at startup: a missing secret stops the app
// with a clear message instead of failing on the first login.
@Validated
@ConfigurationProperties("foodcircle.jwt")
public record JwtProperties(
        // Base64 of at least 32 random bytes (256 bits, the HS256 minimum). Comes from env FOODCIRCLE_JWT_SECRET only.
        @NotBlank String secret) {

    // Records print every field in toString(); the secret must never reach a log.
    @Override
    public String toString() {
        return "JwtProperties[secret=***]";
    }
}
