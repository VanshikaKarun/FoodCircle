package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.dto.CreateUserRequest;
import com.vkarun.foodcircle.dto.UserResponse;
import com.vkarun.foodcircle.dto.UserSummary;
import com.vkarun.foodcircle.entity.Follow;
import com.vkarun.foodcircle.entity.User;
import com.vkarun.foodcircle.repository.FollowRepository;
import com.vkarun.foodcircle.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, FollowRepository followRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw ApiException.conflict("Username '" + request.username() + "' is taken");
        }
        // Only the hash is stored; the raw password is never saved or logged.
        String passwordHash = passwordEncoder.encode(request.password());
        User user = userRepository.save(new User(request.username(), request.displayName(), passwordHash));
        log.info("user {} created with username {}", user.getId(), user.getUsername());
        return UserResponse.from(user, 0, 0);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = findUser(id);
        long followers = followRepository.countByFolloweeId(id);
        long following = followRepository.countByFollowerId(id);
        log.debug("user {} profile: {} followers, {} following", id, followers, following);
        return UserResponse.from(user, followers, following);
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
            log.info("user {} followed user {}", currentUserId, targetId);
        } else {
            log.debug("user {} already follows user {}, nothing to do", currentUserId, targetId);
        }
    }

    // Unfollowing someone you do not follow is not an error either.
    @Transactional
    public void unfollow(Long currentUserId, Long targetId) {
        requireCurrentUser(currentUserId);
        long removed = followRepository.deleteByFollowerIdAndFolloweeId(currentUserId, targetId);
        if (removed > 0) {
            log.info("user {} unfollowed user {}", currentUserId, targetId);
        } else {
            log.debug("user {} was not following user {}, nothing to do", currentUserId, targetId);
        }
    }

    @Transactional(readOnly = true)
    public List<UserSummary> followers(Long userId) {
        findUser(userId);
        List<UserSummary> result = followRepository.findFollowers(userId).stream().map(UserSummary::from).toList();
        log.debug("user {} has {} followers", userId, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> following(Long userId) {
        findUser(userId);
        List<UserSummary> result = followRepository.findFollowing(userId).stream().map(UserSummary::from).toList();
        log.debug("user {} follows {} users", userId, result.size());
        return result;
    }

    // The caller, by the user id inside their token. A valid token whose user no longer exists = 401.
    public User requireCurrentUser(Long currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> ApiException.unauthorized("Unknown user"));
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
