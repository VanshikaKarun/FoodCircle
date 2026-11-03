package com.vkarun.foodcircle.dto;

// What a successful login returns. The client sends accessToken back as "Authorization: Bearer <accessToken>".
// expiresIn is in seconds (the OAuth 2 convention), so the client knows when to log in again.
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    public static LoginResponse bearer(String accessToken, long expiresInSeconds) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
