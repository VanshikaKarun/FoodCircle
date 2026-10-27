package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.CreateReviewRequest;
import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.Review;
import com.vkarun.foodcircle.entity.User;
import com.vkarun.foodcircle.repository.PlaceRepository;
import com.vkarun.foodcircle.repository.ReviewRepository;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Temporary ResponseStatusExceptions again; day 6 replaces them with ProblemDetail handling.
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final UserService userService;

    public ReviewService(ReviewRepository reviewRepository, PlaceRepository placeRepository, UserService userService) {
        this.reviewRepository = reviewRepository;
        this.placeRepository = placeRepository;
        this.userService = userService;
    }

    // One transaction: the review and its dishes are saved together or not at all.
    @Transactional
    public ReviewResponse create(Long currentUserId, Long placeId, CreateReviewRequest request) {
        User user = userService.requireCurrentUser(currentUserId);
        Place place = findPlace(placeId);
        if (place.isClosed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Place " + placeId + " is closed");
        }

        Review review = new Review(user, place, request.foodRating(), request.ambienceRating(), request.text());
        if (request.dishes() != null) {
            for (CreateReviewRequest.DishRequest dish : request.dishes()) {
                review.addDish(dish.name().trim(), dish.liked());   // saved with the review (cascade)
            }
        }
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForPlace(Long placeId, int limit) {
        findPlace(placeId);
        return reviewRepository.findByPlaceIdOrderByCreatedAtDescIdDesc(placeId, Limit.of(limit)).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place " + placeId + " not found"));
    }
}
