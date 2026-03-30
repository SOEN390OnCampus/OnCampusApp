package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class NotificationRouteHelperTest {

    private Map<String, BuildingDetails> buildingDetailsMap;

    @Before
    public void setUp() {
        buildingDetailsMap = new HashMap<>();

        BuildingDetails hall = new BuildingDetails();
        hall.setName("Henry F. Hall Building");

        BuildingDetails jmsb = new BuildingDetails();
        jmsb.setName("John Molson School of Business");

        buildingDetailsMap.put("hall", hall);
        buildingDetailsMap.put("jmsb", jmsb);
    }

    private String toIso(long millis) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }

    private JSONObject createTimedEvent(String summary, String location, long startMillis, long endMillis) throws Exception {
        JSONObject event = new JSONObject();
        event.put("summary", summary);
        event.put("location", location);

        JSONObject start = new JSONObject();
        start.put("dateTime", toIso(startMillis));
        event.put("start", start);

        JSONObject end = new JSONObject();
        end.put("dateTime", toIso(endMillis));
        event.put("end", end);

        return event;
    }

    @Test
    public void resolveRoute_returnsNull_whenEventsJsonIsNull() {
        NotificationRouteHelper.Result result =
                NotificationRouteHelper.resolveRoute(null, null, buildingDetailsMap);

        assertNull(result);
    }

    @Test
    public void resolveRoute_returnsNull_whenDestinationCannotBeResolved() throws Exception {
        long now = System.currentTimeMillis();
        JSONObject nextClass = createTimedEvent("SOEN 341", "", now + 10000L, now + 20000L);

        NotificationRouteHelper.Result result =
                NotificationRouteHelper.resolveRoute("[]", nextClass, buildingDetailsMap);

        assertNull(result);
    }

    @Test
    public void resolveRoute_returnsDestinationOnly_whenNoPreviousClassExists() throws Exception {
        long now = System.currentTimeMillis();

        JSONObject nextClass = createTimedEvent(
                "SOEN 341",
                "Henry F. Hall Building",
                now + 1800000L,
                now + 3600000L
        );

        JSONArray events = new JSONArray();
        events.put(nextClass);

        NotificationRouteHelper.Result result =
                NotificationRouteHelper.resolveRoute(events.toString(), nextClass, buildingDetailsMap);

        assertNotNull(result);
        assertEquals("Henry F. Hall Building", result.getDestinationBuilding());
        assertNull(result.getPreviousBuilding());
        assertFalse(result.shouldStartFromPreviousBuilding());
    }

    @Test
    public void resolveRoute_usesPreviousBuilding_whenPreviousClassIsWithin15Minutes() throws Exception {
        long now = System.currentTimeMillis();

        JSONObject previousClass = createTimedEvent(
                "COMP 346",
                "John Molson School of Business",
                now - 3600000L,
                now
        );

        JSONObject nextClass = createTimedEvent(
                "SOEN 341",
                "Henry F. Hall Building",
                now + 10 * 60 * 1000L,
                now + 70 * 60 * 1000L
        );

        JSONArray events = new JSONArray();
        events.put(previousClass);
        events.put(nextClass);

        NotificationRouteHelper.Result result =
                NotificationRouteHelper.resolveRoute(events.toString(), nextClass, buildingDetailsMap);

        assertNotNull(result);
        assertEquals("Henry F. Hall Building", result.getDestinationBuilding());
        assertEquals("John Molson School of Business", result.getPreviousBuilding());
        assertTrue(result.shouldStartFromPreviousBuilding());
    }

    @Test
    public void resolveRoute_doesNotUsePreviousBuilding_whenPreviousClassIsTooFarApart() throws Exception {
        long now = System.currentTimeMillis();

        JSONObject previousClass = createTimedEvent(
                "COMP 346",
                "John Molson School of Business",
                now - 7200000L,
                now - 3600000L
        );

        JSONObject nextClass = createTimedEvent(
                "SOEN 341",
                "Henry F. Hall Building",
                now + 30 * 60 * 1000L,
                now + 90 * 60 * 1000L
        );

        JSONArray events = new JSONArray();
        events.put(previousClass);
        events.put(nextClass);

        NotificationRouteHelper.Result result =
                NotificationRouteHelper.resolveRoute(events.toString(), nextClass, buildingDetailsMap);

        assertNotNull(result);
        assertEquals("Henry F. Hall Building", result.getDestinationBuilding());
        assertNull(result.getPreviousBuilding());
        assertFalse(result.shouldStartFromPreviousBuilding());
    }

    @Test
    public void result_shouldStartFromPreviousBuilding_returnsFalse_whenPreviousBuildingIsEmpty() {
        NotificationRouteHelper.Result result =
                new NotificationRouteHelper.Result("Henry F. Hall Building", "");

        assertFalse(result.shouldStartFromPreviousBuilding());
    }

    @Test
    public void result_shouldStartFromPreviousBuilding_returnsTrue_whenPreviousBuildingExists() {
        NotificationRouteHelper.Result result =
                new NotificationRouteHelper.Result("Henry F. Hall Building", "John Molson School of Business");

        assertTrue(result.shouldStartFromPreviousBuilding());
    }
}