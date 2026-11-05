package com.vkarun.foodcircle.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Who may call what (D38 choice E). Spring Security checks every request against these rules, top to bottom; the first match wins.
// Tokens are checked by Spring's OAuth2 resource server using the JwtDecoder from JwtConfig (D38 choice A).
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is for credentials the browser adds by itself (cookies). A Bearer token is never
                // added automatically, so a forged cross-site request arrives without one and gets a 401 anyway.
                .csrf(csrf -> csrf.disable())
                // No HTTP session and no JSESSIONID cookie: every request must carry its own token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Sign-up and login must work without a token, or nobody could ever get one.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()
                        // The two GETs that are about "me". They must come BEFORE the "all GETs are public" rule below.
                        .requestMatchers(HttpMethod.GET, "/api/feed", "/api/places/*/friends-reviews").authenticated()
                        // Every other read is public.
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Spring forwards failures to /error; if it were protected, a 500 would show up as a 401.
                        .requestMatchers("/error").permitAll()
                        // Everything else (follow, unfollow, post a review, any new endpoint) needs a valid token.
                        // Safe default: a new endpoint is private until someone deliberately makes it public.
                        .anyRequest().authenticated())
                // Read "Authorization: Bearer <token>" and check it with our JwtDecoder (signature, exp, iss).
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
