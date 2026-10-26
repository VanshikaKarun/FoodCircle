package com.vkarun.foodcircle.dto;

import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;

// What the API returns for a place. The entity is never sent out: this record decides exactly which fields the API exposes.
public record PlaceResponse(
        Long id,
        String name,
        PlaceCategory category,
        String cuisine,
        double latitude,
        double longitude,
        String address,
        boolean closed) {

    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getCategory(),
                place.getCuisine(),
                place.getLatitude(),
                place.getLongitude(),
                place.getAddress(),
                place.isClosed());
    }
}
