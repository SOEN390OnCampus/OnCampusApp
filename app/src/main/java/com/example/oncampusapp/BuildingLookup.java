package com.example.oncampusapp;

import com.google.android.gms.maps.model.LatLng;
import java.util.Map;

public class BuildingLookup {
    private BuildingLookup() {}

    // Helper to find a building's center by its name.
    // Tries exact (case-insensitive, trimmed) match first, then a startsWith fallback.
    // Buildings whose polygon center cannot be computed are skipped rather than returned as null.
    public static LatLng getLatLngFromBuildingName(String name, Map<String, Building> buildingsMap) {
        if (name == null || name.trim().isEmpty() || buildingsMap == null) return null;

        String query = name.trim();

        // Pass 1: exact case-insensitive match
        for (Building b : buildingsMap.values()) {
            if (b == null || b.getName() == null) continue;
            if (b.getName().trim().equalsIgnoreCase(query)) {
                LatLng center = b.getCenter();
                if (center != null) return center;
            }
        }

        // Pass 2: startsWith fallback (handles partial autocomplete text)
        for (Building b : buildingsMap.values()) {
            if (b == null || b.getName() == null) continue;
            if (b.getName().trim().toLowerCase().startsWith(query.toLowerCase())) {
                LatLng center = b.getCenter();
                if (center != null) return center;
            }
        }

        // Pass 3: contains fallback
        for (Building b : buildingsMap.values()) {
            if (b == null || b.getName() == null) continue;
            if (b.getName().trim().toLowerCase().contains(query.toLowerCase())) {
                LatLng center = b.getCenter();
                if (center != null) return center;
            }
        }

        return null;
    }

}
