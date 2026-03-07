package com.example.oncampusapp;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import static org.junit.Assert.*;

public class CalendarEventManagerTest {

    private final long ONE_HOUR = 60 * 60 * 1000;
    private final SimpleDateFormat exactTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

    // Helper method to generate dynamic Google Calendar JSON
    private String createMockJsonEvent(long startOffsetMillis, long endOffsetMillis, String summary) throws Exception {
        long now = System.currentTimeMillis();
        JSONObject event = new JSONObject();
        event.put("summary", summary);
        event.put("location", "H-110");

        JSONObject start = new JSONObject();
        start.put("dateTime", exactTimeFormat.format(new Date(now + startOffsetMillis)));
        event.put("start", start);

        JSONObject end = new JSONObject();
        end.put("dateTime", exactTimeFormat.format(new Date(now + endOffsetMillis)));
        event.put("end", end);

        JSONArray array = new JSONArray();
        array.put(event);
        return array.toString();
    }

    @Test
    public void testFindNextUpcomingEvent_WithEmptyJson_ReturnsNull() {
        JSONObject result = CalendarEventManager.findNextUpcomingEvent("");
        assertNull("Should return null for empty string", result);
    }

    @Test
    public void testFindNextUpcomingEvent_WithNullJson_ReturnsNull() {
        JSONObject result = CalendarEventManager.findNextUpcomingEvent(null);
        assertNull("Should return null for null input", result);
    }

    @Test
    public void testFindNextUpcomingEvent_WithFutureEvent_ReturnsEvent() throws Exception {
        // Event starts in 1 hour, ends in 2 hours
        String mockJson = createMockJsonEvent(ONE_HOUR, ONE_HOUR * 2, "Future Class");
        JSONObject result = CalendarEventManager.findNextUpcomingEvent(mockJson);

        assertNotNull("Should find the future event", result);
        assertEquals("Future Class", result.getString("summary"));
    }

    @Test
    public void testFindNextUpcomingEvent_WithOngoingEvent_ReturnsOngoingEvent() throws Exception {
        // Event started 10 mins ago, ends in 50 mins
        long tenMins = 10 * 60 * 1000;
        long fiftyMins = 50 * 60 * 1000;
        String mockJson = createMockJsonEvent(-tenMins, fiftyMins, "Ongoing Class");

        JSONObject result = CalendarEventManager.findNextUpcomingEvent(mockJson);

        assertNotNull("Should prioritize and return the ongoing event", result);
        assertEquals("Ongoing Class", result.getString("summary"));
    }

    @Test
    public void testFindNextUpcomingEvent_WithPastEventsOnly_ReturnsNull() throws Exception {
        // Event started 2 hours ago, ended 1 hour ago
        String mockJson = createMockJsonEvent(-ONE_HOUR * 2, -ONE_HOUR, "Past Class");
        JSONObject result = CalendarEventManager.findNextUpcomingEvent(mockJson);

        assertNull("Should ignore past events and return null", result);
    }

    @Test
    public void testFindNextUpcomingEvent_WithAllDayEvent_IgnoresEvent() throws Exception {
        JSONObject event = new JSONObject();
        event.put("summary", "Reading Week");

        JSONObject start = new JSONObject();
        start.put("date", "2026-10-25");
        event.put("start", start);

        JSONObject end = new JSONObject();
        end.put("date", "2026-10-30");
        event.put("end", end);

        JSONArray array = new JSONArray();
        array.put(event);

        JSONObject result = CalendarEventManager.findNextUpcomingEvent(array.toString());

        assertNull("Should ignore all-day events that lack a specific time", result);
    }
}