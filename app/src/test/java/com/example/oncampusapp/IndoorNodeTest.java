package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class IndoorNodeTest {

    @Test
    public void testLegacyConstructorAndGetters() {
        // Arrange & Act: Use the legacy constructor
        IndoorNode node = new IndoorNode("MB-S2.440", 1273.5f, 1371.0f);

        // Assert: Use getters
        assertEquals("MB-S2.440", node.getLabel());
        assertEquals(1273.5f, node.getX(), 0.001);
        assertEquals(1371.0f, node.getY(), 0.001);
    }

    @Test
    public void testFullConstructorAndGetters() {
        // Arrange & Act: Use the Builder pattern used by JSON loading
        IndoorNode node = new IndoorNode.Builder()
                .id("node_123").label("H-807").type("room")
                .buildingId("H").floor("8")
                .x(500.0f).y(600.0f).accessible(true)
                .build();

        // Assert: Check all fields
        assertEquals("node_123", node.getId());
        assertEquals("H-807", node.getLabel());
        assertEquals("room", node.getType());
        assertEquals("H", node.getBuildingId());
        assertEquals("8", node.getFloor());
        assertEquals(500.0f, node.getX(), 0.001);
        assertEquals(600.0f, node.getY(), 0.001);
        assertTrue(node.isAccessible());
    }

    @Test
    public void testIndoorNodeSetters() {
        // Arrange: Use the empty constructor
        IndoorNode node = new IndoorNode();

        // Act: Use all setters
        node.setId("test_id");
        node.setLabel("LB-207");
        node.setType("hallway");
        node.setBuildingId("LB");
        node.setFloor("2");
        node.setX(100.0f);
        node.setY(200.0f);
        node.setAccessible(false);

        // Assert: Verify all setters worked
        assertEquals("test_id", node.getId());
        assertEquals("LB-207", node.getLabel());
        assertEquals("hallway", node.getType());
        assertEquals("LB", node.getBuildingId());
        assertEquals("2", node.getFloor());
        assertEquals(100.0f, node.getX(), 0.001);
        assertEquals(200.0f, node.getY(), 0.001);
        assertFalse(node.isAccessible());
    }

    @Test
    public void testGetRootBuildingId() {
        // Standard case
        IndoorNode node1 = new IndoorNode();
        node1.setBuildingId("H");
        assertEquals("H", node1.getRootBuildingId());

        // Sub-building case (stripping the suffix)
        IndoorNode node2 = new IndoorNode();
        node2.setBuildingId("MB-S2");
        assertEquals("MB", node2.getRootBuildingId());

        // Null case
        IndoorNode node3 = new IndoorNode();
        assertEquals("", node3.getRootBuildingId());
    }

    @Test
    public void testGetFloorMenuId() {
        // Standard floor
        IndoorNode node1 = new IndoorNode();
        node1.setBuildingId("LB");
        node1.setFloor("8");
        assertEquals("8", node1.getFloorMenuId());

        // MB-S2 special edge case
        IndoorNode node2 = new IndoorNode();
        node2.setBuildingId("MB-S2");
        node2.setFloor("-2"); // Even if floor is set to -2, it should return S2
        assertEquals("S2", node2.getFloorMenuId());

        // Null floor case fallback
        IndoorNode node3 = new IndoorNode();
        node3.setBuildingId("H");
        assertEquals("", node3.getFloorMenuId());
    }
}