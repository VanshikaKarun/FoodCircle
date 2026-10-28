package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.CreateUserRequest;
import com.vkarun.foodcircle.dto.UserResponse;
import com.vkarun.foodcircle.dto.UserSummary;
import com.vkarun.foodcircle.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// X-User-Id is the temporary "who am I" until real authentication in week 2.
@Tag(name = "Users and follows")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a user", description = "409 if the username is taken.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @Operation(summary = "A user's profile with follower and following counts")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "Follow a user (the caller is X-User-Id)", description = "Repeating it is harmless. Following yourself is a 400.")
    @PostMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable Long id, @RequestHeader("X-User-Id") Long currentUserId) {
        userService.follow(currentUserId, id);
    }

    @Operation(summary = "Unfollow a user", description = "Repeating it is harmless.")
    @DeleteMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long id, @RequestHeader("X-User-Id") Long currentUserId) {
        userService.unfollow(currentUserId, id);
    }

    @Operation(summary = "Who follows this user, newest follow first")
    @GetMapping("/{id}/followers")
    public List<UserSummary> followers(@PathVariable Long id) {
        return userService.followers(id);
    }

    @Operation(summary = "Whom this user follows, newest follow first")
    @GetMapping("/{id}/following")
    public List<UserSummary> following(@PathVariable Long id) {
        return userService.following(id);
    }
}
