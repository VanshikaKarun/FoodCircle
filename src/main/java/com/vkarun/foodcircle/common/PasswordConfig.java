package com.vkarun.foodcircle.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// The one place that decides how passwords are hashed (D38). Callers depend on the PasswordEncoder interface,
// so switching to another algorithm (e.g. Argon2id) changes only this method.
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
