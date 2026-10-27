package com.vkarun.foodcircle.dto;

import com.vkarun.foodcircle.entity.User;

import java.time.Instant;

// A user's profile. The follower counts are not stored (D11): they are counted from the follows table when asked for.
public record UserResponse(
        Long id,
        String username,
        String displayName,
        long followerCount,
        long followingCount,
        Instant createdAt) {

    public static UserResponse from(User user, long followerCount, long followingCount) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                followerCount, followingCount, user.getCreatedAt());
    }
}
