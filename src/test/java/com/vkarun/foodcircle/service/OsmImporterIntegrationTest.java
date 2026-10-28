package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.ImportResult;
import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.geo.Geohash;
import com.vkarun.foodcircle.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// The importer end to end against PostgreSQL (foodcircle_test): a small Overpass-style file, run twice.
// 1,200 named places so that the periodic flush/clear (every 500) is exercised more than once.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OsmImporterIntegrationTest {

    private static final int NAMED = 1_200;

    @Autowired
    private OsmImporter importer;

    @Autowired
    private PlaceRepository placeRepository;

    private Path writeFile(Path dir, String nameOfPlace7) throws IOException {
        StringBuilder json = new StringBuilder("{\"elements\":[");
        for (int i = 1; i <= NAMED; i++) {
            String name = i == 7 ? nameOfPlace7 : "Place " + i;
            json.append("{\"type\":\"node\",\"id\":").append(i)
                    .append(",\"lat\":").append(17.2 + (i % 400) * 0.001)
                    .append(",\"lon\":").append(78.25 + (i % 397) * 0.001)
                    .append(",\"tags\":{\"amenity\":\"cafe\",\"name\":\"").append(name).append("\"}},");
        }
        json.append("{\"type\":\"way\",\"id\":5000,\"center\":{\"lat\":17.4,\"lon\":78.4},")
                .append("\"tags\":{\"amenity\":\"restaurant\",\"name\":\"A building\",\"cuisine\":\"indian\"}},")
                .append("{\"type\":\"node\",\"id\":9001,\"lat\":17.4,\"lon\":78.4,\"tags\":{\"amenity\":\"cafe\"}},")        // no name
                .append("{\"type\":\"node\",\"id\":9002,\"lat\":17.4,\"lon\":78.4,\"tags\":{\"amenity\":\"pub\",\"name\":\"X\"}},") // not a food amenity
                .append("{\"type\":\"node\",\"id\":9003,\"tags\":{\"amenity\":\"cafe\",\"name\":\"Nowhere\"}}")                 // no coordinates
                .append("]}");
        Path file = dir.resolve("overpass.json");
        Files.writeString(file, json);
        return file;
    }

    @Test
    void firstRunAddsEverythingUsable_secondRunUpdatesTheSameRows(@TempDir Path dir) throws IOException {
        Path file = writeFile(dir, "Place 7");

        ImportResult first = importer.importFile(file);
        assertThat(first).isEqualTo(new ImportResult(NAMED + 1, 0, 3));
        assertThat(placeRepository.count()).isEqualTo(NAMED + 1);

        ImportResult second = importer.importFile(file);
        assertThat(second).isEqualTo(new ImportResult(0, NAMED + 1, 3));
        assertThat(placeRepository.count()).isEqualTo(NAMED + 1);   // idempotent: no duplicates
    }

    @Test
    void aRerunRefreshesWhatOsmChanged_andKeepsTheRowIdentity(@TempDir Path dir) throws IOException {
        importer.importFile(writeFile(dir, "Old Name"));
        Long idBefore = placeRepository.findByOsmTypeAndOsmId(OsmType.NODE, 7L).orElseThrow().getId();

        importer.importFile(writeFile(dir, "New Name"));

        Place after = placeRepository.findByOsmTypeAndOsmId(OsmType.NODE, 7L).orElseThrow();
        assertThat(after.getName()).isEqualTo("New Name");
        assertThat(after.getId()).isEqualTo(idBefore);   // reviews point at this id, so it must not change
    }

    @Test
    void importedPlacesGetTheirMappedFieldsAndAGeohash(@TempDir Path dir) throws IOException {
        importer.importFile(writeFile(dir, "Place 7"));

        Place way = placeRepository.findByOsmTypeAndOsmId(OsmType.WAY, 5000L).orElseThrow();
        assertThat(way.getName()).isEqualTo("A building");
        assertThat(way.getCategory()).isEqualTo(PlaceCategory.RESTAURANT);
        assertThat(way.getCuisine()).isEqualTo("indian");
        assertThat(way.getLatitude()).isEqualTo(17.4);                      // taken from "center"
        assertThat(way.getGeohash()).isEqualTo(Geohash.encode(17.4, 78.4, Geohash.STORED_LENGTH));
        assertThat(placeRepository.findByOsmTypeAndOsmId(OsmType.NODE, 9001L)).isEmpty();   // skipped: no name
    }
}
