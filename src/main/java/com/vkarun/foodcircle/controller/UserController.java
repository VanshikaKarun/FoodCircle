package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.CreateUserRequest;
import com.vkarun.foodcircle.dto.UserResponse;
import com.vkarun.foodcircle.dto.UserSummary;
import com.vkarun.foodcircle.service.UserService;
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
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable Long id, @RequestHeader("X-User-Id") Long currentUserId) {
        userService.follow(currentUserId, id);
    }

    @DeleteMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long id, @RequestHeader("X-User-Id") Long currentUserId) {
        userService.unfollow(currentUserId, id);
    }

    @GetMapping("/{id}/followers")
    public List<UserSummary> followers(@PathVariable Long id) {
        return userService.followers(id);
    }

    @GetMapping("/{id}/following")
    public List<UserSummary> following(@PathVariable Long id) {
        return userService.following(id);
    }
}
