package com.vkarun.foodcircle.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// The expected values come from the geometry of the sphere itself (a circle of radius R has circumference 2*pi*R),
// not from a website, so they check the formula and the radius constant.
class HaversineTest {

    private static final double R = 6371.0088;

    @Test
    void samePoint_isZero() {
        assertThat(Haversine.distanceKm(17.385, 78.4867, 17.385, 78.4867)).isZero();
    }

    @Test
    void oneDegreeAlongTheEquator_isOneThreeHundredSixtiethOfTheCircumference() {
        double expected = 2 * Math.PI * R / 360;   // about 111.19 km
        assertThat(Haversine.distanceKm(0, 0, 0, 1)).isCloseTo(expected, within(0.001));
    }

    @Test
    void oneDegreeOfLatitude_isTheSameEverywhere() {
        double expected = 2 * Math.PI * R / 360;
        assertThat(Haversine.distanceKm(17, 78, 18, 78)).isCloseTo(expected, within(0.001));
    }

    @Test
    void oppositeSidesOfTheEarth_isHalfTheCircumference() {
        assertThat(Haversine.distanceKm(0, 0, 0, 180)).isCloseTo(Math.PI * R, within(0.001));
    }

    @Test
    void distanceIsTheSameInBothDirections() {
        double there = Haversine.distanceKm(17.385, 78.4867, 17.45, 78.38);
        double back = Haversine.distanceKm(17.45, 78.38, 17.385, 78.4867);
        assertThat(there).isEqualTo(back);
    }

    @Test
    void aDegreeOfLongitudeIsShorterAwayFromTheEquator() {
        // Meridians converge towards the poles: at Hyderabad's latitude one degree of longitude is about cos(17 deg) of the equator's.
        double atEquator = Haversine.distanceKm(0, 78, 0, 79);
        double atHyderabad = Haversine.distanceKm(17, 78, 17, 79);
        assertThat(atHyderabad).isCloseTo(atEquator * Math.cos(Math.toRadians(17)), within(0.1));
    }
}
