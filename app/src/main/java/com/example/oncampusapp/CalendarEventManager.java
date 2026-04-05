package com.example.oncampusapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CalendarEventManager {

    public static String globalEventsJson = "";
    public static String globalCalendarListJson = "";
    private static final String DATE_TIME_KEY = "dateTime";

    private CalendarEventManager() {
        // private implicit constructor for this utility class
    }

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
                long[] times = parseEventTimes(event, fmt);
                if (times.length == 0) continue;

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
            Log.e("CalendarEventManager", "Couldn't find next upcoming event");
        }

        return nextEvent;
    }

    // Returns {startTime, endTime} in millis, or empty array if missing start/end or is an all-day event
    private static long[] parseEventTimes(JSONObject event, SimpleDateFormat fmt) throws Exception {
        if (!event.has("start") || !event.has("end")) return new long[0];
        JSONObject startObj = event.getJSONObject("start");
        JSONObject endObj = event.getJSONObject("end");
        if (!startObj.has(DATE_TIME_KEY) || !endObj.has(DATE_TIME_KEY)) return new long[0];
        long startTime = fmt.parse(startObj.getString(DATE_TIME_KEY)).getTime();
        long endTime = fmt.parse(endObj.getString(DATE_TIME_KEY)).getTime();
        return new long[]{startTime, endTime};
    }
}