package com.vkarun.foodcircle.repository;

import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.geo.Geohash;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Repository slice against the real PostgreSQL test database (not H2: the constraints and the collation must behave as in production).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PlaceRepositoryTest {

    @Autowired
    private PlaceRepository placeRepository;

    private Place place(OsmType type, long osmId, double lat, double lng) {
        return new Place(type, osmId, "Place " + osmId, PlaceCategory.CAFE, null, lat, lng, null);
    }

    @Test
    void findByOsmTypeAndOsmId_distinguishesTheTypeBecauseOsmNumbersEachKindSeparately() {
        placeRepository.save(place(OsmType.NODE, 1, 17.4, 78.4));
        placeRepository.save(place(OsmType.WAY, 1, 17.5, 78.5));

        assertThat(placeRepository.findByOsmTypeAndOsmId(OsmType.NODE, 1L)).get().extracting(Place::getLatitude).isEqualTo(17.4);
        assertThat(placeRepository.findByOsmTypeAndOsmId(OsmType.WAY, 1L)).get().extracting(Place::getLatitude).isEqualTo(17.5);
        assertThat(placeRepository.findByOsmTypeAndOsmId(OsmType.NODE, 2L)).isEmpty();
    }

    @Test
    void theSameOsmPlaceCannotBeStoredTwice() {
        placeRepository.saveAndFlush(place(OsmType.NODE, 1, 17.4, 78.4));

        assertThatThrownBy(() -> placeRepository.saveAndFlush(place(OsmType.NODE, 1, 17.4, 78.4)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void coordinatesOutsideTheEarthAreRejectedByTheDatabase() {
        assertThatThrownBy(() -> placeRepository.saveAndFlush(place(OsmType.NODE, 1, 91, 78)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findOpenInGeohashRange_returnsOpenPlacesOfTheCellOnly() {
        double lat = 17.385;
        double lng = 78.4867;
        String cell = Geohash.encode(lat, lng, 5);
        List<String> neighbours = Geohash.neighbours(cell);

        Place inCell = placeRepository.save(place(OsmType.NODE, 1, lat, lng));
        Place alsoInCell = placeRepository.save(place(OsmType.NODE, 2, lat + 0.0001, lng + 0.0001));
        Place closed = place(OsmType.NODE, 3, lat, lng);
        closed.markClosed();
        placeRepository.save(closed);
        // a place in the cell right next door must not come back for this cell
        var box = Geohash.decode(neighbours.get(0));
        Place elsewhere = placeRepository.save(place(OsmType.NODE, 4, box.latitude(), box.longitude()));

        List<Place> result = placeRepository.findOpenInGeohashRange(cell, cell + "{");

        assertThat(result).extracting(Place::getId).containsExactlyInAnyOrder(inCell.getId(), alsoInCell.getId())
                .doesNotContain(elsewhere.getId());
    }

    @Test
    void findOpenInGeohashRange_aShorterPrefixCoversTheLongerCells() {
        Place place = placeRepository.save(place(OsmType.NODE, 1, 17.385, 78.4867));
        String hash8 = place.getGeohash();

        for (int length = 1; length <= Geohash.STORED_LENGTH; length++) {
            String prefix = hash8.substring(0, length);
            assertThat(placeRepository.findOpenInGeohashRange(prefix, prefix + "{"))
                    .extracting(Place::getId).as("prefix of length " + length).contains(place.getId());
        }
    }

    private Place named(long osmId, String name) {
        return new Place(OsmType.NODE, osmId, name, PlaceCategory.RESTAURANT, null, 17.4, 78.4, null);
    }

    @Test
    void findOpenByNamePattern_ignoresCaseMatchesAnywhereAndSortsByName() {
        placeRepository.save(named(1, "Paradise Biryani"));
        placeRepository.save(named(2, "Hotel PARADISE"));
        placeRepository.save(named(3, "Cafe Bahar"));

        List<Place> result = placeRepository.findOpenByNamePattern("%paradise%", Limit.unlimited());

        assertThat(result).extracting(Place::getName).containsExactly("Hotel PARADISE", "Paradise Biryani");
    }

    @Test
    void findOpenByNamePattern_skipsClosedPlacesAndHonoursTheLimit() {
        Place closed = named(1, "Old Paradise");
        closed.markClosed();
        placeRepository.save(closed);
        placeRepository.save(named(2, "Paradise A"));
        placeRepository.save(named(3, "Paradise B"));
        placeRepository.save(named(4, "Paradise C"));

        List<Place> result = placeRepository.findOpenByNamePattern("%paradise%", Limit.of(2));

        assertThat(result).extracting(Place::getName).containsExactly("Paradise A", "Paradise B");
    }

    @Test
    void findOpenByNamePattern_anEscapedPercentSignIsPlainTextNotAWildcard() {
        placeRepository.save(named(1, "50% Off Pizza"));
        placeRepository.save(named(2, "Pizza Palace"));

        // "\%" is how the service will write a literal percent sign
        List<Place> result = placeRepository.findOpenByNamePattern("%50\\%%", Limit.unlimited());

        assertThat(result).extracting(Place::getName).containsExactly("50% Off Pizza");
    }
}
