package com.vkarun.foodcircle.common;

import java.time.Duration;

// Shared fixed values of the app, grouped by area (user decision 2026-11-28; replaces the foodcircle.ollama
// yaml block, the JWT expiry setting and the constants that lived in JwtConfig, AuthService, Geohash and Haversine).
// Limits that belong to one class only (OSM text lengths, import batch size, minimum search length) stay in that class.
// Changing a value here means recompiling and restarting the app.
//
// NOT here, on purpose: secrets (DB password, JWT secret) stay environment variables, never in a file;
// Spring's own settings (datasource, JPA, logging) stay in application.yaml.
public final class AppConstants {

    private AppConstants() {
    }

    // Ollama, the local model server (D41).
    public static final class Ollama {
        private Ollama() {
        }

        public static final String BASE_URL = "http://localhost:11434";
        // Turns text into a vector (768 numbers, measured 2026-11-27).
        public static final String EMBEDDING_MODEL = "nomic-embed-text";
        // Understands the question and writes the answer.
        public static final String CHAT_MODEL = "llama3.2:3b";
        // How long to wait for one Ollama call before giving up.
        public static final Duration TIMEOUT = Duration.ofSeconds(30);
    }

    // Login and JWT (D38).
    public static final class Auth {
        private Auth() {
        }

        // The "iss" claim we write into every token and require when checking one.
        public static final String ISSUER = "foodcircle";
        // HS256 needs a key of at least 256 bits = 32 bytes.
        public static final int MIN_KEY_BYTES = 32;
        // How long an access token is valid (no refresh token).
        public static final Duration TOKEN_EXPIRY = Duration.ofHours(1);
        // The same message for an unknown username and a wrong password, so neither is revealed.
        public static final String INVALID_LOGIN = "Invalid username or password";
    }

    // Geohash and distances (D24, D40).
    public static final class Geo {
        private Geo() {
        }

        // The 32 symbols, in value order 0..31: digits and lower-case letters except a, i, l, o.
        public static final String GEOHASH_ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz";
        // How many characters we store per place (about 38 m x 19 m cells). Searches use a shorter prefix of it.
        public static final int GEOHASH_STORED_LENGTH = 8;
        // Mean Earth radius in km (the usual value for haversine). The Earth is not a perfect sphere,
        // so results can be off by a fraction of a percent, which is fine for "places within 2 km".
        public static final double EARTH_RADIUS_KM = 6371.0088;
    }
}
