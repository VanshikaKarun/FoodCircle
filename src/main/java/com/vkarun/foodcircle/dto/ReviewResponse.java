package com.vkarun.foodcircle.dto;

import com.vkarun.foodcircle.entity.Review;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        Long id,
        Long userId,
        String username,
        Long placeId,
        String placeName,
        Integer foodRating,
        Integer ambienceRating,
        BigDecimal overallRating,
        String text,
        Instant createdAt,
        List<DishResponse> dishes) {

    public record DishResponse(String name, Boolean liked) {
    }

    public static ReviewResponse from(Review review) {
        List<DishResponse> dishes = review.getDishes().stream()
                .map(dish -> new DishResponse(dish.getDishName(), dish.getLiked()))
                .toList();
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getUsername(),
                review.getPlace().getId(),
                review.getPlace().getName(),
                review.getFoodRating(),
                review.getAmbienceRating(),
                review.getOverallRating(),
                review.getText(),
                review.getCreatedAt(),
                dishes);
    }
}
