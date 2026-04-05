package com.example.oncampusapp;

import java.util.HashMap;
import java.util.Map;
public class BuildingManager {
    private static final Map<String, Building> buildingsMap = new HashMap<>();

    public void addBuilding(Building building1) {
        buildingsMap.put(building1.getId(),building1);
    }

    /**
     * get the current building the user is in
     * @return Building
     */
    public Building getCurrentBuilding() {
        for (Building building : buildingsMap.values()) {
            if (building.isCurrentlyInside()) {
                return building;
            }
        }
        return null;
    }
    public static Map<String, Building> getBuildingsMap() {
        return buildingsMap;
    }
}
