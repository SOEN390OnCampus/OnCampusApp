package com.example.oncampusapp;

public class BuildingClassifier {

    /**
     * Determines if a GeoJSON feature should be treated as a Concordia University building.
     *
     * @param building The "building" property of the feature.
     * @param name The "name" property of the feature.
     * @param operator The "operator" property of the feature.
     * @return True if the feature is a Concordia building, false otherwise.
     */
    public boolean isConcordiaBuilding(String building, String name, String operator) {
        return "university".equals(building) ||
                (name != null && name.contains("Concordia")) ||
                "Concordia University".equals(operator) ||
                "Stinger Dome (SD)".equals(name);
    }
}
