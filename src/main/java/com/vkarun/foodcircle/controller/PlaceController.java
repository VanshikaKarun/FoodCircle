package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.NearbyPlaceResponse;
import com.vkarun.foodcircle.dto.PlaceResponse;
import com.vkarun.foodcircle.service.PlaceService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    // Fixed text "nearby" wins over the "/{id}" pattern, so the two paths do not clash.
    @GetMapping("/nearby")
    public List<NearbyPlaceResponse> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            // Above 15 km the geohash cells grow so large that the search would read most of the table (see learning-notes 6c).
            @RequestParam(defaultValue = "2") @DecimalMin(value = "0", inclusive = false) @DecimalMax("15") double radiusKm) {
        return placeService.findNearby(lat, lng, radiusKm);
    }

    @GetMapping("/{id}")
    public PlaceResponse getPlace(@PathVariable Long id) {
        return placeService.getById(id);
    }
}
