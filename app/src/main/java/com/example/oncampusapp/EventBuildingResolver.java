package com.example.oncampusapp;

import org.json.JSONObject;

import java.util.Map;

public class EventBuildingResolver {

    public static String extractBuildingNameFromEvent(
            JSONObject event,
            Map<String, BuildingDetails> buildingDetailsMap
    ) {
        if (event == null || buildingDetailsMap == null) {
            return null;
        }

        String rawLocation = event.optString("location", "").trim();
        if (rawLocation.isEmpty()) {
            return null;
        }

        String lower = rawLocation.toLowerCase();

        String exactMatch = findExactBuildingMatch(lower, buildingDetailsMap);
        if (exactMatch != null) {
            return exactMatch;
        }

        String fallbackMatch = findFallbackBuildingMatch(lower);
        if (fallbackMatch != null) {
            return fallbackMatch;
        }

        return rawLocation;
    }

    private static String findExactBuildingMatch(
            String lowerLocation,
            Map<String, BuildingDetails> buildingDetailsMap
    ) {
        for (BuildingDetails details : buildingDetailsMap.values()) {
            if (details == null || details.getName() == null) continue;

            String buildingName = details.getName().trim();
            if (!buildingName.isEmpty() && lowerLocation.contains(buildingName.toLowerCase())) {
                return buildingName;
            }
        }
        return null;
    }

    private static String findFallbackBuildingMatch(String lowerLocation) {
        if (matchesHall(lowerLocation)) return "Henry F. Hall Building";
        if (matchesMolson(lowerLocation)) return "John Molson School of Business";
        if (matchesEngineering(lowerLocation)) return "Engineering, Computer Science and Visual Arts Integrated Complex";
        if (matchesFaubourg(lowerLocation)) return "Le Faubourg";
        return null;
    }

    private static boolean matchesHall(String lowerLocation) {
        return lowerLocation.startsWith("h") || lowerLocation.contains("hall");
    }

    private static boolean matchesMolson(String lowerLocation) {
        return lowerLocation.startsWith("mb") || lowerLocation.contains("john molson");
    }

    private static boolean matchesEngineering(String lowerLocation) {
        return lowerLocation.startsWith("ev") || lowerLocation.contains("engineering");
    }

    private static boolean matchesFaubourg(String lowerLocation) {
        return lowerLocation.startsWith("fg")
                || lowerLocation.startsWith("fb")
                || lowerLocation.contains("faubourg");
    }
}