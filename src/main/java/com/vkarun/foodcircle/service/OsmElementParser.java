package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.OsmPlaceData;
import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.PlaceCategory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Pure translation, no database: one element of the Overpass "elements" array -> OsmPlaceData.
// Returns empty for entries we cannot use (no name, unknown amenity, no coordinates).
@Component
public class OsmElementParser {

    // Column lengths of places (name 200, cuisine 100, address 300); longer text is cut instead of failing the insert.
    private static final int MAX_NAME = 200;
    private static final int MAX_CUISINE = 100;
    private static final int MAX_ADDRESS = 300;

    public Optional<OsmPlaceData> parse(JsonNode element) {
        JsonNode tags = element.path("tags");

        String name = text(tags, "name");
        if (name == null) {
            return Optional.empty();
        }

        Optional<PlaceCategory> category = category(text(tags, "amenity"));
        if (category.isEmpty()) {
            return Optional.empty();
        }

        // A node carries lat/lon itself; a way (a building outline) carries them inside "center".
        JsonNode position = element.has("center") ? element.get("center") : element;
        if (!position.hasNonNull("lat") || !position.hasNonNull("lon")) {
            return Optional.empty();
        }

        OsmType osmType = OsmType.valueOf(element.get("type").asString().toUpperCase(Locale.ROOT));

        return Optional.of(new OsmPlaceData(
                osmType,
                element.get("id").asLong(),
                cut(name, MAX_NAME),
                category.get(),
                cut(text(tags, "cuisine"), MAX_CUISINE),
                position.get("lat").asDouble(),
                position.get("lon").asDouble(),
                cut(address(tags), MAX_ADDRESS)));
    }

    private Optional<PlaceCategory> category(String amenity) {
        if (amenity == null) {
            return Optional.empty();
        }
        return switch (amenity) {
            case "restaurant" -> Optional.of(PlaceCategory.RESTAURANT);
            case "cafe" -> Optional.of(PlaceCategory.CAFE);
            case "fast_food" -> Optional.of(PlaceCategory.FAST_FOOD);
            default -> Optional.empty();
        };
    }

    // Only 4 places have addr:full; the rest have parts, so join whichever parts exist.
    private String address(JsonNode tags) {
        String full = text(tags, "addr:full");
        if (full != null) {
            return full;
        }
        List<String> parts = new ArrayList<>();
        for (String key : List.of("addr:housenumber", "addr:street", "addr:suburb", "addr:city", "addr:postcode")) {
            String part = text(tags, key);
            if (part != null) {
                parts.add(part);
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    // The tag's value trimmed, or null when it is missing or blank.
    private String text(JsonNode tags, String key) {
        JsonNode value = tags.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        String trimmed = value.asString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String cut(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
