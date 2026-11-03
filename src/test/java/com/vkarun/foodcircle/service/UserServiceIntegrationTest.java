package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.CreateUserRequest;
import com.vkarun.foodcircle.dto.UserResponse;
import com.vkarun.foodcircle.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Real service, repositories and PostgreSQL (foodcircle_test); every test rolls back.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long newUser(String username) {
        return userService.create(new CreateUserRequest(username, "Name of " + username, "password123")).id();
    }

    private HttpStatus statusOf(Throwable t) {
        return ((ApiException) t).getStatus();
    }

    @Test
    void create_returnsTheUserWithZeroCounts() {
        UserResponse user = userService.create(new CreateUserRequest("asha", "Asha K", "password123"));

        assertThat(user.id()).isNotNull();
        assertThat(user.username()).isEqualTo("asha");
        assertThat(user.followerCount()).isZero();
        assertThat(user.followingCount()).isZero();
    }

    @Test
    void create_storesABcryptHash_neverThePassword() {
        Long id = userService.create(new CreateUserRequest("asha", "Asha K", "password123")).id();

        String stored = userRepository.findById(id).orElseThrow().getPasswordHash();
        assertThat(stored).isNotEqualTo("password123");
        assertThat(stored).startsWith("$2a$10$").hasSize(60);    // BCrypt, cost 10 (Spring's default)
        assertThat(passwordEncoder.matches("password123", stored)).isTrue();
        assertThat(passwordEncoder.matches("password124", stored)).isFalse();
    }

    @Test
    void create_samePasswordTwice_givesDifferentHashes() {
        Long asha = newUser("asha");
        Long ravi = newUser("ravi");    // both use "password123"

        String ashaHash = userRepository.findById(asha).orElseThrow().getPasswordHash();
        String raviHash = userRepository.findById(ravi).orElseThrow().getPasswordHash();
        assertThat(ashaHash).isNotEqualTo(raviHash);    // random salt per hash
    }

    @Test
    void create_aTakenUsername_isRejectedWith409() {
        newUser("asha");

        assertThatThrownBy(() -> newUser("asha"))
                .isInstanceOf(ApiException.class)
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void follow_updatesCountsAndLists() {
        Long asha = newUser("asha");
        Long ravi = newUser("ravi");
        Long meera = newUser("meera");

        userService.follow(asha, ravi);
        userService.follow(meera, ravi);
        userService.follow(asha, meera);

        assertThat(userService.getById(ravi).followerCount()).isEqualTo(2);
        assertThat(userService.getById(asha).followingCount()).isEqualTo(2);
        assertThat(userService.followers(ravi)).extracting(UserSummary::username).containsExactlyInAnyOrder("asha", "meera");
        assertThat(userService.following(asha)).extracting(UserSummary::username).containsExactlyInAnyOrder("ravi", "meera");
        assertThat(userService.following(ravi)).isEmpty();
    }

    @Test
    void follow_twice_isHarmless() {
        Long asha = newUser("asha");
        Long ravi = newUser("ravi");

        userService.follow(asha, ravi);
        userService.follow(asha, ravi);

        assertThat(userService.getById(ravi).followerCount()).isEqualTo(1);
    }

    @Test
    void follow_yourself_isRejectedWith400() {
        Long asha = newUser("asha");

        assertThatThrownBy(() -> userService.follow(asha, asha))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void follow_aUserThatDoesNotExist_is404_andAnUnknownCaller_is401() {
        Long asha = newUser("asha");

        assertThatThrownBy(() -> userService.follow(asha, 999_999L))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> userService.follow(999_999L, asha))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void unfollow_removesTheFollow_andDoingItAgainIsHarmless() {
        Long asha = newUser("asha");
        Long ravi = newUser("ravi");
        userService.follow(asha, ravi);

        userService.unfollow(asha, ravi);
        userService.unfollow(asha, ravi);

        assertThat(userService.getById(ravi).followerCount()).isZero();
        List<UserSummary> following = userService.following(asha);
        assertThat(following).isEmpty();
    }

    @Test
    void getById_unknown_is404() {
        assertThatThrownBy(() -> userService.getById(999_999L))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
