package com.vkarun.foodcircle.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorTest {

    @Test
    void encodeThenDecode_givesTheSameCursor() {
        Cursor cursor = new Cursor(Instant.parse("2026-10-27T10:15:30.123456Z"), 42);

        assertThat(Cursor.decode(cursor.encode())).isEqualTo(cursor);
    }

    @Test
    void theEncodedTextIsUrlSafe() {
        String text = new Cursor(Instant.parse("2026-10-27T10:15:30.123456Z"), 9_999_999_999L).encode();

        assertThat(text).matches("[A-Za-z0-9_-]+");   // no '+', '/' or '=' that would need escaping in a URL
    }

    @Test
    void microsecondPrecisionSurvives() {
        Instant a = Instant.parse("2026-10-27T10:15:30.000001Z");
        Instant b = Instant.parse("2026-10-27T10:15:30.000002Z");

        assertThat(Cursor.decode(new Cursor(a, 1).encode()).createdAt()).isEqualTo(a).isNotEqualTo(b);
    }

    @Test
    void decode_rejectsTextThatIsNotOurCursor() {
        String noColon = Base64.getUrlEncoder().withoutPadding().encodeToString("12345".getBytes(UTF_8));
        String notNumbers = Base64.getUrlEncoder().withoutPadding().encodeToString("abc:def".getBytes(UTF_8));

        for (String bad : new String[]{"", "!!!", "not a cursor", noColon, notNumbers}) {
            assertThatThrownBy(() -> Cursor.decode(bad)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
