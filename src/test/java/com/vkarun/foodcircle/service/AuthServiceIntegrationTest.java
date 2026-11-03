package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.common.JwtConfig;
import com.vkarun.foodcircle.dto.CreateUserRequest;
import com.vkarun.foodcircle.dto.LoginRequest;
import com.vkarun.foodcircle.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Real service, BCrypt, JWT encoder/decoder and PostgreSQL (foodcircle_test); every test rolls back.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    private Long newUser(String username, String password) {
        return userService.create(new CreateUserRequest(username, "Name of " + username, password)).id();
    }

    private String messageOf(Throwable t) {
        assertThat(((ApiException) t).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        return t.getMessage();
    }

    @Test
    void login_rightPassword_givesATokenThatNamesTheUser() {
        Long ashaId = newUser("asha", "password123");

        LoginResponse response = authService.login(new LoginRequest("asha", "password123"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        Jwt jwt = jwtDecoder.decode(response.accessToken());    // throws if signature, expiry or issuer are wrong
        assertThat(jwt.getSubject()).isEqualTo(ashaId.toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtConfig.ISSUER);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
    }

    @Test
    void wrongPassword_andUnknownUsername_giveTheSame401() {
        newUser("asha", "password123");

        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> authService.login(new LoginRequest("asha", "password124")));
        Throwable unknownUser = org.assertj.core.api.Assertions.catchThrowable(
                () -> authService.login(new LoginRequest("nobody", "password123")));

        assertThat(messageOf(wrongPassword)).isEqualTo(AuthService.INVALID_LOGIN);
        assertThat(messageOf(unknownUser)).isEqualTo(messageOf(wrongPassword));
    }

    @Test
    void usernamesAreCaseSensitive_atLoginToo() {
        newUser("asha", "password123");

        assertThatThrownBy(() -> authService.login(new LoginRequest("Asha", "password123")))
                .isInstanceOf(ApiException.class);
    }

    // The signature at work: change one claim and the token is worthless.
    @Test
    void aTokenWithAChangedPayload_isRejected() {
        newUser("asha", "password123");
        String token = authService.login(new LoginRequest("asha", "password123")).accessToken();
        String[] parts = token.split("\\.");

        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"foodcircle\",\"sub\":\"999\",\"exp\":" + (Instant.now().getEpochSecond() + 3600) + "}")
                        .getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];    // old signature, new payload

        assertThatThrownBy(() -> jwtDecoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTokenSignedWithAnotherKey_isRejected() {
        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        JwtEncoder attackerEncoder = NimbusJwtEncoder
                .withSecretKey(new SecretKeySpec(otherKey, "HmacSHA256")).algorithm(MacAlgorithm.HS256).build();

        String token = encode(attackerEncoder, JwtConfig.ISSUER, Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void anExpiredToken_isRejected() {
        // Two minutes ago: past the decoder's 60-second clock-skew allowance.
        String token = encode(jwtEncoder, JwtConfig.ISSUER, Instant.now().minusSeconds(120));

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTokenFromAnotherIssuer_isRejected() {
        String token = encode(jwtEncoder, "someone-else", Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private static String encode(JwtEncoder encoder, String issuer, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).subject("1")
                .issuedAt(expiresAt.minusSeconds(3600)).expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
