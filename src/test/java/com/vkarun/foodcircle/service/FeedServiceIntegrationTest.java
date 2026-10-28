package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.CreateReviewRequest;
import com.vkarun.foodcircle.dto.CreateUserRequest;
import com.vkarun.foodcircle.dto.CursorPage;
import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.repository.PlaceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.vkarun.foodcircle.common.ApiException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Real services, repositories and PostgreSQL (foodcircle_test); every test rolls back.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FeedServiceIntegrationTest {

    @Autowired
    private FeedService feedService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long newUser(String username) {
        return userService.create(new CreateUserRequest(username, "Name of " + username)).id();
    }

    private Long newPlace(long osmId) {
        return placeRepository.save(new Place(OsmType.NODE, osmId, "Cafe " + osmId, PlaceCategory.CAFE, null, 17.4, 78.4, null)).getId();
    }

    private ReviewResponse review(Long userId, Long placeId, String text) {
        return reviewService.create(userId, placeId, new CreateReviewRequest(4, null, text, null));
    }

    private HttpStatus statusOf(Throwable t) {
        return ((ApiException) t).getStatus();
    }

    // Reads every page of the feed, following the cursors, and returns all the review texts in the order received.
    private List<String> readWholeFeed(Long me, int pageSize) {
        List<String> texts = new ArrayList<>();
        String cursor = null;
        do {
            CursorPage<ReviewResponse> page = feedService.feed(me, cursor, pageSize);
            page.items().forEach(r -> texts.add(r.text()));
            cursor = page.nextCursor();
        } while (cursor != null);
        return texts;
    }

    @Test
    void feed_showsOnlyReviewsOfPeopleIFollow_newestFirst() {
        Long me = newUser("me");
        Long bob = newUser("bob");
        Long cara = newUser("cara");
        Long stranger = newUser("stranger");
        Long place = newPlace(1);
        userService.follow(me, bob);
        userService.follow(me, cara);

        review(bob, place, "bob 1");
        review(stranger, place, "stranger");
        review(cara, place, "cara 1");
        review(me, place, "my own");
        review(bob, place, "bob 2");

        CursorPage<ReviewResponse> page = feedService.feed(me, null, 20);

        assertThat(page.items()).extracting(ReviewResponse::text).containsExactly("bob 2", "cara 1", "bob 1");
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void feed_ofSomeoneFollowingNobody_isEmpty() {
        Long me = newUser("me");
        review(me, newPlace(1), "my own");

        CursorPage<ReviewResponse> page = feedService.feed(me, null, 20);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void feed_pages_neverRepeatOrSkip_andTheLastPageHasNoCursor() {
        Long me = newUser("me");
        Long bob = newUser("bob");
        Long place = newPlace(1);
        userService.follow(me, bob);
        for (int i = 1; i <= 7; i++) {
            review(bob, place, "r" + i);
        }

        CursorPage<ReviewResponse> first = feedService.feed(me, null, 3);
        assertThat(first.items()).extracting(ReviewResponse::text).containsExactly("r7", "r6", "r5");
        assertThat(first.nextCursor()).isNotNull();

        CursorPage<ReviewResponse> second = feedService.feed(me, first.nextCursor(), 3);
        assertThat(second.items()).extracting(ReviewResponse::text).containsExactly("r4", "r3", "r2");

        CursorPage<ReviewResponse> third = feedService.feed(me, second.nextCursor(), 3);
        assertThat(third.items()).extracting(ReviewResponse::text).containsExactly("r1");
        assertThat(third.nextCursor()).isNull();
    }

    @Test
    void feed_exactlyOnePageOfItems_hasNoNextCursor() {
        Long me = newUser("me");
        Long bob = newUser("bob");
        Long place = newPlace(1);
        userService.follow(me, bob);
        review(bob, place, "r1");
        review(bob, place, "r2");

        assertThat(feedService.feed(me, null, 2).nextCursor()).isNull();
    }

    @Test
    void feed_pages_workWhenReviewsShareTheSameTimestamp() {
        // The id is the tiebreaker: force every review to the same instant and the paging must still be complete and ordered.
        Long me = newUser("me");
        Long bob = newUser("bob");
        Long place = newPlace(1);
        userService.follow(me, bob);
        for (int i = 1; i <= 6; i++) {
            review(bob, place, "r" + i);
        }
        entityManager.flush();
        jdbcTemplate.update("update reviews set created_at = timestamptz '2026-10-27 10:00:00+00'");
        entityManager.clear();   // forget the in-memory copies so the next reads come from the database

        assertThat(readWholeFeed(me, 2)).containsExactly("r6", "r5", "r4", "r3", "r2", "r1");
    }

    @Test
    void feed_aReviewPostedBetweenPages_doesNotShiftTheNextPage() {
        Long me = newUser("me");
        Long bob = newUser("bob");
        Long place = newPlace(1);
        userService.follow(me, bob);
        for (int i = 1; i <= 4; i++) {
            review(bob, place, "r" + i);
        }

        CursorPage<ReviewResponse> first = feedService.feed(me, null, 2);
        assertThat(first.items()).extracting(ReviewResponse::text).containsExactly("r4", "r3");

        review(bob, place, "new while reading");   // with page numbers this would push r3 onto page 2 again

        CursorPage<ReviewResponse> second = feedService.feed(me, first.nextCursor(), 2);
        assertThat(second.items()).extracting(ReviewResponse::text).containsExactly("r2", "r1");
    }

    @Test
    void feed_dishesAreIncluded() {
        Long me = newUser("me");
        Long bob = newUser("bob");
        userService.follow(me, bob);
        reviewService.create(bob, newPlace(1), new CreateReviewRequest(5, null, "great",
                List.of(new CreateReviewRequest.DishRequest("Biryani", true))));

        ReviewResponse item = feedService.feed(me, null, 20).items().get(0);

        assertThat(item.dishes()).extracting(ReviewResponse.DishResponse::name).containsExactly("Biryani");
        assertThat(item.username()).isEqualTo("bob");
        assertThat(item.placeName()).isEqualTo("Cafe 1");
    }

    @Test
    void feed_badCursor_is400_unknownCaller_is401() {
        Long me = newUser("me");

        assertThatThrownBy(() -> feedService.feed(me, "garbage!", 20))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> feedService.feed(999_999L, null, 20))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void reviewsOf_listsOnlyThatUsersReviews_paged_includingMyOwn() {
        Long me = newUser("me");
        Long bob = newUser("bob");
        Long place = newPlace(1);
        review(me, place, "mine 1");
        review(bob, place, "bob's");
        review(me, place, "mine 2");
        review(me, place, "mine 3");

        CursorPage<ReviewResponse> first = feedService.reviewsOf(me, null, 2);
        assertThat(first.items()).extracting(ReviewResponse::text).containsExactly("mine 3", "mine 2");
        CursorPage<ReviewResponse> second = feedService.reviewsOf(me, first.nextCursor(), 2);
        assertThat(second.items()).extracting(ReviewResponse::text).containsExactly("mine 1");
        assertThat(second.nextCursor()).isNull();
    }

    @Test
    void reviewsOf_unknownUser_is404() {
        assertThatThrownBy(() -> feedService.reviewsOf(999_999L, null, 20))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void friendsReviews_isTheLatestReviewOfEachFollowedUser_forThatPlaceOnly() {
        Long me = newUser("me");
        Long bob = newUser("bob");
        Long cara = newUser("cara");
        Long stranger = newUser("stranger");
        Long place = newPlace(1);
        Long otherPlace = newPlace(2);
        userService.follow(me, bob);
        userService.follow(me, cara);

        review(bob, place, "bob old");
        review(cara, place, "cara only");
        review(bob, place, "bob latest");
        review(stranger, place, "stranger");
        review(me, place, "my own");
        review(cara, otherPlace, "cara elsewhere");

        List<ReviewResponse> result = feedService.friendsReviews(me, place);

        assertThat(result).extracting(ReviewResponse::text).containsExactly("bob latest", "cara only");
    }

    @Test
    void friendsReviews_nobodyIFollowReviewedIt_isEmpty_unknownPlaceIs404() {
        Long me = newUser("me");
        Long place = newPlace(1);

        assertThat(feedService.friendsReviews(me, place)).isEmpty();
        assertThatThrownBy(() -> feedService.friendsReviews(me, 999_999L))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
