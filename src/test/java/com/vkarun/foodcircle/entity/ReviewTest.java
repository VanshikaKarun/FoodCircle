package com.vkarun.foodcircle.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewTest {

    private static Review review(Integer food, Integer ambience) {
        return new Review(null, null, food, ambience, null);
    }

    @Test
    void overallIsTheAverageWhenBothRatingsGiven() {
        assertThat(review(4, 5).getOverallRating()).isEqualByComparingTo("4.5");
        assertThat(review(2, 5).getOverallRating()).isEqualByComparingTo("3.5");
        assertThat(review(3, 3).getOverallRating()).isEqualByComparingTo("3.0");
    }

    @Test
    void overallIsTheOnlyRatingGivenWhenOneIsMissing() {
        assertThat(review(null, 3).getOverallRating()).isEqualByComparingTo("3.0");
        assertThat(review(5, null).getOverallRating()).isEqualByComparingTo("5.0");
    }

    @Test
    void overallIsNullWhenNoRatingGiven() {
        assertThat(review(null, null).getOverallRating()).isNull();
    }

    @Test
    void overallKeepsOneDecimal() {
        BigDecimal overall = review(4, 5).getOverallRating();
        assertThat(overall.scale()).isEqualTo(1);
    }
}
