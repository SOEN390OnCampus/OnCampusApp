package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class IndoorNodeTest {

    @Test
    public void testIndoorNodeAssignment() {
        IndoorNode node = new IndoorNode();

        node.label = "MB-S2.440";
        node.x = 1273.5f;
        node.y = 1371.0f;

        assertEquals("MB-S2.440", node.label);
        assertEquals(1273.5f, node.x, 0.001); // 0.001 is the delta for float comparison
        assertEquals(1371.0f, node.y, 0.001);
    }
}