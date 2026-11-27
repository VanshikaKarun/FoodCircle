package com.vkarun.foodcircle.ai;

// Body of POST /v1/embeddings (Ollama's OpenAI-compatible endpoint; same two fields as OpenAI's own API). Jackson writes it as {"model": "...", "input": "..."}.
// One text per call (user decision 2026-11-27).
public record EmbedRequest(String model, String input) {
}
