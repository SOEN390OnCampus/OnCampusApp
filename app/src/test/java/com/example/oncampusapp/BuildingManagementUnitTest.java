package com.example.oncampusapp;

import static org.junit.Assert.*;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class BuildingManagementUnitTest {

    private BuildingManager buildingManager;

    @Before
    public void setUp() {
        // This re-initializes the static buildingsMap inside BuildingManager for a fresh start each test
        buildingManager = new BuildingManager();
    }

    // Helper method to make creating test buildings cleaner
    private Building makeBuildingWithCenter(String id, String name, LatLng... points) {
        return new Building(id, name, points == null ? null : Arrays.asList(points));
    }

    // ==========================================
    // TESTS FOR: Building.java (getCenter)
    // ==========================================

    @Test
    public void building_getCenter_returnsCorrectAverage() {
        Building building = makeBuildingWithCenter("1", "Test", new LatLng(0, 0), new LatLng(2, 2));
        LatLng center = building.getCenter();

        assertEquals(1.0, center.latitude, 0.0001);
        assertEquals(1.0, center.longitude, 0.0001);
    }

    @Test
    public void building_getCenter_nullPolygon_returnsNull() {
        // Changed (LatLng[]) to (List<LatLng>)
        Building building = new Building("1", "Test", (java.util.List<LatLng>) null);
        assertNull(building.getCenter());
    }

    @Test
    public void building_getCenter_emptyPolygon_returnsNull() {
        Building building = new Building("1", "Test", new ArrayList<>());
        assertNull(building.getCenter());
    }

    // ==========================================
    // TESTS FOR: BuildingManager.java (getCurrentBuilding)
    // ==========================================

    @Test
    public void manager_returnsBuildingWhenUserInside() {
        Building building1 = makeBuildingWithCenter("1", "Hall", new LatLng(0, 0));
        buildingManager.addBuilding(building1);

        building1.currentlyInside = true;
        Building result = buildingManager.getCurrentBuilding();

        assertNotNull(result);
        assertEquals("Hall", result.getName());
    }

    @Test
    public void manager_returnsFirstBuildingIfMultipleInside() {
        Building building1 = makeBuildingWithCenter("1", "Hall", new LatLng(0, 0));
        Building building2 = makeBuildingWithCenter("2", "JMSB", new LatLng(0, 0));

        buildingManager.addBuilding(building1);
        buildingManager.addBuilding(building2);

        building1.currentlyInside = true;
        building2.currentlyInside = true;

        Building result = buildingManager.getCurrentBuilding();

        assertNotNull(result);
        assertTrue(result == building1 || result == building2);
    }

    @Test
    public void manager_returnsNullWhenUserNotInsideAnyBuilding() {
        Building building1 = makeBuildingWithCenter("1", "Hall", new LatLng(0, 0));
        buildingManager.addBuilding(building1);

        building1.currentlyInside = false;

        assertNull(buildingManager.getCurrentBuilding());
    }

    // ==========================================
    // TESTS FOR: BuildingLookup.java
    // ==========================================

    @Test
    public void lookup_returnsNull_whenNameIsNull() {
        assertNull(BuildingLookup.getLatLngFromBuildingName(null, BuildingManager.buildingsMap));
    }

    @Test
    public void lookup_returnsNull_whenNameIsEmpty() {
        assertNull(BuildingLookup.getLatLngFromBuildingName("", BuildingManager.buildingsMap));
    }

    @Test
    public void lookup_returnsNull_whenBuildingsMapIsNull() {
        assertNull(BuildingLookup.getLatLngFromBuildingName("SGW", null));
    }

    @Test
    public void lookup_returnsCenter_whenNameMatchesIgnoringCase() {
        Building b = makeBuildingWithCenter("1", "SGW", new LatLng(0, 0), new LatLng(2, 2));

        // Utilizing the BuildingManager just like the reviewer requested
        buildingManager.addBuilding(b);

        LatLng result = BuildingLookup.getLatLngFromBuildingName("sgw", BuildingManager.buildingsMap);

        assertNotNull(result);
        assertEquals(1.0, result.latitude, 0.0001);
        assertEquals(1.0, result.longitude, 0.0001);
    }

    @Test
    public void lookup_skipsNullBuildings_andStillFindsMatch() {
        Building match = makeBuildingWithCenter("2", "Loyola", new LatLng(10, 10), new LatLng(14, 14));

        // Simulating a corrupt/null entry in the map
        BuildingManager.buildingsMap.put("nullBuilding", null);
        buildingManager.addBuilding(match);

        LatLng result = BuildingLookup.getLatLngFromBuildingName("LOYOLA", BuildingManager.buildingsMap);

        assertNotNull(result);
        assertEquals(12.0, result.latitude, 0.0001);
        assertEquals(12.0, result.longitude, 0.0001);
    }

    @Test
    public void lookup_skipsBuildingsWithNullName_andStillFindsMatch() {
        Building nullName = makeBuildingWithCenter("3", null, new LatLng(0, 0), new LatLng(2, 2));
        Building match = makeBuildingWithCenter("4", "Hall", new LatLng(5, 5), new LatLng(9, 9));

        buildingManager.addBuilding(nullName);
        buildingManager.addBuilding(match);

        LatLng result = BuildingLookup.getLatLngFromBuildingName("hall", BuildingManager.buildingsMap);

        assertNotNull(result);
        assertEquals(7.0, result.latitude, 0.0001);
        assertEquals(7.0, result.longitude, 0.0001);
    }

    @Test
    public void lookup_returnsNull_whenNoMatchFound() {
        Building b = makeBuildingWithCenter("5", "SGW", new LatLng(0, 0), new LatLng(2, 2));
        buildingManager.addBuilding(b);

        LatLng result = BuildingLookup.getLatLngFromBuildingName("NotARealBuilding", BuildingManager.buildingsMap);
        assertNull(result);
    }
}