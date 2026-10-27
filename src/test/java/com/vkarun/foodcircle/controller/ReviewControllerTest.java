package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.ReviewResponse;
import com.vkarun.foodcircle.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    private org.springframework.test.web.servlet.ResultActions postReview(String body) throws Exception {
        return mockMvc.perform(post("/api/places/5/reviews").header("X-User-Id", "3")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void create_validReview_returns201() throws Exception {
        given(reviewService.create(any(), any(), any())).willReturn(new ReviewResponse(
                1L, 3L, "asha", 5L, "Cafe X", 4, 5, new BigDecimal("4.5"), "Nice", Instant.now(),
                List.of(new ReviewResponse.DishResponse("Dosa", true))));

        postReview("{\"foodRating\":4,\"ambienceRating\":5,\"text\":\"Nice\",\"dishes\":[{\"name\":\"Dosa\",\"liked\":true}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overallRating").value(4.5))
                .andExpect(jsonPath("$.dishes[0].name").value("Dosa"));
    }

    @Test
    void create_invalidReviews_return400() throws Exception {
        for (String body : new String[]{
                "{}",                                                          // no rating at all
                "{\"text\":\"only text\"}",                                    // still no rating
                "{\"foodRating\":6}",                                          // above 5
                "{\"foodRating\":0}",                                          // below 1
                "{\"ambienceRating\":3,\"text\":\"" + "x".repeat(2001) + "\"}",// text too long
                "{\"foodRating\":3,\"dishes\":[{\"name\":\"\"}]}",             // blank dish name
                "{\"foodRating\":3,\"dishes\":[{\"name\":\"" + "d".repeat(101) + "\"}]}"}) {   // dish name too long
            postReview(body).andExpect(status().isBadRequest());
        }
    }

    @Test
    void create_withoutTheUserHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/places/5/reviews").contentType(MediaType.APPLICATION_JSON).content("{\"foodRating\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_passesTheLimit_andRejectsOutOfRangeLimits() throws Exception {
        given(reviewService.listForPlace(5L, 20)).willReturn(List.of());

        mockMvc.perform(get("/api/places/5/reviews")).andExpect(status().isOk());
        mockMvc.perform(get("/api/places/5/reviews").param("limit", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/places/5/reviews").param("limit", "51")).andExpect(status().isBadRequest());
    }
}
