package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class FeatureStylerTest {

    private final FeatureStyler styler = new FeatureStyler();

    @Test
    public void testGetStyle_Tunnel() {
        // Test logic for Tunnels ("route")
        FeatureStyler.StyleConfig style = styler.getStyle("route", false);
        assertTrue(style.isLineString);
        assertEquals(0x7F000000, style.strokeColor);
        assertEquals(8f, style.strokeWidth, 0.01);
    }

    @Test
    public void testGetStyle_ConcordiaBuilding() {
        // Test logic for Concordia Buildings
        FeatureStyler.StyleConfig style = styler.getStyle("building", true);
        assertFalse(style.isLineString);
        assertEquals(0xFF912338, style.fillColor); // Maroon
        assertEquals(0xFF5E1624, style.strokeColor); // Outline
        assertEquals(2f, style.strokeWidth, 0.01);
    }

    @Test
    public void testGetStyle_IrrelevantBuilding() {
        // Test logic for non-Concordia buildings (should be invisible)
        FeatureStyler.StyleConfig style = styler.getStyle("building", false);
        assertFalse(style.isLineString);
        assertEquals(0x00000000, style.fillColor); // Transparent
        assertEquals(0f, style.strokeWidth, 0.01);
    }

    @Test
    public void testGetStyle_StandardBuilding_NullType() {
        // Most buildings in the GeoJSON (like Hall, EV) have NO "type" property.
        // This test confirms they still get processed as buildings, not tunnels.
        FeatureStyler.StyleConfig style = styler.getStyle(null, true);
        assertFalse(style.isLineString);
        assertEquals(0xFF912338, style.fillColor); // Maroon
    }
}