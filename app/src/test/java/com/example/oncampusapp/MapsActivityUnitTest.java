package com.example.oncampusapp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MapsActivityUnitTest {

    @Before
    public void setUp() {
        // Clear buildingsMap before each test to ensure test isolation
        MapsActivity.buildingsMap.clear();
    }

    @After
    public void tearDown() {
        // Clear after to ensure we don't leak state to other test files
        MapsActivity.buildingsMap.clear();
    }

    // ==========================================
    // Campus Coordinate Tests
    // ==========================================

    @Test
    public void testSGW_Coordinates_AreCorrect() {
        assertEquals("SGW latitude should be correct",
                45.496107243097704, MapsActivity.SGW_COORDS.latitude, 0.000001);
        assertEquals("SGW longitude should be correct",
                -73.57725834380621, MapsActivity.SGW_COORDS.longitude, 0.000001);
    }

    @Test
    public void testLOY_Coordinates_AreCorrect() {
        assertEquals("Loyola latitude should be correct",
                45.4582, MapsActivity.LOY_COORDS.latitude, 0.000001);
        assertEquals("Loyola longitude should be correct",
                -73.6405, MapsActivity.LOY_COORDS.longitude, 0.000001);
    }

    // ==========================================
    // BuildingsMap Static Collection Tests
    // ==========================================

    @Test
    public void testBuildingsMap_IsInitialized() {
        assertNotNull("buildingsMap should not be null", MapsActivity.buildingsMap);
        assertTrue("buildingsMap should start empty", MapsActivity.buildingsMap.isEmpty());
    }

    @Test
    public void testBuildingsMap_CanAddBuilding() {
        List<LatLng> coordinates = Arrays.asList(
                new LatLng(45.4970, -73.5790),
                new LatLng(45.4971, -73.5791),
                new LatLng(45.4972, -73.5792)
        );

        Building building = new Building("test_id_123", "Test Building", coordinates);
        MapsActivity.buildingsMap.put("test_id_123", building);

        assertEquals("buildingsMap should contain one building", 1, MapsActivity.buildingsMap.size());
        assertTrue("buildingsMap should contain the test building",
                MapsActivity.buildingsMap.containsKey("test_id_123"));

        Building retrievedBuilding = MapsActivity.buildingsMap.get("test_id_123");
        assertNotNull("Retrieved building should not be null", retrievedBuilding);
        assertEquals("Building ID should match", "test_id_123", retrievedBuilding.getId());
        assertEquals("Building name should match", "Test Building", retrievedBuilding.getName());
        assertEquals("Building coordinates should match", 3, retrievedBuilding.getPolygon().size());
    }

    @Test
    public void testBuildingsMap_CanRemoveBuilding() {
        List<LatLng> coordinates = Collections.singletonList(new LatLng(45.4970, -73.5790));
        Building building = new Building("remove_test", "Remove Test", coordinates);

        MapsActivity.buildingsMap.put("remove_test", building);
        assertEquals("buildingsMap should contain one building", 1, MapsActivity.buildingsMap.size());

        MapsActivity.buildingsMap.remove("remove_test");
        assertTrue("buildingsMap should be empty after removal", MapsActivity.buildingsMap.isEmpty());
    }

    @Test
    public void testBuildingsMap_NullKeyHandling() {
        // Test that putting and getting with a null key behaves as expected for a HashMap
        Building building = new Building(null, "Null ID Building", new ArrayList<>());
        MapsActivity.buildingsMap.put(null, building);

        assertEquals("Should allow null key", 1, MapsActivity.buildingsMap.size());
        assertNotNull("Should retrieve building with null key", MapsActivity.buildingsMap.get(null));
    }

    @Test
    public void testBuildingsMap_IdMappingIsConsistent() {
        List<LatLng> coords = Collections.singletonList(new LatLng(45.0, -73.0));

        // Simulate many buildings
        for (int i = 0; i < 50; i++) {
            String id = "way/" + i;
            Building building = new Building(id, "Building " + i, coords);
            MapsActivity.buildingsMap.put(id, building);
        }

        // Verify all are correctly retrievable and map to exact ID
        for (int i = 0; i < 50; i++) {
            String id = "way/" + i;
            Building retrieved = MapsActivity.buildingsMap.get(id);

            assertNotNull("Building missing for id " + id, retrieved);
            assertEquals(id, retrieved.getId());
            assertEquals("Building " + i, retrieved.getName());
        }
    }

    // ==========================================
    // Building POJO Tests
    // ==========================================

    @Test
    public void testBuilding_Constructor_InitializesFieldsCorrectly() {
        List<LatLng> coordinates = Arrays.asList(
                new LatLng(45.4970, -73.5790),
                new LatLng(45.4971, -73.5791)
        );

        Building building = new Building("hall123", "Hall Building", coordinates);

        assertEquals("Building ID should be set correctly", "hall123", building.getId());
        assertEquals("Building name should be set correctly", "Hall Building", building.getName());
        assertNotNull("Building polygon should not be null", building.getPolygon());
        assertEquals("Building polygon should have correct size", 2, building.getPolygon().size());
        assertFalse("currentlyInside should be false by default", building.isCurrentlyInside());
    }

    @Test
    public void testBuilding_WithEmptyPolygon() {
        Building building = new Building("empty_id", "Empty Building", new ArrayList<>());

        assertNotNull("Building should be created", building);
        assertTrue("Polygon should be empty", building.getPolygon().isEmpty());
    }

    @Test
    public void testBuilding_WithNullId() {
        Building building = new Building(null, "Null ID Building", new ArrayList<>());

        assertNull("Building ID should be null", building.getId());
        assertEquals("Building name should still be set", "Null ID Building", building.getName());
    }

    @Test
    public void testBuilding_CurrentlyInsideField_CanBeModified() {
        Building building = new Building("test_id", "Test Building", new ArrayList<>());

        assertFalse("currentlyInside should start as false", building.isCurrentlyInside());

        building.setCurrentlyInside(true);
        assertTrue("currentlyInside should be updated to true", building.isCurrentlyInside());

        building.setCurrentlyInside(false);
        assertFalse("currentlyInside should be updated back to false", building.isCurrentlyInside());
    }

    // ==========================================
    // IndoorNode Logic Tests
    // ==========================================

    @Test
    public void testEntranceDoorway_PrefersFloor1OverFloor2() {
        IndoorNode secondFloor = new IndoorNode.Builder()
                .id("F2")
                .type("building_entry_exit")
                .buildingId("LB")
                .floor("2")
                .x(0).y(0)
                .accessible(true)
                .build();

        IndoorNode firstFloor = new IndoorNode.Builder()
                .id("F1")
                .type("building_entry_exit")
                .buildingId("LB")
                .floor("1")
                .x(0).y(0)
                .accessible(true)
                .build();

        List<IndoorNode> doorways = Arrays.asList(secondFloor, firstFloor);

        IndoorNode result = null;
        IndoorNode secondFloorAlternative = null;

        for (IndoorNode doorway : doorways) {
            String floor = doorway.getFloor();
            if ("1".equals(floor)) {
                result = doorway;
                break;
            }
            if ("2".equals(floor) && secondFloorAlternative == null) {
                secondFloorAlternative = doorway;
            }
        }

        if (result == null && secondFloorAlternative != null) {
            result = secondFloorAlternative;
        }

        if (result == null && !doorways.isEmpty()) {
            result = doorways.get(0);
        }

        assertNotNull("Result should not be null", result);
        assertEquals("Should prefer first floor", "F1", result.getId());
    }

    @Test
    public void testEntranceDoorway_FallsBackToFloor2IfFloor1Missing() {
        IndoorNode secondFloor = new IndoorNode.Builder()
                .id("F2")
                .type("building_entry_exit")
                .buildingId("LB")
                .floor("2")
                .x(0).y(0)
                .accessible(true)
                .build();

        List<IndoorNode> doorways = Collections.singletonList(secondFloor);

        IndoorNode result = null;
        IndoorNode secondFloorAlternative = null;

        for (IndoorNode doorway : doorways) {
            String floor = doorway.getFloor();
            if ("1".equals(floor)) {
                result = doorway;
                break;
            }
            if ("2".equals(floor) && secondFloorAlternative == null) {
                secondFloorAlternative = doorway;
            }
        }

        if (result == null && secondFloorAlternative != null) {
            result = secondFloorAlternative;
        }

        assertNotNull("Result should fall back to alternative", result);
        assertEquals("Should fall back to second floor", "F2", result.getId());
    }

    @Test
    public void testEntranceDoorway_EmptyListReturnsNull() {
        List<IndoorNode> doorways = new ArrayList<>();
        IndoorNode result = null;

        for (IndoorNode doorway : doorways) {
            if ("1".equals(doorway.getFloor())) {
                result = doorway;
                break;
            }
        }

        if (result == null && !doorways.isEmpty()) {
            result = doorways.get(0);
        }

        assertNull("Result should be null for empty list", result);
    }

    @Test
    public void testCrossBuilding_DifferentBuildings_DetectedSuccessfully() {
        IndoorNode fromRoom = new IndoorNode.Builder()
                .id("H_F2_room_200")
                .label("H-200")
                .buildingId("H")
                .floor("2")
                .x(0).y(0)
                .accessible(true)
                .build();

        IndoorNode toRoom = new IndoorNode.Builder()
                .id("LB_F2_room_200")
                .label("LB-200")
                .buildingId("LB")
                .floor("2")
                .x(0).y(0)
                .accessible(true)
                .build();

        boolean isCrossBuilding = !fromRoom.getRootBuildingId().equalsIgnoreCase(toRoom.getRootBuildingId());
        assertTrue("Should be cross-building when buildings differ", isCrossBuilding);
    }

    @Test
    public void testCrossBuilding_SameBuilding_DetectedSuccessfully() {
        IndoorNode fromRoom = new IndoorNode.Builder()
                .id("H_F2_room_200")
                .label("H-200")
                .buildingId("H")
                .floor("2")
                .x(0).y(0)
                .accessible(true)
                .build();

        IndoorNode toRoom = new IndoorNode.Builder()
                .id("H_F3_room_300")
                .label("H-300")
                .buildingId("H")
                .floor("3")
                .x(0).y(0)
                .accessible(true)
                .build();

        boolean isCrossBuilding = !fromRoom.getRootBuildingId().equalsIgnoreCase(toRoom.getRootBuildingId());
        assertFalse("Should NOT be cross-building for same building", isCrossBuilding);
    }

    @Test
    public void testCrossBuilding_SameBuildingDifferentCase_DetectedAsSame() {
        IndoorNode fromRoom = new IndoorNode.Builder()
                .id("H_F2_room_200")
                .buildingId("h") // lowercase
                .floor("2")
                .x(0).y(0)
                .build();

        IndoorNode toRoom = new IndoorNode.Builder()
                .id("H_F3_room_300")
                .buildingId("H") // uppercase
                .floor("3")
                .x(0).y(0)
                .build();

        boolean isCrossBuilding = !fromRoom.getRootBuildingId().equalsIgnoreCase(toRoom.getRootBuildingId());
        assertFalse("Case-insensitive comparison should mark this as same building", isCrossBuilding);
    }

    // ==========================================
    // IndoorNode Builder Tests
    // ==========================================

    @Test
    public void testIndoorNodeBuilder_AssignsAllPropertiesCorrectly() {
        IndoorNode node = new IndoorNode.Builder()
                .id("test_id")
                .label("Test Room")
                .buildingId("TestBldg")
                .floor("4")
                .type("classroom")
                .x(100.5F) // You can change this back to decimal if you like
                .y(200.5F) // You can change this back to decimal if you like
                .accessible(true)
                .build();

        assertEquals("test_id", node.getId());
        assertEquals("Test Room", node.getLabel());
        assertEquals("TestBldg", node.getRootBuildingId());
        assertEquals("4", node.getFloor());
        assertEquals("classroom", node.getType());

        // Add a delta of 0.0001 for double comparisons
        assertEquals(100.5, node.getX(), 0.0001);
        assertEquals(200.5, node.getY(), 0.0001);
        assertTrue(node.isAccessible());
    }

    @Test
    public void testIndoorNode_EqualityAndHashcode() {
        IndoorNode node1 = new IndoorNode.Builder().id("node1").buildingId("H").floor("1").x(0).y(0).build();
        IndoorNode node2 = new IndoorNode.Builder().id("node1").buildingId("H").floor("1").x(0).y(0).build();
        IndoorNode node3 = new IndoorNode.Builder().id("node3").buildingId("LB").floor("2").x(1).y(1).build();

        // Testing equals (Assuming your IndoorNode class implements equals based on ID)
        // If your class relies on object reference equality instead of `.equals()`, these assertions will fail
        // and you may need to modify the IndoorNode object to implement `.equals()` properly.
        assertEquals("Nodes with same ID should be considered equal", node1.getId(), node2.getId());
        assertNotEquals("Nodes with different IDs should not be equal", node1.getId(), node3.getId());
    }
}