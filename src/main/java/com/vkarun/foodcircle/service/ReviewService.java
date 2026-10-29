package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.dto.CreateReviewRequest;
import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.Review;
import com.vkarun.foodcircle.entity.User;
import com.vkarun.foodcircle.repository.PlaceRepository;
import com.vkarun.foodcircle.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

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
            throw ApiException.conflict("Place " + placeId + " is closed");
        }

        Review review = new Review(user, place, request.foodRating(), request.ambienceRating(), request.text());
        if (request.dishes() != null) {
            for (CreateReviewRequest.DishRequest dish : request.dishes()) {
                review.addDish(dish.name().trim(), dish.liked());   // saved with the review (cascade)
            }
        }
        Review saved = reviewRepository.save(review);
        // The review text is not logged: it is the user's own writing, and ids are enough to find it.
        log.info("review {} created by user {} for place {} with {} dishes",
                saved.getId(), currentUserId, placeId, saved.getDishes().size());
        return ReviewResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForPlace(Long placeId, int limit) {
        findPlace(placeId);
        List<ReviewResponse> result = reviewRepository.findByPlaceIdOrderByCreatedAtDescIdDesc(placeId, Limit.of(limit)).stream()
                .map(ReviewResponse::from)
                .toList();
        log.debug("reviews of place {} limit={} -> {} reviews", placeId, limit, result.size());
        return result;
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> ApiException.notFound("Place " + placeId + " not found"));
    }
}
