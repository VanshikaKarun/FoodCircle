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
    void theGeohashSearchGivesExactlyTheSameAnswerAsTheNaiveScan() {
        // 400 pseudo-random places over a 0.4 x 0.4 degree box (the Hyderabad box), a fixed seed so a failure can be repeated.
        java.util.Random random = new java.util.Random(42);
        for (int i = 1; i <= 400; i++) {
            save(i, "p" + i, 17.2 + random.nextDouble() * 0.4, 78.25 + random.nextDouble() * 0.4);
        }
        // a few closed ones: neither search may return them
        for (int i = 401; i <= 410; i++) {
            save(i, "closed" + i, 17.4, 78.45).markClosed();
        }

        double[][] searches = {{17.385, 78.4867, 2}, {17.385, 78.4867, 0.3}, {17.30, 78.35, 5}, {17.5, 78.6, 15},
                {17.2, 78.25, 4}, {17.6, 78.65, 1}, {17.4, 78.45, 0.05}};
        for (double[] s : searches) {
            List<NearbyPlaceResponse> geohash = placeService.findNearby(s[0], s[1], s[2]);
            List<NearbyPlaceResponse> naive = placeService.findNearbyNaive(s[0], s[1], s[2]);

            assertThat(geohash).as("search %s", java.util.Arrays.toString(s)).isEqualTo(naive);
        }
        assertThat(placeService.findNearbyNaive(17.385, 78.4867, 15)).isNotEmpty();
    }

    @Test
    void nothingNearby_returnsAnEmptyList() {
        assertThat(placeService.findNearby(LAT, LNG, 2)).isEmpty();
    }
}
