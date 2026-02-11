package com.example.oncampusapp;

public class FeatureStyler {

    public static class StyleConfig {
        public int fillColor;
        public int strokeColor;
        public float strokeWidth;
        public boolean isLineString; // true for tunnels, false for polygons

        public StyleConfig(int fillColor, int strokeColor, float strokeWidth, boolean isLineString) {
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
            this.strokeWidth = strokeWidth;
            this.isLineString = isLineString;
        }
    }

    /**
     * Determines the visual style for a map feature.
     * @param type The "type" property from GeoJSON (e.g., "route")
     * @param isConcordiaBuilding True if the classifier identified it as a Concordia building
     * @return The style configuration
     */
    public StyleConfig getStyle(String type, boolean isConcordiaBuilding) {
        if ("route".equals(type)) {
            // Tunnel - 50% transparent black
            return new StyleConfig(0, 0x7F000000, 8f, true);
        } else if (isConcordiaBuilding) {
            // Concordia Building - Opaque Maroon with Darker Outline
            return new StyleConfig(0xFF912338, 0xFF5E1624, 2f, false);
        } else {
            // Irrelevant - Invisible
            return new StyleConfig(0x00000000, 0x00000000, 0f, false);
        }
    }
}