package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.ImportResult;
import com.vkarun.foodcircle.dto.OsmPlaceData;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.Optional;

// The coordinator: reads the Overpass file, asks the parser about each entry, saves through the repository.
@Service
public class OsmImporter {

    private final OsmElementParser parser;
    private final PlaceRepository placeRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public OsmImporter(OsmElementParser parser, PlaceRepository placeRepository) {
        this.parser = parser;
        this.placeRepository = placeRepository;
    }

    // One transaction for the whole file: either every place is saved or none (a crash half-way leaves no half import).
    @Transactional
    public ImportResult importFile(Path file) {
        JsonNode elements = jsonMapper.readTree(file).path("elements");

        int added = 0;
        int updated = 0;
        int skipped = 0;

        for (JsonNode element : elements) {
            Optional<OsmPlaceData> parsed = parser.parse(element);
            if (parsed.isEmpty()) {
                skipped++;
                continue;
            }
            OsmPlaceData data = parsed.get();

            // Upsert: look the place up by its OSM identity; update it if found, insert it if not.
            Optional<Place> existing = placeRepository.findByOsmTypeAndOsmId(data.osmType(), data.osmId());
            if (existing.isPresent()) {
                existing.get().updateFromOsm(data.name(), data.category(), data.cuisine(),
                        data.latitude(), data.longitude(), data.address());
                updated++;
            } else {
                placeRepository.save(new Place(data.osmType(), data.osmId(), data.name(), data.category(),
                        data.cuisine(), data.latitude(), data.longitude(), data.address()));
                added++;
            }
        }
        return new ImportResult(added, updated, skipped);
    }
}
