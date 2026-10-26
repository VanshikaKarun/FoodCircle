package com.vkarun.foodcircle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

import java.time.Instant;

// Java fields are camelCase; every column name is written out explicitly (D18).
@Entity
@Table(name = "places",
        uniqueConstraints = @UniqueConstraint(name = "uk_places_osm", columnNames = {"osm_type", "osm_id"}))
@Check(constraints = "latitude between -90 and 90 and longitude between -180 and 180")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "osm_type", nullable = false, length = 10)
    private OsmType osmType;

    @Column(name = "osm_id", nullable = false)
    private Long osmId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PlaceCategory category;

    // Optional: OSM often has none, and may hold several values joined by ';' (e.g. "indian;chinese").
    @Column(name = "cuisine", length = 100)
    private String cuisine;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Set when the place shuts down; closed places are kept (reviews refer to them) but not shown.
    @Column(name = "closed_at")
    private Instant closedAt;

    // Required by JPA (Hibernate creates objects through it); not for use in our own code.
    protected Place() {
    }

    public Place(OsmType osmType, Long osmId, String name, PlaceCategory category, String cuisine,
                 double latitude, double longitude, String address) {
        this.osmType = osmType;
        this.osmId = osmId;
        this.name = name;
        this.category = category;
        this.cuisine = cuisine;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.createdAt = Instant.now();
    }

    // Used by the OSM re-import: refresh what OSM may have changed. The identity (osmType, osmId), id and createdAt stay.
    public void updateFromOsm(String name, PlaceCategory category, String cuisine,
                              double latitude, double longitude, String address) {
        this.name = name;
        this.category = category;
        this.cuisine = cuisine;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public void markClosed() {
        this.closedAt = Instant.now();
    }

    public boolean isClosed() {
        return closedAt != null;
    }

    public Long getId() {
        return id;
    }

    public OsmType getOsmType() {
        return osmType;
    }

    public Long getOsmId() {
        return osmId;
    }

    public String getName() {
        return name;
    }

    public PlaceCategory getCategory() {
        return category;
    }

    public String getCuisine() {
        return cuisine;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
