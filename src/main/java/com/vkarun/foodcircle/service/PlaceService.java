package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.dto.NearbyPlaceResponse;
import com.vkarun.foodcircle.dto.PlaceResponse;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.geo.Geohash;
import com.vkarun.foodcircle.geo.Haversine;
import com.vkarun.foodcircle.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Transactional(readOnly = true)
    public PlaceResponse getById(Long id) {
        return placeRepository.findById(id)
                .map(PlaceResponse::from)
                .orElseThrow(() -> ApiException.notFound("Place " + id + " not found"));
    }

    // Open places within radiusKm of (latitude, longitude), nearest first.
    @Transactional(readOnly = true)
    public List<NearbyPlaceResponse> findNearby(double latitude, double longitude, double radiusKm) {
        // 1. Which cells to read: the searcher's cell and its 8 neighbours, at the length that fits the radius.
        int length = Geohash.lengthForRadius(radiusKm, latitude);
        String centreCell = Geohash.encode(latitude, longitude, length);
        List<String> cells = new ArrayList<>();
        cells.add(centreCell);
        cells.addAll(Geohash.neighbours(centreCell));

        // 2. The coarse filter: one indexed range query per cell. These rows are close, but not all within the radius.
        List<Place> candidates = new ArrayList<>();
        for (String cell : cells) {
            // '{' is the character after 'z', so [cell, cell + '{') holds every stored hash that starts with the cell.
            candidates.addAll(placeRepository.findOpenInGeohashRange(cell, cell + "{"));
        }

        // 3. The exact filter: the cells are rectangles, the search area is a circle.
        return withinRadius(candidates, latitude, longitude, radiusKm);
    }

    // The baseline for the day-7 comparison: no index, read every open place and measure the distance to each.
    // Gives the same answer as findNearby; only the way of finding the candidates differs.
    @Transactional(readOnly = true)
    public List<NearbyPlaceResponse> findNearbyNaive(double latitude, double longitude, double radiusKm) {
        return withinRadius(placeRepository.findAllOpen(), latitude, longitude, radiusKm);
    }

    // The exact filter shared by both searches: keep places within the radius, nearest first.
    private static List<NearbyPlaceResponse> withinRadius(List<Place> candidates, double latitude, double longitude, double radiusKm) {
        record Candidate(Place place, double distanceKm) {
        }
        return candidates.stream()
                .map(place -> new Candidate(place,
                        Haversine.distanceKm(latitude, longitude, place.getLatitude(), place.getLongitude())))
                .filter(candidate -> candidate.distanceKm() <= radiusKm)   // exact, before any rounding
                .sorted(Comparator.comparingDouble(Candidate::distanceKm))
                .map(candidate -> NearbyPlaceResponse.from(candidate.place(), roundToMetres(candidate.distanceKm())))
                .toList();
    }

    private static double roundToMetres(double km) {
        return Math.round(km * 1000) / 1000.0;
    }
}
