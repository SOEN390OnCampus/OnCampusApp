package com.example.oncampusapp;

import com.google.android.gms.maps.model.LatLng;
import java.util.Map;

public class BuildingLookup {
    private BuildingLookup() {
    }

    // Helper to find a building's center by its name.
    // Tries exact (case-insensitive, trimmed) match first, then a startsWith fallback.
    // Buildings whose polygon center cannot be computed are skipped rather than returned as null.
    public static LatLng getLatLngFromBuildingName(String name, Map<String, Building> buildingsMap) {
        if (name == null || name.trim().isEmpty() || buildingsMap == null) return null;

        String query = name.trim().toLowerCase();

        // Try matching strategies in order
        LatLng result;

        result = find(buildingsMap, b -> equalsIgnoreCaseTrimmed(b, query));
        if (result != null) return result;

        result = find(buildingsMap, b -> startsWith(b, query));
        if (result != null) return result;

        return find(buildingsMap, b -> contains(b, query));
    }

    private static LatLng find(Map<String, Building> buildingsMap, java.util.function.Predicate<Building> matcher) {
        for (Building b : buildingsMap.values()) {
            if (b == null || b.getName() == null) continue;

            if (matcher.test(b)) {
                LatLng center = b.getCenter();
                if (center != null) return center;
            }
        }
        return null;
    }

    private static boolean equalsIgnoreCaseTrimmed(Building b, String query) {
        return b.getName().trim().equalsIgnoreCase(query);
    }

    private static boolean startsWith(Building b, String query) {
        return b.getName().trim().toLowerCase().startsWith(query);
    }

    private static boolean contains(Building b, String query) {
        return b.getName().trim().toLowerCase().contains(query);
    }
}