package com.vkarun.foodcircle.common;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The secret check that runs at startup. No Spring.
class JwtConfigTest {

    private static String base64OfBytes(int count) {
        return Base64.getEncoder().encodeToString(new byte[count]);
    }

    @Test
    void a32ByteSecret_isAccepted() {
        assertThat(JwtConfig.secretKeyFrom(base64OfBytes(32)).getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void aShortSecret_stopsTheApp_withoutPrintingIt() {
        String shortSecret = base64OfBytes(16);
        assertThatThrownBy(() -> JwtConfig.secretKeyFrom(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes")
                .hasMessageContaining("is 16")
                .message().doesNotContain(shortSecret);
    }

    @Test
    void aSecretThatIsNotBase64_stopsTheApp() {
        assertThatThrownBy(() -> JwtConfig.secretKeyFrom("not base64 !!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not valid Base64");
    }

    @Test
    void propertiesToString_hidesTheSecret() {
        JwtProperties properties = new JwtProperties("super-secret-value", java.time.Duration.ofHours(1));
        assertThat(properties.toString()).doesNotContain("super-secret-value").contains("PT1H");
    }
}
