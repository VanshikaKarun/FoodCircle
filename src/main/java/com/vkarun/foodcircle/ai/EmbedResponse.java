package com.vkarun.foodcircle.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Answer of POST /api/embed (Ollama). Only the fields we use; Ollama also sends model and load_duration.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbedResponse(
        // One vector per input text. We send one text, so the list holds one vector of 768 numbers.
        List<float[]> embeddings,
        // How many tokens the input was split into (8 for "quiet cafe with good filter coffee").
        @JsonProperty("prompt_eval_count") int promptEvalCount,
        // How long Ollama took, in nanoseconds (1 ms = 1,000,000 ns).
        @JsonProperty("total_duration") long totalDuration) {
}
