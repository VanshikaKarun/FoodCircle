package com.vkarun.foodcircle.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

// "Continue after this review": the (createdAt, id) of the last item of a page. Handed to the client as an opaque
// text and sent back for the next page. createdAt is kept in microseconds, the precision the database stores.
public record Cursor(Instant createdAt, long id) {

    public String encode() {
        String raw = ChronoUnit.MICROS.between(Instant.EPOCH, createdAt) + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    // Anything that is not a cursor we produced (cut, edited, random text) is rejected with IllegalArgumentException.
    public static Cursor decode(String text) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8);
            int colon = raw.indexOf(':');
            if (colon <= 0) {
                throw new IllegalArgumentException("no separator");
            }
            long micros = Long.parseLong(raw.substring(0, colon));
            long id = Long.parseLong(raw.substring(colon + 1));
            return new Cursor(Instant.EPOCH.plus(micros, ChronoUnit.MICROS), id);
        } catch (IllegalArgumentException e) {   // also covers NumberFormatException and bad Base64
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }
}
