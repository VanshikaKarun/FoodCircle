package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.OsmPlaceData;
import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.PlaceCategory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OsmElementParserTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final OsmElementParser parser = new OsmElementParser();

    private Optional<OsmPlaceData> parse(String json) {
        JsonNode element = mapper.readTree(json);
        return parser.parse(element);
    }

    @Test
    void node_usesItsOwnCoordinates() {
        OsmPlaceData place = parse("""
                {"type":"node","id":1,"lat":17.44,"lon":78.49,
                 "tags":{"amenity":"cafe","name":"Chai Point","cuisine":"coffee_shop"}}""").orElseThrow();

        assertThat(place.osmType()).isEqualTo(OsmType.NODE);
        assertThat(place.osmId()).isEqualTo(1);
        assertThat(place.category()).isEqualTo(PlaceCategory.CAFE);
        assertThat(place.cuisine()).isEqualTo("coffee_shop");
        assertThat(place.latitude()).isEqualTo(17.44);
        assertThat(place.longitude()).isEqualTo(78.49);
    }

    @Test
    void way_usesCenterCoordinates() {
        OsmPlaceData place = parse("""
                {"type":"way","id":2,"center":{"lat":17.1,"lon":78.2},
                 "tags":{"amenity":"fast_food","name":"Palash Mess"}}""").orElseThrow();

        assertThat(place.osmType()).isEqualTo(OsmType.WAY);
        assertThat(place.category()).isEqualTo(PlaceCategory.FAST_FOOD);
        assertThat(place.latitude()).isEqualTo(17.1);
        assertThat(place.longitude()).isEqualTo(78.2);
        assertThat(place.cuisine()).isNull();
    }

    @Test
    void unnamedOrBlankNamed_isSkipped() {
        assertThat(parse("""
                {"type":"node","id":3,"lat":1,"lon":1,"tags":{"amenity":"cafe"}}""")).isEmpty();
        assertThat(parse("""
                {"type":"node","id":4,"lat":1,"lon":1,"tags":{"amenity":"cafe","name":"  "}}""")).isEmpty();
    }

    @Test
    void unknownAmenityOrMissingCoordinates_isSkipped() {
        assertThat(parse("""
                {"type":"node","id":5,"lat":1,"lon":1,"tags":{"amenity":"pub","name":"X"}}""")).isEmpty();
        assertThat(parse("""
                {"type":"node","id":6,"tags":{"amenity":"cafe","name":"X"}}""")).isEmpty();
    }

    @Test
    void address_joinsTheAvailableParts() {
        OsmPlaceData place = parse("""
                {"type":"node","id":7,"lat":1,"lon":1,
                 "tags":{"amenity":"cafe","name":"X","addr:street":"Road No 1","addr:city":"Hyderabad"}}""").orElseThrow();

        assertThat(place.address()).isEqualTo("Road No 1, Hyderabad");
    }
}
