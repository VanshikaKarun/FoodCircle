package com.vkarun.foodcircle.dto;

import com.vkarun.foodcircle.entity.User;

// A user inside a list (followers, following): no counts, so a list does not trigger a count query per row.
public record UserSummary(Long id, String username, String displayName) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getDisplayName());
    }
}
