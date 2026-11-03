package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.CreateReviewRequest;
import com.vkarun.foodcircle.dto.CreateReviewRequest.DishRequest;
import com.vkarun.foodcircle.dto.CreateUserRequest;
import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.vkarun.foodcircle.common.ApiException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Real services, repositories and PostgreSQL (foodcircle_test); every test rolls back.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewServiceIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private PlaceRepository placeRepository;

    private Long newUser(String username) {
        return userService.create(new CreateUserRequest(username, "Name of " + username, "password123")).id();
    }

    private Place newPlace(long osmId) {
        return placeRepository.save(new Place(OsmType.NODE, osmId, "Cafe " + osmId, PlaceCategory.CAFE, null, 17.4, 78.4, null));
    }

    private HttpStatus statusOf(Throwable t) {
        return ((ApiException) t).getStatus();
    }

    @Test
    void create_savesTheReviewWithItsDishes() {
        Long asha = newUser("asha");
        Place place = newPlace(1);
        var request = new CreateReviewRequest(4, 5, "Lovely garden seating",
                List.of(new DishRequest("  Masala Dosa ", true), new DishRequest("Filter coffee", null)));

        ReviewResponse review = reviewService.create(asha, place.getId(), request);

        assertThat(review.id()).isNotNull();
        assertThat(review.username()).isEqualTo("asha");
        assertThat(review.placeName()).isEqualTo("Cafe 1");
        assertThat(review.overallRating()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(review.dishes()).extracting(ReviewResponse.DishResponse::name)
                .containsExactly("Masala Dosa", "Filter coffee");   // names are trimmed
        assertThat(review.dishes().get(1).liked()).isNull();
    }

    @Test
    void create_withOnlyAmbience_isAllowed_andHasNoDishes() {
        Long asha = newUser("asha");
        Place place = newPlace(1);

        ReviewResponse review = reviewService.create(asha, place.getId(), new CreateReviewRequest(null, 3, null, null));

        assertThat(review.foodRating()).isNull();
        assertThat(review.overallRating()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(review.dishes()).isEmpty();
    }

    @Test
    void create_onAClosedPlace_is409_unknownPlace_is404_unknownCaller_is401() {
        Long asha = newUser("asha");
        Place closed = newPlace(1);
        closed.markClosed();
        var request = new CreateReviewRequest(4, null, null, null);

        assertThatThrownBy(() -> reviewService.create(asha, closed.getId(), request))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> reviewService.create(asha, 999_999L, request))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.NOT_FOUND));
        Place open = newPlace(2);
        assertThatThrownBy(() -> reviewService.create(999_999L, open.getId(), request))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void listForPlace_isNewestFirst_limited_andOnlyThatPlace() {
        Long asha = newUser("asha");
        Long ravi = newUser("ravi");
        Place place = newPlace(1);
        Place other = newPlace(2);
        reviewService.create(asha, place.getId(), new CreateReviewRequest(3, null, "first", null));
        reviewService.create(ravi, place.getId(), new CreateReviewRequest(4, null, "second",
                List.of(new DishRequest("Biryani", true))));
        reviewService.create(asha, place.getId(), new CreateReviewRequest(5, null, "third", null));
        reviewService.create(asha, other.getId(), new CreateReviewRequest(1, null, "elsewhere", null));

        List<ReviewResponse> all = reviewService.listForPlace(place.getId(), 20);
        assertThat(all).extracting(ReviewResponse::text).containsExactly("third", "second", "first");
        assertThat(all.get(1).dishes()).extracting(ReviewResponse.DishResponse::name).containsExactly("Biryani");

        assertThat(reviewService.listForPlace(place.getId(), 2)).extracting(ReviewResponse::text)
                .containsExactly("third", "second");
    }

    @Test
    void listForPlace_unknownPlace_is404() {
        assertThatThrownBy(() -> reviewService.listForPlace(999_999L, 20))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
