package com.example.oncampusapp;

import android.content.Context;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationParser {

    private static final Map<String, String> ALL_BUILDINGS = new HashMap<>();
    private static final List<Map.Entry<String, String>> SORTED_BUILDINGS = new ArrayList<>();
    private static boolean isLoaded = false;

    private LocationParser() {
        // A private constructor to hide the implicit public one of this utility class
    }

    // Lazy load the JSON only once
    private static void loadBuildingsIfNeeded(Context context) {
        if (isLoaded) return;
        try (InputStream is = context.getResources().openRawResource(R.raw.concordia_building_details);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            // Point this to concordia_building_details.json file
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonBuilder.toString());
            Iterator<String> keys = jsonObject.keys();

            // Loop through the outer keys (like "B" or "way/103248055")
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject buildingData = jsonObject.getJSONObject(key);

                // Extract the actual code and name from inside the object
                if (buildingData.has("code") && buildingData.has("name")) {
                    String code = buildingData.getString("code");
                    String name = buildingData.getString("name");

                    // Put it in our map (e.g., "CL" -> "CL Annex")
                    ALL_BUILDINGS.put(code, name);
                }
            }

            // Populate and sort the list so longer prefixes are checked first
            SORTED_BUILDINGS.addAll(ALL_BUILDINGS.entrySet());
            SORTED_BUILDINGS.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

            isLoaded = true;

        } catch (Exception e) {
            android.util.Log.e("LocationParser", "Failed to load buildings JSON", e);
        }
    }

    // Added Context parameter to the signature
    public static String parseSmartLocation(Context context, String title, String rawLocation, String description) {
        // Ensure data is loaded from JSON first
        loadBuildingsIfNeeded(context);

        if (title == null) title = "";
        if (rawLocation == null) rawLocation = "";
        if (description == null) description = "";

        String searchText = (title + " " + rawLocation + " " + description).toUpperCase();
        String searchLower = searchText.toLowerCase();
        String displayLocation = "TBD";

        // Check for Online
        if (searchLower.contains("zoom.us") || searchLower.contains("teams.microsoft") ||
                searchLower.contains("meet.google") || searchLower.contains("online") || searchLower.contains("zoom")) {
            return "Online";
        }

        boolean found = false;
        for (Map.Entry<String, String> entry : SORTED_BUILDINGS) {
            String prefix = entry.getKey();

            Pattern pattern = Pattern.compile("\\b" + prefix + "[-\\s]?([A-Z]?\\d+[A-Z0-9.]*)\\b");
            Matcher matcher = pattern.matcher(searchText);

            if (matcher.find()) {
                displayLocation = entry.getValue();
                String roomNum = matcher.group(1);

                if (roomNum != null && !roomNum.isEmpty()) {
                    displayLocation = new StringBuilder(displayLocation).append(" - Room ").append(roomNum).toString();
                }
                found = true;
                break;
            } else if (searchText.matches(".*\\b" + prefix + "\\b.*")) {
                displayLocation = entry.getValue();
                found = true;
                break;
            }
        }

        if (!found && !rawLocation.isEmpty()) {
            displayLocation = rawLocation;
        }

        return displayLocation;
    }
}