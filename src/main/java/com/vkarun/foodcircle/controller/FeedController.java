package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.CursorPage;
import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@Tag(name = "Feed")
@RestController
@RequestMapping("/api")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    // Reviews from the people I follow. Send the previous response's nextCursor as ?cursor= to get the next page.
    @Operation(summary = "Reviews from the people I follow, newest first",
            description = "Send the previous response's nextCursor as the cursor parameter to get the next page; nextCursor is null on the last page.")
    @GetMapping("/feed")
    public CursorPage<ReviewResponse> feed(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required = false) String cursor,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return feedService.feed(Long.valueOf(Objects.requireNonNull(jwt.getSubject())), cursor, limit);
    }

    // A user's own reviews; to see mine, pass my own id.
    @Operation(summary = "One user's reviews, newest first", description = "To see your own reviews, pass your own id.")
    @GetMapping("/users/{id}/reviews")
    public CursorPage<ReviewResponse> reviewsOf(@PathVariable Long id,
                                                @RequestParam(required = false) String cursor,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return feedService.reviewsOf(id, cursor, limit);
    }

    // "3 people you follow reviewed this": each followed user's latest review of the place.
    @Operation(summary = "People I follow who reviewed this place: their latest review each")
    @GetMapping("/places/{placeId}/friends-reviews")
    public List<ReviewResponse> friendsReviews(@PathVariable Long placeId,
                                               @AuthenticationPrincipal Jwt jwt) {
        return feedService.friendsReviews(Long.valueOf(Objects.requireNonNull(jwt.getSubject())), placeId);
    }
}
