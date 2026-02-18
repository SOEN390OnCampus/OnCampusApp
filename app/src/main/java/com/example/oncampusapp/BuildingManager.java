package com.example.oncampusapp;

import java.util.HashMap;
import java.util.Map;
public class BuildingManager {
    public static Map<String, Building> buildingsMap;

    public BuildingManager() {
        buildingsMap = new HashMap<>();
    }

    public void addBuilding(Building building1) {
        buildingsMap.put(building1.getId(),building1);
    }

    /**
     * get the current building the user is in
     * @return Building
     */
    public Building getCurrentBuilding() {
        for (Building building : buildingsMap.values()) {
            if (building.currentlyInside) {
                return building;
            }
        }
        return null;
    }
}
