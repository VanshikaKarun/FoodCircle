package com.vkarun.foodcircle.ai;

// Body of POST /api/embed (Ollama). Jackson writes it as {"model": "...", "input": "..."}.
// One text per call (user decision 2026-11-27).
public record EmbedRequest(String model, String input) {
}
