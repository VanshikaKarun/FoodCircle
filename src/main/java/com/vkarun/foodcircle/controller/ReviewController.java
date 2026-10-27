package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.CreateReviewRequest;
import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/places/{placeId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // X-User-Id is the temporary "who am I" until real authentication in week 2.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable Long placeId,
                                 @RequestHeader("X-User-Id") Long currentUserId,
                                 @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(currentUserId, placeId, request);
    }

    // The newest reviews of a place. Page-by-page browsing (cursors) comes with the feed on day 5.
    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long placeId,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return reviewService.listForPlace(placeId, limit);
    }
}
