package com.example.oncampusapp;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BuildingLookupTest {

    // ── Null / empty guards ───────────────────────────────────────────────────

    @Test
    public void nullName_returnsNull() {
        assertNull(BuildingLookup.getLatLngFromBuildingName(null, new HashMap<>()));
    }

    @Test
    public void emptyName_returnsNull() {
        assertNull(BuildingLookup.getLatLngFromBuildingName("", new HashMap<>()));
        assertNull(BuildingLookup.getLatLngFromBuildingName("   ", new HashMap<>()));
    }

    @Test
    public void nullMap_returnsNull() {
        assertNull(BuildingLookup.getLatLngFromBuildingName("Hall Building", null));
    }

    @Test
    public void emptyMap_returnsNull() {
        assertNull(BuildingLookup.getLatLngFromBuildingName("Hall Building", new HashMap<>()));
    }

    // ── Pass 1: exact case-insensitive match ──────────────────────────────────

    @Test
    public void exactMatch_returnsCenter() {
        Map<String, Building> map = mapOf("H", "Hall Building",
                new LatLng(45.497, -73.578), new LatLng(45.498, -73.579));

        LatLng result = BuildingLookup.getLatLngFromBuildingName("Hall Building", map);
        assertNotNull(result);
        assertEquals(45.4975, result.latitude, 0.0001);
    }

    @Test
    public void exactMatch_caseInsensitive() {
        Map<String, Building> map = mapOf("H", "Hall Building",
                new LatLng(45.497, -73.578), new LatLng(45.498, -73.579));

        assertNotNull(BuildingLookup.getLatLngFromBuildingName("hall building", map));
        assertNotNull(BuildingLookup.getLatLngFromBuildingName("HALL BUILDING", map));
    }

    @Test
    public void exactMatch_leadingTrailingSpaces_trimmed() {
        Map<String, Building> map = mapOf("H", "Hall Building",
                new LatLng(45.497, -73.578), new LatLng(45.498, -73.579));

        assertNotNull(BuildingLookup.getLatLngFromBuildingName("  Hall Building  ", map));
    }

    // ── Building with null/empty polygon → no center → skipped ───────────────

    @Test
    public void buildingWithNullPolygon_skipped_returnsNull() {
        Map<String, Building> map = new HashMap<>();
        map.put("H", new Building("H", "Hall Building", null));

        assertNull(BuildingLookup.getLatLngFromBuildingName("Hall Building", map));
    }

    @Test
    public void buildingWithEmptyPolygon_skipped_returnsNull() {
        Map<String, Building> map = new HashMap<>();
        map.put("H", new Building("H", "Hall Building", Collections.emptyList()));

        assertNull(BuildingLookup.getLatLngFromBuildingName("Hall Building", map));
    }

    // ── Pass 2: startsWith fallback ───────────────────────────────────────────

    @Test
    public void startsWithFallback_partialName_returnsCenter() {
        Map<String, Building> map = mapOf("H", "Hall Building",
                new LatLng(45.497, -73.578), new LatLng(45.498, -73.579));

        assertNotNull(BuildingLookup.getLatLngFromBuildingName("Hall", map));
    }

    @Test
    public void startsWithFallback_caseInsensitive() {
        Map<String, Building> map = mapOf("MB", "John Molson School of Business",
                new LatLng(45.495, -73.579), new LatLng(45.496, -73.580));

        assertNotNull(BuildingLookup.getLatLngFromBuildingName("john molson", map));
    }

    // ── Pass 3: contains fallback ─────────────────────────────────────────────

    @Test
    public void containsFallback_middleOfName_returnsCenter() {
        Map<String, Building> map = mapOf("H", "Hall Building",
                new LatLng(45.497, -73.578), new LatLng(45.498, -73.579));

        assertNotNull(BuildingLookup.getLatLngFromBuildingName("Building", map));
    }

    // ── No match at all ───────────────────────────────────────────────────────

    @Test
    public void noMatch_returnsNull() {
        Map<String, Building> map = mapOf("H", "Hall Building",
                new LatLng(45.497, -73.578), new LatLng(45.498, -73.579));

        assertNull(BuildingLookup.getLatLngFromBuildingName("Library Building", map));
    }

    // ── Center calculation accuracy ───────────────────────────────────────────

    @Test
    public void centerCalculation_averagesAllPolygonPoints() {
        Map<String, Building> map = new HashMap<>();
        map.put("H", new Building("H", "Hall Building", Arrays.asList(
                new LatLng(10.0, 20.0),
                new LatLng(20.0, 40.0),
                new LatLng(30.0, 60.0)
        )));

        LatLng center = BuildingLookup.getLatLngFromBuildingName("Hall Building", map);
        assertNotNull(center);
        assertEquals(20.0, center.latitude, 0.0001);
        assertEquals(40.0, center.longitude, 0.0001);
    }

    // ── Null entry in map values ───────────────────────────────────────────────

    @Test
    public void mapWithNullEntry_doesNotThrow() {
        Map<String, Building> map = new HashMap<>();
        map.put("X", null);
        map.put("H", new Building("H", "Hall Building",
                Arrays.asList(new LatLng(45.497, -73.578), new LatLng(45.498, -73.579))));

        assertNotNull(BuildingLookup.getLatLngFromBuildingName("Hall Building", map));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Map<String, Building> mapOf(String id, String name, LatLng... polygon) {
        Map<String, Building> map = new HashMap<>();
        map.put(id, new Building(id, name, Arrays.asList(polygon)));
        return map;
    }
}
