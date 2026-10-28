package com.vkarun.foodcircle.repository;

import com.vkarun.foodcircle.entity.Review;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Newest first, matching idx_reviews_place_created (place_id, created_at desc, id desc), so the database reads
    // the index in order and stops after `limit` rows. The EntityGraph loads each review's user in the same query.
    @EntityGraph(attributePaths = "user")
    List<Review> findByPlaceIdOrderByCreatedAtDescIdDesc(Long placeId, Limit limit);

    // ---- The feed: reviews written by the people `me` follows, newest first (fan-out on read, D3). ----
    // Two queries instead of one with an optional cursor: a null parameter in "? is null or ..." confuses PostgreSQL's type inference.

    @EntityGraph(attributePaths = {"user", "place"})
    @Query("""
            select r from Review r
            where r.user.id in (select f.followee.id from Follow f where f.follower.id = :me)
            order by r.createdAt desc, r.id desc""")
    List<Review> findFeedFirstPage(@Param("me") Long me, Limit limit);

    // (createdAt, id) < (cursor) is the cursor rule: strictly older, and for the same instant a smaller id.
    @EntityGraph(attributePaths = {"user", "place"})
    @Query("""
            select r from Review r
            where r.user.id in (select f.followee.id from Follow f where f.follower.id = :me)
              and (r.createdAt, r.id) < (:createdAt, :id)
            order by r.createdAt desc, r.id desc""")
    List<Review> findFeedAfter(@Param("me") Long me, @Param("createdAt") Instant createdAt, @Param("id") Long id, Limit limit);

    // ---- One user's own reviews (uses idx_reviews_user_created). ----

    @EntityGraph(attributePaths = {"user", "place"})
    @Query("select r from Review r where r.user.id = :userId order by r.createdAt desc, r.id desc")
    List<Review> findByUserFirstPage(@Param("userId") Long userId, Limit limit);

    @EntityGraph(attributePaths = {"user", "place"})
    @Query("""
            select r from Review r
            where r.user.id = :userId and (r.createdAt, r.id) < (:createdAt, :id)
            order by r.createdAt desc, r.id desc""")
    List<Review> findByUserAfter(@Param("userId") Long userId, @Param("createdAt") Instant createdAt, @Param("id") Long id, Limit limit);

    // ---- "People I follow who reviewed this place": each followed user's latest review of the place. ----
    // DISTINCT ON (user_id) keeps the first row per user in the given order, i.e. the newest. It is PostgreSQL-specific, which is
    // fine here (D1); the plain-SQL alternative is a correlated subquery or a window function.
    @Query(nativeQuery = true, value = """
            select distinct on (r.user_id) r.*
            from reviews r
            where r.place_id = :placeId
              and r.user_id in (select f.followee_id from follows f where f.follower_id = :me)
            order by r.user_id, r.created_at desc, r.id desc""")
    List<Review> findLatestReviewPerFollowee(@Param("placeId") Long placeId, @Param("me") Long me);
}
