package com.example.oncampusapp;

public class FeatureStyler {

    // Color constants for better readability
    public static final int TUNNEL_COLOR = 0x7F000000; // 50% transparent black
    public static final int CONCORDIA_BUILDING_FILL_COLOR = 0xFF912338; // Opaque Maroon
    public static final int CONCORDIA_BUILDING_STROKE_COLOR = 0xFF5E1624; // Darker Maroon
    public static final int INVISIBLE_COLOR = 0x00000000; // Fully transparent

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
            // Style for tunnels
            return new StyleConfig(INVISIBLE_COLOR, TUNNEL_COLOR, 8f, true);
        } else if (isConcordiaBuilding) {
            // Style for Concordia buildings
            return new StyleConfig(CONCORDIA_BUILDING_FILL_COLOR, CONCORDIA_BUILDING_STROKE_COLOR, 2f, false);
        } else {
            // Style for irrelevant features (invisible)
            return new StyleConfig(INVISIBLE_COLOR, INVISIBLE_COLOR, 0f, false);
        }
    }
}
