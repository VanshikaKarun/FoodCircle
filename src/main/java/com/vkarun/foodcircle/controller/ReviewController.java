package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.CreateReviewRequest;
import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@Tag(name = "Reviews")
@RestController
@RequestMapping("/api/places/{placeId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Review a place (the author is the logged-in user)",
            description = "Food and ambience ratings are each 1 to 5; at least one is required. 409 if the place is closed.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable Long placeId,
                                 @AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(Long.valueOf(Objects.requireNonNull(jwt.getSubject())), placeId, request);
    }

    // The newest reviews of a place. Page-by-page browsing (cursors) comes with the feed on day 5.
    @Operation(summary = "The newest reviews of a place")
    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long placeId,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return reviewService.listForPlace(placeId, limit);
    }
}
