package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PreviousClassFinderTest {

    private String toIso(long millis) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }

    private JSONObject createTimedEvent(String summary, long startMillis, long endMillis) throws Exception {
        JSONObject event = new JSONObject();
        event.put("summary", summary);

        JSONObject start = new JSONObject();
        start.put("dateTime", toIso(startMillis));
        event.put("start", start);

        JSONObject end = new JSONObject();
        end.put("dateTime", toIso(endMillis));
        event.put("end", end);

        return event;
    }

    @Test
    public void findPreviousClass_returnsNull_whenEventsJsonIsInvalid() throws Exception {
        JSONObject target = createTimedEvent("Next Class", 10000L, 20000L);

        JSONObject result = PreviousClassFinder.findPreviousClass("not json", target);

        assertNull(result);
    }

    @Test
    public void findPreviousClass_returnsClosestPreviousEvent() throws Exception {
        long now = System.currentTimeMillis();

        JSONObject olderClass = createTimedEvent("Older Class", now - 7200000L, now - 5400000L);
        JSONObject closestPrevious = createTimedEvent("Closest Previous", now - 3600000L, now - 1800000L);
        JSONObject targetClass = createTimedEvent("Target Class", now + 1800000L, now + 3600000L);

        JSONArray events = new JSONArray();
        events.put(olderClass);
        events.put(targetClass);
        events.put(closestPrevious);

        JSONObject result = PreviousClassFinder.findPreviousClass(events.toString(), targetClass);

        assertNotNull(result);
        assertEquals("Closest Previous", result.getString("summary"));
    }

    @Test
    public void findPreviousClass_returnsNull_whenNoPreviousClassExists() throws Exception {
        long now = System.currentTimeMillis();

        JSONObject targetClass = createTimedEvent("Target Class", now + 1800000L, now + 3600000L);
        JSONObject futureClass = createTimedEvent("Future Class", now + 7200000L, now + 10800000L);

        JSONArray events = new JSONArray();
        events.put(targetClass);
        events.put(futureClass);

        JSONObject result = PreviousClassFinder.findPreviousClass(events.toString(), targetClass);

        assertNull(result);
    }

    @Test
    public void findPreviousClass_ignoresAllDayEventsWithoutDateTime() throws Exception {
        long now = System.currentTimeMillis();

        JSONObject targetClass = createTimedEvent("Target Class", now + 1800000L, now + 3600000L);

        JSONObject allDayEvent = new JSONObject();
        allDayEvent.put("summary", "All Day Event");
        JSONObject start = new JSONObject();
        start.put("date", "2026-03-25");
        allDayEvent.put("start", start);
        JSONObject end = new JSONObject();
        end.put("date", "2026-03-26");
        allDayEvent.put("end", end);

        JSONArray events = new JSONArray();
        events.put(allDayEvent);
        events.put(targetClass);

        JSONObject result = PreviousClassFinder.findPreviousClass(events.toString(), targetClass);

        assertNull(result);
    }
}