package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.common.JwtConfig;
import com.vkarun.foodcircle.common.JwtProperties;
import com.vkarun.foodcircle.dto.LoginRequest;
import com.vkarun.foodcircle.dto.LoginResponse;
import com.vkarun.foodcircle.entity.User;
import com.vkarun.foodcircle.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

// Login: check username + password, then issue a signed access token (D38).
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // One message for both "no such user" and "wrong password", so a caller cannot find out which usernames exist.
    static final String INVALID_LOGIN = "Invalid username or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    // Checked when the username does not exist, so that case costs one BCrypt check too and takes about as long
    // as a wrong password. Otherwise the response time would reveal which usernames exist. Not a secret: no user has it.
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByUsername(request.username());
        String hashToCheck = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user.isEmpty() || !passwordMatches) {
            // Not logged here: ApiExceptionHandler logs every 4xx once at DEBUG.
            throw ApiException.unauthorized(INVALID_LOGIN);
        }

        String token = issueToken(user.get().getId());
        log.info("user {} logged in", user.get().getId());
        return LoginResponse.bearer(token, jwtProperties.expiry().toSeconds());
    }

    // The token says only who the caller is (sub = user id) and when it stops working.
    // Nothing private goes in: the payload is Base64, readable by anyone; only the signature is protected.
    private String issueToken(Long userId) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);    // JWT times are whole seconds
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.expiry()))
                .build();
        // The header must name HS256; without it Spring assumes RS256 and finds no matching key.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
