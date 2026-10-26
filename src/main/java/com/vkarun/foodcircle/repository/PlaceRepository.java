package com.vkarun.foodcircle.repository;

import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // The importer's "does this OSM place already exist?" question (the unique pair osm_type + osm_id, D14).
    Optional<Place> findByOsmTypeAndOsmId(OsmType osmType, Long osmId);
}
