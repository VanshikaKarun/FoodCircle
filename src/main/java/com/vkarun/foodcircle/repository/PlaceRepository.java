package com.vkarun.foodcircle.repository;

import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // The importer's "does this OSM place already exist?" question (the unique pair osm_type + osm_id, D14).
    Optional<Place> findByOsmTypeAndOsmId(OsmType osmType, Long osmId);

    // Open places whose geohash lies in [from, to). A range instead of LIKE 'prefix%': a LIKE with a bound parameter can
    // lose its index when Postgres switches to a generic plan, while a range always can use it.
    @Query("select p from Place p where p.geohash >= :from and p.geohash < :to and p.closedAt is null")
    List<Place> findOpenInGeohashRange(@Param("from") String from, @Param("to") String to);
}
