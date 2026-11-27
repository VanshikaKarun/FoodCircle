package com.vkarun.foodcircle.geo;

import ch.hsr.geohash.BoundingBox;
import com.vkarun.foodcircle.common.AppConstants;
import ch.hsr.geohash.GeoHash;

import java.util.Arrays;
import java.util.List;

// Geohash helpers for the nearby search. No Spring, no database.
//
// Two implementations live in this file:
//  - CURRENT (since 2026-11-18): encode, neighbours and decode call the open-source library geohash-java
//    (ch.hsr:geohash 1.4.0, Apache 2.0). Before switching, GeohashLibraryComparisonTest checked that the library gives
//    exactly the same results as our own code: 160,000 hashes (20,000 random points x lengths 1-8, in Hyderabad and
//    worldwide) and 80,000 neighbour sets in Hyderabad, all identical. So the hashes already stored in `places` stay valid.
//  - INITIAL (2026-10-27, written by hand): kept below each method as a comment, for reference and for explaining how
//    geohash works (repeated halving of the latitude and longitude ranges).
//
// lengthForRadius is our own design choice (which cell size fits which radius) and is not part of the library.
public final class Geohash {

    private Geohash() {
    }

    // The longest hash length whose cells are still at least as big as the radius on their smaller side, so the
    // user's cell plus its 8 neighbours always cover the whole search circle. Longer = fewer rows to check.
    // Cell width shrinks away from the equator, so the user's latitude is needed.
    public static int lengthForRadius(double radiusKm, double latitude) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + radiusKm);
        }
        double kmPerDegree = 2 * Math.PI * AppConstants.Geo.EARTH_RADIUS_KM / 360;
        double lngShrink = Math.cos(Math.toRadians(latitude));

        for (int length = AppConstants.Geo.GEOHASH_STORED_LENGTH; length >= 1; length--) {
            int latBits = (5 * length) / 2;          // latitude gets the 2nd, 4th, ... bit, so the smaller share
            int lngBits = (5 * length + 1) / 2;      // longitude goes first, so it gets the larger share
            double height = 180.0 / Math.pow(2, latBits) * kmPerDegree;
            double width = 360.0 / Math.pow(2, lngBits) * kmPerDegree * lngShrink;
            if (Math.min(height, width) >= radiusKm) {
                return length;
            }
        }
        return 1;   // a radius bigger than any cell: use the biggest cells
    }

    // ---------------------------------------------------------------------------------------------------------------
    // encode
    // ---------------------------------------------------------------------------------------------------------------

    // CURRENT: open-source implementation (geohash-java).
    public static String encode(double latitude, double longitude, int length) {
        return GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, length);
    }

    /* INITIAL: own implementation (2026-10-27), replaced by the library on 2026-11-18.

    public static String encode(double latitude, double longitude, int length) {
        // The range each coordinate is still known to lie in; every bit halves one of them.
        double latMin = -90;
        double latMax = 90;
        double lngMin = -180;
        double lngMax = 180;

        StringBuilder hash = new StringBuilder();

        boolean longitudeTurn = true;
        int val = 0;
        int bitsInVal = 0;
        // YOUR PART: loop over length * 5 bits. See the notes in the chat for what each bit does.
        for (int bit = 0; bit < length * 5; bit++) {
            int answer;
            if(longitudeTurn) {
                double mid = (lngMax + lngMin) / 2;
                if (longitude >= mid) {
                    answer = 1;
                    lngMin = mid;
                } else {
                    answer = 0;
                    lngMax = mid;
                }
            } else {
                double mid = (latMax + latMin)/2;
                if(latitude>=mid) {
                    answer = 1;
                    latMin = mid;
                } else {
                    answer = 0;
                    latMax = mid;
                }
            }
            val = (val << 1) | answer;
            bitsInVal++;
            longitudeTurn = !longitudeTurn;
            if(bitsInVal == 5){
                hash.append(AppConstants.Geo.GEOHASH_ALPHABET.charAt(val));
                val = 0;
                bitsInVal = 0;
            }
        }

        return hash.toString();
    }
    */

    // ---------------------------------------------------------------------------------------------------------------
    // neighbours
    // ---------------------------------------------------------------------------------------------------------------

    // CURRENT: open-source implementation (geohash-java). The (up to) 8 cells around this one, same length.
    public static List<String> neighbours(String hash) {
        BoundingBox box = GeoHash.fromGeohashString(hash).getBoundingBox();
        double centreLatitude = box.getCenter().getLatitude();
        double cellHeight = box.getLatitudeSize();

        return Arrays.stream(GeoHash.fromGeohashString(hash).getAdjacent())
                // Past a pole the library wraps to the opposite end of the world (north of the top row it returns cells
                // at the South Pole). A real neighbour is at most one cell height away, so drop the wrapped ones; the
                // initial implementation below did the same with "past a pole there is no cell". Never hit in Hyderabad.
                .filter(n -> Math.abs(n.getBoundingBox().getCenter().getLatitude() - centreLatitude) < 1.5 * cellHeight)
                .map(GeoHash::toBase32)
                .toList();
    }

    /* INITIAL: own implementation (2026-10-27), replaced by the library on 2026-11-18.

    // The (up to) 8 cells touching this one: decode to find the centre and cell size, step one cell in each
    // direction, and encode the new centres at the same length.
    public static List<String> neighbours(String hash) {
        GeohashCell cell = decode(hash);
        double stepLat = 2 * cell.latitudeError();     // one cell's height
        double stepLng = 2 * cell.longitudeError();    // one cell's width

        List<String> result = new ArrayList<>();
        for (int north = -1; north <= 1; north++) {
            for (int east = -1; east <= 1; east++) {
                if (north == 0 && east == 0) {
                    continue;   // that is the cell itself
                }
                double lat = cell.latitude() + north * stepLat;
                if (lat > 90 || lat < -90) {
                    continue;   // past a pole there is no cell, so that neighbour does not exist
                }
                // Longitude wraps around: just east of 180 is just east of -180.
                double lng = cell.longitude() + east * stepLng;
                if (lng > 180) {
                    lng -= 360;
                } else if (lng < -180) {
                    lng += 360;
                }
                result.add(encode(lat, lng, hash.length()));
            }
        }
        return result;
    }
    */

    // ---------------------------------------------------------------------------------------------------------------
    // decode
    // ---------------------------------------------------------------------------------------------------------------

    // CURRENT: open-source implementation (geohash-java). The rectangle a hash stands for: centre and half its size.
    // Not used by the search itself; kept for tests and for explaining what a cell is.
    public static GeohashCell decode(String hash) {
        for (char c : hash.toCharArray()) {
            if (AppConstants.Geo.GEOHASH_ALPHABET.indexOf(c) < 0) {
                throw new IllegalArgumentException("Not a geohash character: " + c);
            }
        }
        BoundingBox box = GeoHash.fromGeohashString(hash).getBoundingBox();
        return new GeohashCell(
                (box.getSouthLatitude() + box.getNorthLatitude()) / 2,
                (box.getWestLongitude() + box.getEastLongitude()) / 2,
                box.getLatitudeSize() / 2,
                box.getLongitudeSize() / 2);
    }

    /* INITIAL: own implementation (2026-10-27), replaced by the library on 2026-11-18.

    // The reverse of encode: replay the same halving, but the bits now come from the characters.
    public static GeohashCell decode(String hash) {
        double latMin = -90;
        double latMax = 90;
        double lngMin = -180;
        double lngMax = 180;

        boolean longitudeTurn = true;

        for (char c : hash.toCharArray()) {
            int value = AppConstants.Geo.GEOHASH_ALPHABET.indexOf(c);   // the character's position, 0..31
            if (value < 0) {
                throw new IllegalArgumentException("Not a geohash character: " + c);
            }
            // Read the 5 bits of the value from the left-most to the right-most.
            for (int i = 4; i >= 0; i--) {
                int answer = (value >> i) & 1;
                if (longitudeTurn) {
                    double mid = (lngMin + lngMax) / 2;
                    if (answer == 1) {
                        lngMin = mid;
                    } else {
                        lngMax = mid;
                    }
                } else {
                    double mid = (latMin + latMax) / 2;
                    if (answer == 1) {
                        latMin = mid;
                    } else {
                        latMax = mid;
                    }
                }
                longitudeTurn = !longitudeTurn;
            }
        }

        return new GeohashCell(
                (latMin + latMax) / 2,
                (lngMin + lngMax) / 2,
                (latMax - latMin) / 2,
                (lngMax - lngMin) / 2);
    }
    */
}
