package com.example.oncampusapp;

import static org.junit.Assert.*;
import com.google.android.gms.maps.model.LatLng;
import org.junit.Test;
import java.util.*;

public class BuildingLookupTest {

    private Building makeBuildingWithCenter(String id, String name, LatLng... points) {
        return new Building(id, name, Arrays.asList(points));
    }

    @Test
    public void returnsNull_whenNameIsNull() {
        Map<String, Building> map = new HashMap<>();
        LatLng result = BuildingLookup.getLatLngFromBuildingName(null, map);
        assertNull(result);
    }

    @Test
    public void returnsNull_whenNameIsEmpty() {
        Map<String, Building> map = new HashMap<>();
        LatLng result = BuildingLookup.getLatLngFromBuildingName("", map);
        assertNull(result);
    }

    @Test
    public void returnsNull_whenBuildingsMapIsNull() {
        LatLng result = BuildingLookup.getLatLngFromBuildingName("SGW", null);
        assertNull(result);
    }

    @Test
    public void returnsCenter_whenNameMatchesIgnoringCase() {
        // Center should be (1,1)
        Building b = makeBuildingWithCenter(
                "1",
                "SGW",
                new LatLng(0, 0),
                new LatLng(2, 2)
        );

        Map<String, Building> map = new HashMap<>();
        map.put("sgw", b);

        LatLng result = BuildingLookup.getLatLngFromBuildingName("sgw", map);

        assertNotNull(result);
        assertEquals(1.0, result.latitude, 0.0001);
        assertEquals(1.0, result.longitude, 0.0001);
    }

    @Test
    public void skipsNullBuildings_andStillFindsMatch() {
        Building match = makeBuildingWithCenter(
                "2",
                "Loyola",
                new LatLng(10, 10),
                new LatLng(14, 14) // center (12,12)
        );

        Map<String, Building> map = new LinkedHashMap<>();
        map.put("nullBuilding", null);
        map.put("match", match);

        LatLng result = BuildingLookup.getLatLngFromBuildingName("LOYOLA", map);

        assertNotNull(result);
        assertEquals(12.0, result.latitude, 0.0001);
        assertEquals(12.0, result.longitude, 0.0001);
    }

    @Test
    public void skipsBuildingsWithNullName_andStillFindsMatch() {
        Building nullName = makeBuildingWithCenter(
                "3",
                null,
                new LatLng(0, 0),
                new LatLng(2, 2)
        );

        Building match = makeBuildingWithCenter(
                "4",
                "Hall",
                new LatLng(5, 5),
                new LatLng(9, 9) // center (7,7)
        );

        Map<String, Building> map = new LinkedHashMap<>();
        map.put("nullName", nullName);
        map.put("match", match);

        LatLng result = BuildingLookup.getLatLngFromBuildingName("hall", map);

        assertNotNull(result);
        assertEquals(7.0, result.latitude, 0.0001);
        assertEquals(7.0, result.longitude, 0.0001);
    }

    @Test
    public void returnsNull_whenNoMatchFound() {
        Building b = makeBuildingWithCenter(
                "5",
                "SGW",
                new LatLng(0, 0),
                new LatLng(2, 2)
        );

        Map<String, Building> map = new HashMap<>();
        map.put("sgw", b);

        LatLng result = BuildingLookup.getLatLngFromBuildingName("NotARealBuilding", map);
        assertNull(result);
    }
}

