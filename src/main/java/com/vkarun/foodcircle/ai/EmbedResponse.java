package com.vkarun.foodcircle.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Answer of POST /v1/embeddings, Ollama's OpenAI-compatible endpoint (user decision 2026-11-28).
// Shape (seen on the real Ollama 2026-11-28):
// {"object":"list","data":[{"object":"embedding","embedding":[...768 numbers...],"index":0}],
//  "model":"nomic-embed-text","usage":{"prompt_tokens":8,"total_tokens":8}}
// Only the fields we use; "object" is skipped.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbedResponse(
        // One entry per input text. We send one text, so the list holds one entry.
        List<Item> data,
        // The model that made the vectors.
        String model,
        // Token counts. The OpenAI format has no timing fields; we time calls ourselves.
        Usage usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            // The vector: 768 numbers for nomic-embed-text.
            float[] embedding,
            // Which input text this vector belongs to (0 for the first).
            int index) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            // How many tokens the input was split into (8 for "quiet cafe with good filter coffee").
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("total_tokens") int totalTokens) {
    }
}
