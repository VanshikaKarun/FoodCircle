package com.vkarun.foodcircle.repository;

import com.vkarun.foodcircle.entity.Follow;
import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.entity.Review;
import com.vkarun.foodcircle.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The rules of the data model, checked by the database itself: uniqueness, check constraints, delete behaviour.
// These are the same rules first verified by hand in psql on day 1, now repeatable.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SocialRulesRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user(String username) {
        return userRepository.save(new User(username, "Name of " + username));
    }

    private Place place() {
        return placeRepository.save(new Place(OsmType.NODE, 1L, "Cafe", PlaceCategory.CAFE, null, 17.4, 78.4, null));
    }

    // ---- users ----

    @Test
    void aUsernameCanBeUsedOnlyOnce() {
        user("asha");
        userRepository.flush();

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("asha", "Another Asha")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---- follows ----

    @Test
    void aFollowPairCanExistOnlyOnce() {
        User asha = user("asha");
        User ravi = user("ravi");
        followRepository.saveAndFlush(new Follow(asha, ravi));

        assertThatThrownBy(() -> followRepository.saveAndFlush(new Follow(asha, ravi)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void followingYourselfIsRejectedByTheDatabase() {
        User asha = user("asha");

        assertThatThrownBy(() -> followRepository.saveAndFlush(new Follow(asha, asha)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void followQueries_countListAndDelete() {
        User asha = user("asha");
        User ravi = user("ravi");
        User meera = user("meera");
        followRepository.save(new Follow(asha, ravi));
        followRepository.save(new Follow(meera, ravi));
        followRepository.flush();

        assertThat(followRepository.countByFolloweeId(ravi.getId())).isEqualTo(2);
        assertThat(followRepository.countByFollowerId(asha.getId())).isEqualTo(1);
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(asha.getId(), ravi.getId())).isTrue();
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(ravi.getId(), asha.getId())).isFalse();
        assertThat(followRepository.findFollowers(ravi.getId())).extracting(User::getUsername).containsExactlyInAnyOrder("asha", "meera");
        assertThat(followRepository.findFollowing(asha.getId())).extracting(User::getUsername).containsExactly("ravi");

        assertThat(followRepository.deleteByFollowerIdAndFolloweeId(asha.getId(), ravi.getId())).isEqualTo(1);
        assertThat(followRepository.deleteByFollowerIdAndFolloweeId(asha.getId(), ravi.getId())).isZero();
    }

    // ---- reviews ----

    @Test
    void aReviewNeedsAtLeastOneRating() {
        User asha = user("asha");
        Place place = place();

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(new Review(asha, place, null, null, "no stars")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ratingsOutsideOneToFiveAreRejectedByTheDatabase() {
        User asha = user("asha");
        Place place = place();

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(new Review(asha, place, 6, null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ratingsOfOneOrFiveAndASingleRatingAreAccepted() {
        User asha = user("asha");
        Place place = place();

        reviewRepository.save(new Review(asha, place, 1, null, null));
        reviewRepository.save(new Review(asha, place, null, 5, null));
        reviewRepository.flush();

        assertThat(reviewRepository.count()).isEqualTo(2);
    }

    @Test
    void deletingAUserDeletesTheirReviewsAndDishes() {
        User asha = user("asha");
        Place place = place();
        Review review = new Review(asha, place, 4, 4, "good");
        review.addDish("Dosa", true);
        reviewRepository.saveAndFlush(review);
        assertThat(jdbcTemplate.queryForObject("select count(*) from review_dishes", Integer.class)).isEqualTo(1);

        userRepository.delete(asha);
        userRepository.flush();
        entityManager.clear();

        assertThat(reviewRepository.count()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from review_dishes", Integer.class)).isZero();
    }

    @Test
    void aPlaceThatHasReviewsCannotBeDeleted() {
        User asha = user("asha");
        Place place = place();
        reviewRepository.saveAndFlush(new Review(asha, place, 4, null, null));

        // Plain SQL on purpose: this is the database's own foreign key (ON DELETE RESTRICT) refusing, not Hibernate's checks.
        assertThatThrownBy(() -> jdbcTemplate.update("delete from places where id = ?", place.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
