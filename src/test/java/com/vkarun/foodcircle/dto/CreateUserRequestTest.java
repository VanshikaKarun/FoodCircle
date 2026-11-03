package com.vkarun.foodcircle.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Plain Bean Validation, no Spring: the same rules @Valid applies in the controller.
class CreateUserRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<String> invalidFields(String password) {
        Set<ConstraintViolation<CreateUserRequest>> violations =
                validator.validate(new CreateUserRequest("asha", "Asha K", password));
        return violations.stream().map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void aNormalPassword_isValid() {
        assertThat(invalidFields("password123")).isEmpty();
    }

    @Test
    void missingBlankOrShortPasswords_areInvalid() {
        assertThat(invalidFields(null)).containsExactly("password");     // reported once, not also as a byte error
        assertThat(invalidFields("        ")).contains("password");
        assertThat(invalidFields("1234567")).containsExactly("password"); // 7 characters
    }

    @Test
    void exactly72AsciiCharacters_isValid_but73IsNot() {
        assertThat(invalidFields("a".repeat(72))).isEmpty();
        assertThat(invalidFields("a".repeat(73))).contains("password");
    }

    // The bug this rule exists for: few characters, too many bytes.
    @Test
    void teluguPasswordUnder72Characters_butOver72Bytes_isInvalid() {
        String telugu = "అ".repeat(25);
        assertThat(telugu.length()).isEqualTo(25);
        assertThat(telugu.getBytes(StandardCharsets.UTF_8).length).isEqualTo(75);

        assertThat(invalidFields(telugu)).containsExactly("passwordWithinBcryptLimit");
    }

    @Test
    void toString_neverShowsThePassword() {
        assertThat(new CreateUserRequest("asha", "Asha K", "secret-pass-1").toString())
                .doesNotContain("secret-pass-1")
                .contains("asha");
    }
}
