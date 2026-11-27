package com.vkarun.foodcircle.ai;

import com.vkarun.foodcircle.common.AppConstants;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Talks to Ollama over plain HTTP (D41). Knows Ollama's JSON, nothing about places or reviews.
// URL, models and timeout come from AppConstants.Ollama.
@Component
public class OllamaClient {

    private final RestClient restClient;

    public OllamaClient() {
        // How long to wait: to connect, and for the answer. After that the call throws instead of hanging.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(AppConstants.Ollama.TIMEOUT);
        requestFactory.setReadTimeout(AppConstants.Ollama.TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(AppConstants.Ollama.BASE_URL)          // http://localhost:11434
                .requestFactory(requestFactory)
                .build();
    }

    // Turns one text into its vector (768 numbers for nomic-embed-text).
    public float[] embed(String text) {
        EmbedResponse response = restClient.post()
                .uri("/v1/embeddings")                                                // OpenAI-compatible endpoint
                .body(new EmbedRequest(AppConstants.Ollama.EMBEDDING_MODEL, text))   // sent as JSON
                .retrieve()                                                           // a 4xx/5xx from Ollama throws here
                .body(EmbedResponse.class);                                           // JSON -> record

        if (response == null || response.data() == null || response.data().isEmpty()
                || response.data().get(0).embedding() == null) {
            throw new IllegalStateException("Ollama returned no embedding");
        }
        return response.data().get(0).embedding();    // one text in, so exactly one vector out
    }
}
