package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.NearbyPlaceResponse;
import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Real service, real repository, real PostgreSQL (foodcircle_test); each test rolls back what it saved.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NearbyPlacesIntegrationTest {

    // The searcher, in Hyderabad. 0.01 degrees of latitude is about 1.11 km.
    private static final double LAT = 17.385;
    private static final double LNG = 78.4867;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private PlaceRepository placeRepository;

    private Place save(long osmId, String name, double lat, double lng) {
        return placeRepository.save(new Place(OsmType.NODE, osmId, name, PlaceCategory.CAFE, null, lat, lng, null));
    }

    @Test
    void returnsOnlyPlacesWithinTheRadius_nearestFirst() {
        save(1, "half a km north", LAT + 0.0045, LNG);
        save(2, "three km north", LAT + 0.027, LNG);
        save(3, "one km east", LAT, LNG + 0.0094);
        save(4, "thirty km north", LAT + 0.27, LNG);

        List<NearbyPlaceResponse> within2 = placeService.findNearby(LAT, LNG, 2);
        assertThat(within2).extracting(NearbyPlaceResponse::name).containsExactly("half a km north", "one km east");

        List<NearbyPlaceResponse> within5 = placeService.findNearby(LAT, LNG, 5);
        assertThat(within5).extracting(NearbyPlaceResponse::name)
                .containsExactly("half a km north", "one km east", "three km north");
    }

    @Test
    void distancesAreReportedAndIncrease() {
        save(1, "near", LAT + 0.0045, LNG);
        save(2, "farther", LAT + 0.018, LNG);

        List<NearbyPlaceResponse> result = placeService.findNearby(LAT, LNG, 5);

        assertThat(result.get(0).distanceKm()).isBetween(0.49, 0.51);   // 0.0045 degrees of latitude = 0.5 km
        assertThat(result.get(1).distanceKm()).isBetween(1.99, 2.01);
    }

    @Test
    void aPlaceAcrossACellBorderIsStillFound() {
        // Put a place a few metres either side of the searcher's cell edge: the neighbour cells must be searched too.
        String cell = com.vkarun.foodcircle.geo.Geohash.encode(LAT, LNG, 5);
        var box = com.vkarun.foodcircle.geo.Geohash.decode(cell);
        double eastEdge = box.longitude() + box.longitudeError();
        save(1, "just inside", box.latitude(), eastEdge - 0.0001);
        save(2, "just outside", box.latitude(), eastEdge + 0.0001);

        // Search from a point right at the edge of the cell.
        List<NearbyPlaceResponse> result = placeService.findNearby(box.latitude(), eastEdge - 0.00005, 2);

        assertThat(result).extracting(NearbyPlaceResponse::name).containsExactlyInAnyOrder("just inside", "just outside");
    }

    @Test
    void closedPlacesAreNotReturned() {
        save(1, "open", LAT + 0.0045, LNG);
        Place closed = save(2, "closed", LAT + 0.0046, LNG);
        closed.markClosed();

        assertThat(placeService.findNearby(LAT, LNG, 2)).extracting(NearbyPlaceResponse::name).containsExactly("open");
    }

    @Test
    void nothingNearby_returnsAnEmptyList() {
        assertThat(placeService.findNearby(LAT, LNG, 2)).isEmpty();
    }
}
