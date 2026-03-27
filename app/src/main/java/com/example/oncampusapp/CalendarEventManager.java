package com.example.oncampusapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CalendarEventManager {

    public static String globalEventsJson = "";

    /**
     * Parses the JSON array of events and returns the current OR next upcoming event.
     * Priority 1: An event happening right now.
     * Priority 2: The closest event in the future.
     */
    public static JSONObject findNextUpcomingEvent(String eventsJsonString) {
        if (eventsJsonString == null || eventsJsonString.isEmpty()) return null;

        JSONObject nextEvent = null;
        long closestFutureTime = Long.MAX_VALUE;
        long currentTime = System.currentTimeMillis();

        SimpleDateFormat exactTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

        try {
            JSONArray eventsArray = new JSONArray(eventsJsonString);

            for (int i = 0; i < eventsArray.length(); i++) {
                JSONObject event = eventsArray.getJSONObject(i);

                if (!event.has("start") || !event.has("end")) continue;

                JSONObject startObj = event.getJSONObject("start");
                JSONObject endObj = event.getJSONObject("end");

                // We only want events with specific times (ignoring all-day events)
                if (startObj.has("dateTime") && endObj.has("dateTime")) {

                    long startTime = exactTimeFormat.parse(startObj.getString("dateTime")).getTime();
                    long endTime = exactTimeFormat.parse(endObj.getString("dateTime")).getTime();

                    // --- NEW LOGIC: Check if class is currently IN PROGRESS ---
                    if (currentTime >= startTime && currentTime <= endTime) {
                        return event; // Exit immediately and return the active class
                    }

                    // --- EXISTING LOGIC: Find the closest FUTURE class ---
                    if (startTime > currentTime && startTime < closestFutureTime) {
                        closestFutureTime = startTime;
                        nextEvent = event;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CalendarEventManager", "Couldn't find next upcoming event");
        }

        return nextEvent;
    }
}