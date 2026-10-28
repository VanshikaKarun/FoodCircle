package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.NearbyPlaceResponse;
import com.vkarun.foodcircle.dto.PlaceResponse;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.vkarun.foodcircle.common.ApiException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    void nearby_returnsTheListWithDistances_andDefaultsTheRadiusTo2() throws Exception {
        given(placeService.findNearby(17.385, 78.4867, 2.0)).willReturn(List.of(
                new NearbyPlaceResponse(6L, "KFC", PlaceCategory.FAST_FOOD, "chicken", 17.39, 78.49, null, 0.5)));

        mockMvc.perform(get("/api/places/nearby").param("lat", "17.385").param("lng", "78.4867"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("KFC"))
                .andExpect(jsonPath("$[0].distanceKm").value(0.5));
    }

    @Test
    void nearbyNaive_hasTheSameContractAsNearby() throws Exception {
        given(placeService.findNearbyNaive(17.385, 78.4867, 2.0)).willReturn(List.of(
                new NearbyPlaceResponse(6L, "KFC", PlaceCategory.FAST_FOOD, "chicken", 17.39, 78.49, null, 0.5)));

        mockMvc.perform(get("/api/places/nearby-naive").param("lat", "17.385").param("lng", "78.4867"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distanceKm").value(0.5));
        mockMvc.perform(get("/api/places/nearby-naive").param("lat", "95").param("lng", "78"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearby_rejectsBadParameters() throws Exception {
        mockMvc.perform(get("/api/places/nearby").param("lat", "95").param("lng", "78"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/places/nearby").param("lat", "17").param("lng", "200"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/places/nearby").param("lat", "17").param("lng", "78").param("radiusKm", "16"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/places/nearby").param("lat", "17").param("lng", "78").param("radiusKm", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/places/nearby").param("lng", "78"))   // lat missing
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPlace_unknownId_returns404() throws Exception {
        given(placeService.getById(999L)).willThrow(ApiException.notFound("Place 999 not found"));

        mockMvc.perform(get("/api/places/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Place 999 not found"));
    }
}
