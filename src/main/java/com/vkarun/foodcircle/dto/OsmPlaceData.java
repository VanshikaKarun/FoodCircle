package com.vkarun.foodcircle.dto;

import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.PlaceCategory;

// One OSM entry after cleaning: exactly the values a Place needs, nothing OSM-specific left.
public record OsmPlaceData(
        OsmType osmType,
        long osmId,
        String name,
        PlaceCategory category,
        String cuisine,
        double latitude,
        double longitude,
        String address) {
}
