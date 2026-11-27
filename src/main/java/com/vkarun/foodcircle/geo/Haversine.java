package com.vkarun.foodcircle.geo;

import com.vkarun.foodcircle.common.AppConstants;

// Distance between two points on the Earth's surface (a sphere), no Spring and no database.
public final class Haversine {

    private Haversine() {
    }

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        // The formula needs radians, not degrees.
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);

        return 2 * AppConstants.Geo.EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }
}
