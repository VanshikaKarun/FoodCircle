package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.common.Cursor;
import com.vkarun.foodcircle.dto.CursorPage;
import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.entity.Review;
import com.vkarun.foodcircle.repository.PlaceRepository;
import com.vkarun.foodcircle.repository.ReviewRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// Reads of reviews that are about people (the feed, one user's reviews, friends' reviews of a place).
@Service
public class FeedService {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final UserService userService;

    public FeedService(ReviewRepository reviewRepository, PlaceRepository placeRepository, UserService userService) {
        this.reviewRepository = reviewRepository;
        this.placeRepository = placeRepository;
        this.userService = userService;
    }

    // Reviews from the people the caller follows (not the caller's own, and not strangers'), newest first.
    @Transactional(readOnly = true)
    public CursorPage<ReviewResponse> feed(Long currentUserId, String cursorText, int limit) {
        userService.requireCurrentUser(currentUserId);
        Cursor cursor = parse(cursorText);
        // Ask for one more row than the page needs: if it exists there is a next page, and it tells us where the cursor goes.
        Limit fetch = Limit.of(limit + 1);
        List<Review> rows = cursor == null
                ? reviewRepository.findFeedFirstPage(currentUserId, fetch)
                : reviewRepository.findFeedAfter(currentUserId, cursor.createdAt(), cursor.id(), fetch);
        return toPage(rows, limit);
    }

    // One user's reviews, newest first. For the caller's own reviews, pass their own id.
    @Transactional(readOnly = true)
    public CursorPage<ReviewResponse> reviewsOf(Long userId, String cursorText, int limit) {
        userService.requireExisting(userId);
        Cursor cursor = parse(cursorText);
        Limit fetch = Limit.of(limit + 1);
        List<Review> rows = cursor == null
                ? reviewRepository.findByUserFirstPage(userId, fetch)
                : reviewRepository.findByUserAfter(userId, cursor.createdAt(), cursor.id(), fetch);
        return toPage(rows, limit);
    }

    // The people the caller follows who reviewed this place: their latest review of it each, newest first.
    @Transactional(readOnly = true)
    public List<ReviewResponse> friendsReviews(Long currentUserId, Long placeId) {
        userService.requireCurrentUser(currentUserId);
        placeRepository.findById(placeId)
                .orElseThrow(() -> ApiException.notFound("Place " + placeId + " not found"));
        return reviewRepository.findLatestReviewPerFollowee(placeId, currentUserId).stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).thenComparing(Review::getId).reversed())
                .map(ReviewResponse::from)
                .toList();
    }

    private CursorPage<ReviewResponse> toPage(List<Review> rows, int limit) {
        boolean hasMore = rows.size() > limit;
        List<Review> pageRows = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = null;
        if (hasMore) {
            Review last = pageRows.get(pageRows.size() - 1);
            nextCursor = new Cursor(last.getCreatedAt(), last.getId()).encode();
        }
        return new CursorPage<>(pageRows.stream().map(ReviewResponse::from).toList(), nextCursor);
    }

    private Cursor parse(String cursorText) {
        if (cursorText == null || cursorText.isBlank()) {
            return null;   // first page
        }
        try {
            return Cursor.decode(cursorText);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid cursor");
        }
    }
}
