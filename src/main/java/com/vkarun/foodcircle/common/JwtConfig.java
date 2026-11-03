package com.vkarun.foodcircle.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

// One secret key (HS256, D38 choice B) both signs tokens at login (encoder) and checks them on every request (decoder).
// Scaling note: with HS256 anything that can check a token can also create one; RS256 + a public JWKS endpoint fixes that.
@Configuration
public class JwtConfig {

    public static final String ISSUER = "foodcircle";

    static final int MIN_KEY_BYTES = 32;

    private final SecretKey key;

    public JwtConfig(JwtProperties properties) {
        this.key = secretKeyFrom(properties.secret());
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    // Used by Spring Security's resource server (step 3) to check "Authorization: Bearer ..." on each request.
    // Default checks: signature, exp and nbf (with 60 s clock skew); plus: iss must be ours.
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    // Fails at startup, with a message that never contains the secret, if it is not Base64 or too short.
    static SecretKey secretKeyFrom(String base64Secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Secret.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("foodcircle.jwt.secret (FOODCIRCLE_JWT_SECRET) is not valid Base64");
        }
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("foodcircle.jwt.secret (FOODCIRCLE_JWT_SECRET) must be at least "
                    + MIN_KEY_BYTES + " bytes for HS256, but is " + bytes.length);
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
