package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class IndoorNodeTest {

    @Test
    public void testIndoorNodeConstructorAndGetters() {
        // Arrange & Act: Use the new constructor
        IndoorNode node = new IndoorNode("MB-S2.440", 1273.5f, 1371.0f);

        // Assert: Use getters
        assertEquals("MB-S2.440", node.getLabel());
        assertEquals(1273.5f, node.getX(), 0.001); // 0.001 is the delta for float comparison
        assertEquals(1371.0f, node.getY(), 0.001);
    }

    @Test
    public void testIndoorNodeSetters() {
        // Arrange: Use the empty constructor
        IndoorNode node = new IndoorNode();

        // Act: Use setters
        node.setLabel("H-807");
        node.setX(500.0f);
        node.setY(600.0f);

        // Assert: Use getters
        assertEquals("H-807", node.getLabel());
        assertEquals(500.0f, node.getX(), 0.001);
        assertEquals(600.0f, node.getY(), 0.001);
    }
}