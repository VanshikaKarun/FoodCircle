package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.dto.CreateUserRequest;
import com.vkarun.foodcircle.dto.UserResponse;
import com.vkarun.foodcircle.dto.UserSummary;
import com.vkarun.foodcircle.entity.Follow;
import com.vkarun.foodcircle.entity.User;
import com.vkarun.foodcircle.repository.FollowRepository;
import com.vkarun.foodcircle.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public UserService(UserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw ApiException.conflict("Username '" + request.username() + "' is taken");
        }
        User user = userRepository.save(new User(request.username(), request.displayName()));
        return UserResponse.from(user, 0, 0);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = findUser(id);
        return UserResponse.from(user, followRepository.countByFolloweeId(id), followRepository.countByFollowerId(id));
    }

    // Following twice is not an error: the end state "A follows B" is what the caller asked for.
    @Transactional
    public void follow(Long currentUserId, Long targetId) {
        if (currentUserId.equals(targetId)) {
            throw ApiException.badRequest("You cannot follow yourself");
        }
        User me = requireCurrentUser(currentUserId);
        User target = findUser(targetId);
        if (!followRepository.existsByFollowerIdAndFolloweeId(currentUserId, targetId)) {
            followRepository.save(new Follow(me, target));
        }
    }

    // Unfollowing someone you do not follow is not an error either.
    @Transactional
    public void unfollow(Long currentUserId, Long targetId) {
        requireCurrentUser(currentUserId);
        followRepository.deleteByFollowerIdAndFolloweeId(currentUserId, targetId);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> followers(Long userId) {
        findUser(userId);
        return followRepository.findFollowers(userId).stream().map(UserSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> following(Long userId) {
        findUser(userId);
        return followRepository.findFollowing(userId).stream().map(UserSummary::from).toList();
    }

    // The user named in the temporary X-User-Id header. Unknown id = not logged in as anyone real (401).
    // Real authentication replaces this in week 2.
    public User requireCurrentUser(Long currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> ApiException.unauthorized("Unknown user in X-User-Id"));
    }

    // For other services that need "this user must exist" (404 otherwise).
    public User requireExisting(Long id) {
        return findUser(id);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User " + id + " not found"));
    }
}
