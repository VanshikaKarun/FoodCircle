package com.vkarun.foodcircle.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// The JSON body of POST /api/places/{id}/reviews. Both ratings are optional (D13) but at least one is required.
public record CreateReviewRequest(
        @Min(1) @Max(5) Integer foodRating,
        @Min(1) @Max(5) Integer ambienceRating,
        @Size(max = 2000) String text,
        @Valid @Size(max = 20) List<DishRequest> dishes) {

    // Bean Validation treats isXxx() as a property, so this is checked together with the other fields.
    @AssertTrue(message = "at least one of foodRating or ambienceRating is required")
    public boolean isAtLeastOneRatingGiven() {
        return foodRating != null || ambienceRating != null;
    }

    // A dish the reviewer tried; liked is optional (null = no opinion given).
    public record DishRequest(@NotBlank @Size(max = 100) String name, Boolean liked) {
    }
}
