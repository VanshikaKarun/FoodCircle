package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.CursorPage;
import com.vkarun.foodcircle.service.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @Test
    void feed_passesTheCallerCursorAndLimit_andReturnsTheNextCursor() throws Exception {
        given(feedService.feed(3L, "abc", 10)).willReturn(new CursorPage<>(List.of(), "next-token"));

        mockMvc.perform(get("/api/feed").header("X-User-Id", "3").param("cursor", "abc").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.nextCursor").value("next-token"));
    }

    @Test
    void feed_defaultsTo20_andNeedsTheHeader() throws Exception {
        given(feedService.feed(3L, null, 20)).willReturn(new CursorPage<>(List.of(), null));

        mockMvc.perform(get("/api/feed").header("X-User-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
        mockMvc.perform(get("/api/feed")).andExpect(status().isBadRequest());
    }

    @Test
    void feed_rejectsOutOfRangeLimits() throws Exception {
        mockMvc.perform(get("/api/feed").header("X-User-Id", "3").param("limit", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/feed").header("X-User-Id", "3").param("limit", "51")).andExpect(status().isBadRequest());
    }

    @Test
    void reviewsOfAUser_needsNoHeader_andPassesTheArguments() throws Exception {
        given(feedService.reviewsOf(5L, null, 20)).willReturn(new CursorPage<>(List.of(), null));

        mockMvc.perform(get("/api/users/5/reviews")).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/5/reviews").param("limit", "99")).andExpect(status().isBadRequest());
    }

    @Test
    void friendsReviews_needsTheHeader() throws Exception {
        given(feedService.friendsReviews(3L, 8L)).willReturn(List.of());

        mockMvc.perform(get("/api/places/8/friends-reviews").header("X-User-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/places/8/friends-reviews")).andExpect(status().isBadRequest());
    }
}
