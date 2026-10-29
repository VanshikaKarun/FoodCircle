package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.dto.NearbyPlaceResponse;
import com.vkarun.foodcircle.dto.PlaceResponse;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.geo.Geohash;
import com.vkarun.foodcircle.geo.Haversine;
import com.vkarun.foodcircle.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PlaceService {

    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    // A one-letter search would match most of the table.
    static final int MIN_NAME_SEARCH_LENGTH = 2;

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
        List<NearbyPlaceResponse> result = withinRadius(candidates, latitude, longitude, radiusKm);
        // candidates vs result shows how much the 3x3 block over-reads. Coordinates are left out on purpose (user location).
        log.debug("nearby radius={}km geohash length={} cells={} -> {} read, {} within radius",
                radiusKm, length, cells.size(), candidates.size(), result.size());
        return result;
    }

    // Open places whose name contains the text, ignoring case, sorted by name.
    @Transactional(readOnly = true)
    public List<PlaceResponse> searchByName(String name, int limit) {
        String text = name.trim();
        if (text.length() < MIN_NAME_SEARCH_LENGTH) {   // the controller checks the raw text; spaces around it do not count
            throw ApiException.badRequest("Search text must have at least " + MIN_NAME_SEARCH_LENGTH + " characters");
        }
        List<PlaceResponse> result = placeRepository.findOpenByNamePattern(likePattern(text), Limit.of(limit))
                .stream()
                .map(PlaceResponse::from)
                .toList();
        log.debug("name search \"{}\" limit={} -> {} results", text, limit, result.size());
        return result;
    }

    // "%text%" for LIKE. In LIKE, % and _ are wildcards and \ is our escape character, so each of them typed by the
    // user gets a backslash and is then matched as plain text. The backslash must be escaped first, or the backslashes
    // added for % and _ would be doubled.
    static String likePattern(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    // The baseline for the day-7 comparison: no index, read every open place and measure the distance to each.
    // Gives the same answer as findNearby; only the way of finding the candidates differs.
    @Transactional(readOnly = true)
    public List<NearbyPlaceResponse> findNearbyNaive(double latitude, double longitude, double radiusKm) {
        List<Place> all = placeRepository.findAllOpen();
        List<NearbyPlaceResponse> result = withinRadius(all, latitude, longitude, radiusKm);
        log.debug("nearby-naive radius={}km -> {} read, {} within radius", radiusKm, all.size(), result.size());
        return result;
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
