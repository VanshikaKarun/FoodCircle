package com.vkarun.foodcircle.dto;

// What one import run did, for the log line and for tests.
public record ImportResult(int added, int updated, int skipped) {
}
