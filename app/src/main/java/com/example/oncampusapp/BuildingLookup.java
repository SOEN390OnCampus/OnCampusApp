package com.example.oncampusapp;

import com.google.android.gms.maps.model.LatLng;
import java.util.Map;

public class BuildingLookup {
    private BuildingLookup() {}

    // Helper to find a building's center by its name
    public static LatLng getLatLngFromBuildingName(String name, Map<String, Building> buildingsMap) {
        if (name == null || name.isEmpty() || buildingsMap == null) return null;

        for (Building b : buildingsMap.values()) {
            if (b == null || b.getName() == null) continue;

            if (b.getName().equalsIgnoreCase(name)) {
                return b.getCenter();
            }
        }
        return null;
    }

}
