package com.vkarun.foodcircle.dto;

import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;

// A place in the nearby list: the usual place fields plus how far it is from the searcher.
public record NearbyPlaceResponse(
        Long id,
        String name,
        PlaceCategory category,
        String cuisine,
        double latitude,
        double longitude,
        String address,
        double distanceKm) {

    public static NearbyPlaceResponse from(Place place, double distanceKm) {
        return new NearbyPlaceResponse(
                place.getId(),
                place.getName(),
                place.getCategory(),
                place.getCuisine(),
                place.getLatitude(),
                place.getLongitude(),
                place.getAddress(),
                distanceKm);
    }
}
