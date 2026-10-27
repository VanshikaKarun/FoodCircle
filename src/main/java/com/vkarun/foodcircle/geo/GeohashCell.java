package com.vkarun.foodcircle.geo;

// The rectangle a geohash stands for: its centre, and how far the edges are from the centre (half the cell's size).
public record GeohashCell(double latitude, double longitude, double latitudeError, double longitudeError) {
}
