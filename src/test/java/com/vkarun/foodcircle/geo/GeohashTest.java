package com.vkarun.foodcircle.geo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

// Reference values: the English Wikipedia article "Geohash" (checked 2026-10-27). It decodes "ezs42" to
// latitude 42.605, and states that 57.64911, 10.40744 produces "u4pruydqqvj". Longitude -5.603 for "ezs42" is the
// midpoint of the interval its longitude bits (0111110000000) select, worked out by hand.
class GeohashTest {

    @Test
    void encode_matchesTheWikipediaExample_ezs42() {
        assertThat(Geohash.encode(42.605, -5.603, 5)).isEqualTo("ezs42");
    }

    @Test
    void encode_matchesTheWikipediaExample_u4pruydqqvj() {
        assertThat(Geohash.encode(57.64911, 10.40744, 11)).isEqualTo("u4pruydqqvj");
    }

    @Test
    void encode_returnsExactlyTheRequestedLength() {
        assertThat(Geohash.encode(17.385, 78.4867, 1)).hasSize(1);
        assertThat(Geohash.encode(17.385, 78.4867, 6)).hasSize(6);
    }

    @Test
    void decode_matchesTheWikipediaExample_ezs42() {
        GeohashCell cell = Geohash.decode("ezs42");

        // Wikipedia: latitude 42.605 with a maximum error of 0.022 at 5 characters (12 latitude bits, 13 longitude bits).
        assertThat(cell.latitude()).isCloseTo(42.605, within(0.001));
        assertThat(cell.latitudeError()).isCloseTo(0.022, within(0.0005));
        assertThat(cell.longitude()).isCloseTo(-5.603, within(0.001));
        assertThat(cell.longitudeError()).isCloseTo(0.022, within(0.0005));
    }

    @Test
    void decode_theCellContainsThePointThatWasEncoded() {
        double[][] points = {{17.385, 78.4867}, {17.4507642, 78.3793915}, {-33.8688, 151.2093}, {0, 0}, {89.9, -179.9}};
        for (double[] p : points) {
            GeohashCell cell = Geohash.decode(Geohash.encode(p[0], p[1], 7));
            assertThat(p[0]).isBetween(cell.latitude() - cell.latitudeError(), cell.latitude() + cell.latitudeError());
            assertThat(p[1]).isBetween(cell.longitude() - cell.longitudeError(), cell.longitude() + cell.longitudeError());
        }
    }

    @Test
    void decode_thenEncodingTheCentre_givesTheSameHash() {
        GeohashCell cell = Geohash.decode("tepfz4");
        assertThat(Geohash.encode(cell.latitude(), cell.longitude(), 6)).isEqualTo("tepfz4");
    }

    @Test
    void decode_rejectsACharacterOutsideTheAlphabet() {
        assertThatThrownBy(() -> Geohash.decode("ezs4a"))   // 'a' is not in the alphabet
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void neighbours_ofAMiddleCell_areEightDifferentCellsOneStepAway() {
        GeohashCell centre = Geohash.decode("ezs42");
        List<String> neighbours = Geohash.neighbours("ezs42");

        assertThat(neighbours).hasSize(8).doesNotHaveDuplicates().doesNotContain("ezs42");
        for (String n : neighbours) {
            assertThat(n).hasSize(5);
            GeohashCell cell = Geohash.decode(n);
            double dLat = Math.abs(cell.latitude() - centre.latitude());
            double dLng = Math.abs(cell.longitude() - centre.longitude());
            // Each neighbour is 0 or exactly one cell away in each direction (a cell is twice its error wide).
            assertThat(dLat).satisfiesAnyOf(
                    d -> assertThat(d).isCloseTo(0, within(1e-9)),
                    d -> assertThat(d).isCloseTo(2 * centre.latitudeError(), within(1e-9)));
            assertThat(dLng).satisfiesAnyOf(
                    d -> assertThat(d).isCloseTo(0, within(1e-9)),
                    d -> assertThat(d).isCloseTo(2 * centre.longitudeError(), within(1e-9)));
        }
    }

    @Test
    void neighbours_containAPointJustAcrossTheEdge() {
        // The edge problem: two points a few metres apart on either side of a cell border have different hashes.
        String hash = Geohash.encode(17.385, 78.4867, 6);
        GeohashCell cell = Geohash.decode(hash);
        double eastEdge = cell.longitude() + cell.longitudeError();

        String inside = Geohash.encode(cell.latitude(), eastEdge - 1e-6, 6);
        String outside = Geohash.encode(cell.latitude(), eastEdge + 1e-6, 6);

        assertThat(inside).isEqualTo(hash);
        assertThat(outside).isNotEqualTo(hash);
        assertThat(Geohash.neighbours(hash)).contains(outside);
    }

    @Test
    void neighbours_wrapAroundTheDateLine() {
        String hash = Geohash.encode(0, 179.99, 5);
        List<GeohashCell> cells = Geohash.neighbours(hash).stream().map(Geohash::decode).toList();

        // Some neighbours are on the other side of longitude 180, i.e. near -180.
        assertThat(cells).anyMatch(c -> c.longitude() < -179);
    }

    @Test
    void neighbours_atTheTopOfTheWorld_haveNoNorthernRow() {
        // The top row of cells has nothing north of it, so only 5 of the 8 neighbours exist.
        assertThat(Geohash.neighbours(Geohash.encode(89.99, 10, 4))).hasSize(5);
    }

    // Hyderabad is at about 17.4 N. Cell sizes there (height x width): length 5 = 4.89 x 4.66 km, length 6 = 0.61 x 1.17 km,
    // length 4 = 19.55 x 37.30 km; each is the smaller side that must be at least the radius.
    @Test
    void lengthForRadius_picksTheLongestHashWhoseCellsAreBigEnough() {
        assertThat(Geohash.lengthForRadius(2, 17.4)).isEqualTo(5);
        assertThat(Geohash.lengthForRadius(4.5, 17.4)).isEqualTo(5);
        assertThat(Geohash.lengthForRadius(4.7, 17.4)).isEqualTo(4);   // length 5 is only 4.66 km wide here
        assertThat(Geohash.lengthForRadius(0.6, 17.4)).isEqualTo(6);
        assertThat(Geohash.lengthForRadius(0.62, 17.4)).isEqualTo(5);  // length 6 is only 0.61 km tall
        assertThat(Geohash.lengthForRadius(15, 17.4)).isEqualTo(4);
        assertThat(Geohash.lengthForRadius(20, 17.4)).isEqualTo(3);    // length 4 is only 19.55 km tall
    }

    @Test
    void lengthForRadius_neverExceedsTheStoredLength() {
        assertThat(Geohash.lengthForRadius(0.001, 17.4)).isEqualTo(Geohash.STORED_LENGTH);
    }

    @Test
    void lengthForRadius_cellsAreNarrowerAwayFromTheEquator_soTheLengthCanBeShorter() {
        // At the equator length 5 is 4.89 km both ways; at 60 N its width is about half, below a 3 km radius.
        assertThat(Geohash.lengthForRadius(3, 0)).isEqualTo(5);
        assertThat(Geohash.lengthForRadius(3, 60)).isEqualTo(4);
    }

    @Test
    void lengthForRadius_rejectsANonPositiveRadius() {
        assertThatThrownBy(() -> Geohash.lengthForRadius(0, 17.4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encode_shorterHashIsAPrefixOfTheLongerOne() {
        String long9 = Geohash.encode(17.385, 78.4867, 9);
        assertThat(long9).startsWith(Geohash.encode(17.385, 78.4867, 5));
    }
}
