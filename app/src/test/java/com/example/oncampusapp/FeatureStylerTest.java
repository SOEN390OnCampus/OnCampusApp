package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class FeatureStylerTest {

    private final FeatureStyler styler = new FeatureStyler();

    @Test
    public void testGetStyle_Tunnel() {
        // Test logic for Tunnels ("route")
        FeatureStyler.StyleConfig style = styler.getStyle("route", false);
        assertTrue(style.isLineString());
        assertEquals(FeatureStyler.TUNNEL_COLOR, style.getStrokeColor());
        assertEquals(8f, style.getStrokeWidth(), 0.01);
    }

    @Test
    public void testGetStyle_ConcordiaBuilding() {
        // Test logic for Concordia Buildings
        FeatureStyler.StyleConfig style = styler.getStyle("building", true);
        assertFalse(style.isLineString());
        assertEquals(FeatureStyler.CONCORDIA_BUILDING_FILL_COLOR, style.getFillColor()); // Maroon
        assertEquals(FeatureStyler.CONCORDIA_BUILDING_STROKE_COLOR, style.getStrokeColor()); // Outline
        assertEquals(2f, style.getStrokeWidth(), 0.01);
    }

    @Test
    public void testGetStyle_IrrelevantBuilding() {
        // Test logic for non-Concordia buildings (should be invisible)
        FeatureStyler.StyleConfig style = styler.getStyle("building", false);
        assertFalse(style.isLineString());
        assertEquals(FeatureStyler.INVISIBLE_COLOR, style.getFillColor()); // Transparent
        assertEquals(0f, style.getStrokeWidth(), 0.01);
    }

    @Test
    public void testGetStyle_StandardBuilding_NullType() {
        // Most buildings in the GeoJSON (like Hall, EV) have NO "type" property.
        // This test confirms they still get processed as buildings, not tunnels.
        FeatureStyler.StyleConfig style = styler.getStyle(null, true);
        assertFalse(style.isLineString());
        assertEquals(FeatureStyler.CONCORDIA_BUILDING_FILL_COLOR, style.getFillColor()); // Maroon
    }
}