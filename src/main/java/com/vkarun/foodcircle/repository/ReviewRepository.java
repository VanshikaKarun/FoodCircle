package com.vkarun.foodcircle.repository;

import com.vkarun.foodcircle.entity.Review;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Newest first, matching idx_reviews_place_created (place_id, created_at desc, id desc), so the database reads
    // the index in order and stops after `limit` rows. The EntityGraph loads each review's user in the same query.
    @EntityGraph(attributePaths = "user")
    List<Review> findByPlaceIdOrderByCreatedAtDescIdDesc(Long placeId, Limit limit);
}
