package com.vkarun.foodcircle.repository;

import com.vkarun.foodcircle.entity.Follow;
import com.vkarun.foodcircle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // "FollowerId" is read as follower.id, so these compare the foreign key columns follower_id / followee_id.
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    long deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    long countByFolloweeId(Long followeeId);   // how many follow this user

    long countByFollowerId(Long followerId);   // how many this user follows

    // Who follows this user (uses idx_follows_followee), newest follow first.
    @Query("select f.follower from Follow f where f.followee.id = :userId order by f.createdAt desc, f.id desc")
    List<User> findFollowers(@Param("userId") Long userId);

    // Whom this user follows (uses the unique index uk_follows_pair, whose first column is follower_id).
    @Query("select f.followee from Follow f where f.follower.id = :userId order by f.createdAt desc, f.id desc")
    List<User> findFollowing(@Param("userId") Long userId);
}
