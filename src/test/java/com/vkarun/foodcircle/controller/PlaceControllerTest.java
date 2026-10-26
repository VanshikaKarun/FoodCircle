package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.PlaceResponse;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Controller slice: only the web layer is started; the service is a mock, so no database is needed.
@WebMvcTest(PlaceController.class)
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @Test
    void getPlace_returnsThePlaceAsJson() throws Exception {
        given(placeService.getById(6L)).willReturn(
                new PlaceResponse(6L, "KFC", PlaceCategory.FAST_FOOD, "chicken", 17.45, 78.37, null, false));

        mockMvc.perform(get("/api/places/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.name").value("KFC"))
                .andExpect(jsonPath("$.category").value("FAST_FOOD"))
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.closed").value(false));
    }

    @Test
    void getPlace_unknownId_returns404() throws Exception {
        given(placeService.getById(999L)).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/places/999"))
                .andExpect(status().isNotFound());
    }
}
