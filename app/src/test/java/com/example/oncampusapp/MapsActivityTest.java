package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MapsActivityTest {

    @Before
    public void setUp() {
        // Clear buildingsMap before each test to ensure test isolation
        MapsActivity.buildingsMap.clear();
    }

    /**
     * Test that SGW campus coordinates are correctly defined
     */
    @Test
    public void testSGW_Coordinates_AreCorrect() {
        assertEquals("SGW latitude should be correct", 
            45.496107243097704, MapsActivity.SGW_COORDS.latitude, 0.000001);
        assertEquals("SGW longitude should be correct", 
            -73.57725834380621, MapsActivity.SGW_COORDS.longitude, 0.000001);
    }

    /**
     * Test that Loyola campus coordinates are correctly defined
     */
    @Test
    public void testLOY_Coordinates_AreCorrect() {
        assertEquals("Loyola latitude should be correct", 
            45.4582, MapsActivity.LOY_COORDS.latitude, 0.000001);
        assertEquals("Loyola longitude should be correct", 
            -73.6405, MapsActivity.LOY_COORDS.longitude, 0.000001);
    }

    /**
     * Test that buildingsMap is initialized as an empty HashMap
     */
    @Test
    public void testBuildingsMap_IsInitialized() {
        assertNotNull("buildingsMap should not be null", MapsActivity.buildingsMap);
    }

    /**
     * Test adding a building to buildingsMap
     */
    @Test
    public void testBuildingsMap_CanAddBuilding() {
        MapsActivity.buildingsMap.clear();
        
        List<LatLng> coordinates = new ArrayList<>();
        coordinates.add(new LatLng(45.4970, -73.5790));
        coordinates.add(new LatLng(45.4971, -73.5791));
        coordinates.add(new LatLng(45.4972, -73.5792));
        
        Building building = new Building("test_id_123", "Test Building", coordinates);
        MapsActivity.buildingsMap.put("test_id_123", building);
        
        assertEquals("buildingsMap should contain one building", 1, MapsActivity.buildingsMap.size());
        assertTrue("buildingsMap should contain the test building", 
            MapsActivity.buildingsMap.containsKey("test_id_123"));
        
        Building retrievedBuilding = MapsActivity.buildingsMap.get("test_id_123");
        assertNotNull("Retrieved building should not be null", retrievedBuilding);
        assertEquals("Building ID should match", "test_id_123", retrievedBuilding.getId());
        assertEquals("Building name should match", "Test Building", retrievedBuilding.name);
        assertEquals("Building coordinates should match", 3, retrievedBuilding.polygon.size());
    }

    /**
     * Test removing a building from buildingsMap
     */
    @Test
    public void testBuildingsMap_CanRemoveBuilding() {
        MapsActivity.buildingsMap.clear();
        
        List<LatLng> coordinates = new ArrayList<>();
        coordinates.add(new LatLng(45.4970, -73.5790));
        
        Building building = new Building("remove_test", "Remove Test", coordinates);
        MapsActivity.buildingsMap.put("remove_test", building);
        assertEquals("buildingsMap should contain one building", 1, MapsActivity.buildingsMap.size());
        
        MapsActivity.buildingsMap.remove("remove_test");
        assertEquals("buildingsMap should be empty after removal", 0, MapsActivity.buildingsMap.size());
    }

    /**
     * Test Building constructor initializes all fields correctly
     */
    @Test
    public void testBuilding_Constructor_InitializesFieldsCorrectly() {
        List<LatLng> coordinates = new ArrayList<>();
        coordinates.add(new LatLng(45.4970, -73.5790));
        coordinates.add(new LatLng(45.4971, -73.5791));
        
        Building building = new Building("hall123", "Hall Building", coordinates);
        
        assertEquals("Building ID should be set correctly", "hall123", building.getId());
        assertEquals("Building name should be set correctly", "Hall Building", building.name);
        assertNotNull("Building polygon should not be null", building.polygon);
        assertEquals("Building polygon should have correct size", 2, building.polygon.size());
        assertEquals("currentlyInside should be false by default", false, building.currentlyInside);
    }

    /**
     * Test Building with empty polygon
     */
    @Test
    public void testBuilding_WithEmptyPolygon() {
        List<LatLng> emptyCoordinates = new ArrayList<>();
        Building building = new Building("empty_id", "Empty Building", emptyCoordinates);
        
        assertNotNull("Building should be created", building);
        assertEquals("Polygon should be empty", 0, building.polygon.size());
    }

    /**
     * Test Building with null values
     */
    @Test
    public void testBuilding_WithNullId() {
        List<LatLng> coordinates = new ArrayList<>();
        coordinates.add(new LatLng(45.4970, -73.5790));
        
        Building building = new Building(null, "Null ID Building", coordinates);
        
        assertNull("Building ID should be null", building.getId());
        assertEquals("Building name should still be set", "Null ID Building", building.name);
    }

    /**
     * Test Building currentlyInside field can be modified
     */
    @Test
    public void testBuilding_CurrentlyInsideField_CanBeModified() {
        List<LatLng> coordinates = new ArrayList<>();
        coordinates.add(new LatLng(45.4970, -73.5790));
        
        Building building = new Building("test_id", "Test Building", coordinates);
        
        assertEquals("currentlyInside should start as false", false, building.currentlyInside);
        
        building.currentlyInside = true;
        assertEquals("currentlyInside should be updated to true", true, building.currentlyInside);
        
        building.currentlyInside = false;
        assertEquals("currentlyInside should be updated back to false", false, building.currentlyInside);
    }

    //Test that buildingsMap uses the SAME key as the Building's ID.
    //This will prevents mismatches where clicking a building fetches info for another ID
    @Test
    public void testBuildingsMap_IdMappingIsConsistent() {
        MapsActivity.buildingsMap.clear();

        List<LatLng> coords = new ArrayList<>();
        coords.add(new LatLng(45.0, -73.0));

        // Simulate many buildings
        for (int i = 0; i < 50; i++) {
            String id = "way/" + i;
            Building building = new Building(id, "Building " + i, coords);
            MapsActivity.buildingsMap.put(id, building);
        }

        // Verify all are correctly retrievable
        for (int i = 0; i < 50; i++) {
            String id = "way/" + i;
            Building retrieved = MapsActivity.buildingsMap.get(id);

            assertNotNull("Building missing for id " + id, retrieved);
            assertEquals(id, retrieved.getId());
            assertEquals("Building " + i, retrieved.name);
        }
    }
}
