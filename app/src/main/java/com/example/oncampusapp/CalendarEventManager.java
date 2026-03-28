package com.example.oncampusapp;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CalendarEventManager {

    public static String globalEventsJson = "";

    public static synchronized void setGlobalEventsJson(String json) {
        globalEventsJson = json;
    }

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
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

        try {
            JSONArray eventsArray = new JSONArray(eventsJsonString);
            for (int i = 0; i < eventsArray.length(); i++) {
                JSONObject event = eventsArray.getJSONObject(i);
                if (!event.has("start") || !event.has("end")) continue;

                long[] times = parseEventTimes(event, fmt);
                if (times == null) continue;

                long startTime = times[0];
                long endTime = times[1];

                if (currentTime >= startTime && currentTime <= endTime) {
                    return event;
                }
                if (startTime > currentTime && startTime < closestFutureTime) {
                    closestFutureTime = startTime;
                    nextEvent = event;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nextEvent;
    }

    // Returns {startTime, endTime} in millis, or null if the event is an all-day event (no dateTime field)
    private static long[] parseEventTimes(JSONObject event, SimpleDateFormat fmt) throws Exception {
        JSONObject startObj = event.getJSONObject("start");
        JSONObject endObj = event.getJSONObject("end");
        if (!startObj.has("dateTime") || !endObj.has("dateTime")) return null;
        long startTime = fmt.parse(startObj.getString("dateTime")).getTime();
        long endTime = fmt.parse(endObj.getString("dateTime")).getTime();
        return new long[]{startTime, endTime};
    }
}