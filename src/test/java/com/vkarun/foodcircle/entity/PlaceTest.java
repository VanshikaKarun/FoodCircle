package com.vkarun.foodcircle.entity;

import com.vkarun.foodcircle.geo.Geohash;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceTest {

    @Test
    void newPlace_getsItsGeohashFromItsCoordinates() {
        Place place = new Place(OsmType.NODE, 1L, "KFC", PlaceCategory.FAST_FOOD, "chicken", 17.4507642, 78.3793915, null);

        assertThat(place.getGeohash()).hasSize(Geohash.STORED_LENGTH)
                .isEqualTo(Geohash.encode(17.4507642, 78.3793915, Geohash.STORED_LENGTH));
    }

    @Test
    void updateFromOsm_recomputesTheGeohashWhenThePlaceMoves() {
        Place place = new Place(OsmType.NODE, 1L, "KFC", PlaceCategory.FAST_FOOD, null, 17.45, 78.37, null);
        String before = place.getGeohash();

        place.updateFromOsm("KFC", PlaceCategory.FAST_FOOD, null, 17.30, 78.60, null);

        assertThat(place.getGeohash()).isNotEqualTo(before)
                .isEqualTo(Geohash.encode(17.30, 78.60, Geohash.STORED_LENGTH));
    }
}
