package com.vkarun.foodcircle.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

// foodcircle.jwt.* from application.yaml (D38). Validated at startup: a missing secret stops the app
// with a clear message instead of failing on the first login.
@Validated
@ConfigurationProperties("foodcircle.jwt")
public record JwtProperties(
        // Base64 of at least 32 random bytes (256 bits, the HS256 minimum). Comes from env FOODCIRCLE_JWT_SECRET only.
        @NotBlank String secret,
        // How long an access token is valid, e.g. 1h.
        @NotNull Duration expiry) {

    // Records print every field in toString(); the secret must never reach a log.
    @Override
    public String toString() {
        return "JwtProperties[secret=***, expiry=" + expiry + "]";
    }
}
