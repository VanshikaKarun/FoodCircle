package com.vkarun.foodcircle.ai;

import com.vkarun.foodcircle.common.AppConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Calls the REAL Ollama on this laptop (D41, test option B, user decision 2026-11-28).
// No Spring context: OllamaClient reads AppConstants.Ollama, so we simply create it.
// If Ollama is not running, every test in this class is SKIPPED, not failed.
class OllamaClientIntegrationTest {

    private static OllamaClient client;

    @BeforeAll
    static void requireRunningOllama() {
        assumeTrue(ollamaIsRunning(),
                "Ollama is not running on " + AppConstants.Ollama.BASE_URL + ", skipping");

        client = new OllamaClient();
    }

    // Check 1: a real vector comes back, with the size nomic-embed-text gives (768, measured 2026-11-27).
    // A wrong URL, a wrong JSON field or a different model would break this.
    @Test
    void embed_returns768Numbers() {
        float[] vector = client.embed("quiet cafe with good filter coffee");

        assertThat(vector).hasSize(768);
    }

    // Check 2: the vector's length is about 1 ("unit length"). Then cosine similarity is just the dot product.
    // The Ollama docs do not promise this, so the test tells us if a model or Ollama update changes it.
    @Test
    void embed_returnsVectorOfLengthAboutOne() {
        float[] vector = client.embed("quiet cafe with good filter coffee");

        assertThat(length(vector)).isCloseTo(1.0, within(0.01));
    }

    // Length of a vector = square root of the sum of its squared numbers (Pythagoras in 768 dimensions).
    private static double length(float[] vector) {
        double sumOfSquares = 0;
        for (float x : vector) {
            sumOfSquares += x * x;
        }
        return Math.sqrt(sumOfSquares);
    }

    // Asks Ollama for its version with a 1-second limit. Any answer = running; refused or slow = not running.
    private static boolean ollamaIsRunning() {
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(AppConstants.Ollama.BASE_URL + "/api/version"))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
