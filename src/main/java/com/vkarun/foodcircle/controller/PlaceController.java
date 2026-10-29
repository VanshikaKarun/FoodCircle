package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.NearbyPlaceResponse;
import com.vkarun.foodcircle.dto.PlaceResponse;
import com.vkarun.foodcircle.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Places")
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @Operation(summary = "Find open places by name, ignoring case",
            description = "The text may appear anywhere in the name ('para' finds 'Hotel Paradise'). Sorted by name. "
                    + "name must have 2 to 50 characters; limit defaults to 20 and may be at most 50.")
    @GetMapping
    public List<PlaceResponse> searchByName(
            @RequestParam @Size(min = 2, max = 50) String name,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return placeService.searchByName(name, limit);
    }

    // Fixed text "nearby" wins over the "/{id}" pattern, so the two paths do not clash.
    @Operation(summary = "Open places within a radius of a point, nearest first",
            description = "radiusKm defaults to 2 and may be at most 15. Uses a geohash index, then the exact distance.")
    @GetMapping("/nearby")
    public List<NearbyPlaceResponse> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            // Above 15 km the geohash cells grow so large that the search would read most of the table (see learning-notes 6c).
            @RequestParam(defaultValue = "2") @DecimalMin(value = "0", inclusive = false) @DecimalMax("15") double radiusKm) {
        return placeService.findNearby(lat, lng, radiusKm);
    }

    // Same answer as /nearby, found by checking every place. Exists only so the two approaches can be load-tested side by side.
    @Operation(summary = "Benchmark baseline: the same search as /nearby, without the geohash index",
            description = "Reads every open place and measures the distance to each. Not for normal use; it gets slower as the table grows.")
    @GetMapping("/nearby-naive")
    public List<NearbyPlaceResponse> nearbyNaive(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(defaultValue = "2") @DecimalMin(value = "0", inclusive = false) @DecimalMax("15") double radiusKm) {
        return placeService.findNearbyNaive(lat, lng, radiusKm);
    }

    @Operation(summary = "One place by id")
    @GetMapping("/{id}")
    public PlaceResponse getPlace(@PathVariable Long id) {
        return placeService.getById(id);
    }
}
