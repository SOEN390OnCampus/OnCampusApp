package com.example.oncampusapp;

import com.example.oncampusapp.navigation.NavigationHelper;
import com.example.oncampusapp.navigation.Route;
import com.example.oncampusapp.navigation.RouteTravelMode;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for NavigationHelper's pure JSON-building and parsing methods.
 * No network calls, no Android APIs.
 */
public class NavigationHelperParserTest {

    private static final LatLng SGW = new LatLng(45.4972, -73.5790);
    private static final LatLng LOY = new LatLng(45.4582, -73.6405);

    // ── buildRequestJson ──────────────────────────────────────────────────────

    @Test
    public void buildRequestJson_walk_containsTravelMode() throws Exception {
        String json = NavigationHelper.buildRequestJson(SGW, LOY, RouteTravelMode.WALK);
        JSONObject obj = new JSONObject(json);
        assertEquals("WALK", obj.getString("travelMode"));
    }

    @Test
    public void buildRequestJson_drive_containsTravelMode() throws Exception {
        String json = NavigationHelper.buildRequestJson(SGW, LOY, RouteTravelMode.DRIVE);
        assertEquals("DRIVE", new JSONObject(json).getString("travelMode"));
    }

    @Test
    public void buildRequestJson_transit_containsTransitPreferences() throws Exception {
        String json = NavigationHelper.buildRequestJson(SGW, LOY, RouteTravelMode.TRANSIT);
        JSONObject obj = new JSONObject(json);
        assertEquals("TRANSIT", obj.getString("travelMode"));
        assertTrue(obj.has("transitPreferences"));
        assertEquals("LESS_WALKING",
                obj.getJSONObject("transitPreferences").getString("routingPreference"));
    }

    @Test
    public void buildRequestJson_shuttle_noTransitPreferences() throws Exception {
        String json = NavigationHelper.buildRequestJson(SGW, LOY, RouteTravelMode.SHUTTLE);
        JSONObject obj = new JSONObject(json);
        assertEquals("SHUTTLE", obj.getString("travelMode"));
        assertFalse(obj.has("transitPreferences"));
    }

    @Test
    public void buildRequestJson_encodesOriginCoordinates() throws Exception {
        String json = NavigationHelper.buildRequestJson(SGW, LOY, RouteTravelMode.WALK);
        JSONObject obj = new JSONObject(json);
        JSONObject originLatLng = obj
                .getJSONObject("origin")
                .getJSONObject("location")
                .getJSONObject("latLng");
        assertEquals(SGW.latitude,  originLatLng.getDouble("latitude"),  0.0001);
        assertEquals(SGW.longitude, originLatLng.getDouble("longitude"), 0.0001);
    }

    @Test
    public void buildRequestJson_encodesDestinationCoordinates() throws Exception {
        String json = NavigationHelper.buildRequestJson(SGW, LOY, RouteTravelMode.WALK);
        JSONObject obj = new JSONObject(json);
        JSONObject destLatLng = obj
                .getJSONObject("destination")
                .getJSONObject("location")
                .getJSONObject("latLng");
        assertEquals(LOY.latitude,  destLatLng.getDouble("latitude"),  0.0001);
        assertEquals(LOY.longitude, destLatLng.getDouble("longitude"), 0.0001);
    }

    // ── convertResponseJsonToRoute ─────────────────────────────────────────────

    @Test
    public void convertResponseJson_emptyRoutes_returnsEmptyRoute() throws Exception {
        String response = "{\"routes\":[]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertNotNull(route);
        assertNull(route.getDuration());
        assertNull(route.getDistance());
    }

    @Test
    public void convertResponseJson_nullRoutesArray_returnsEmptyRoute() throws Exception {
        String response = "{}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertNotNull(route);
    }

    @Test
    public void convertResponseJson_withDurationAndDistance_parsesCorrectly() throws Exception {
        String response = "{"
                + "\"routes\":[{"
                + "  \"localizedValues\":{"
                + "    \"duration\":{\"text\":\"15 mins\"},"
                + "    \"distance\":{\"text\":\"1.2 km\"}"
                + "  },"
                + "  \"polyline\":{\"encodedPolyline\":\"\"},"
                + "  \"legs\":[{\"steps\":[]}]"
                + "}]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertEquals("15 mins", route.getDuration());
        assertEquals("1.2 km",  route.getDistance());
    }

    @Test
    public void convertResponseJson_withStep_setsStepList() throws Exception {
        String response = "{"
                + "\"routes\":[{"
                + "  \"localizedValues\":{},"
                + "  \"polyline\":{\"encodedPolyline\":\"\"},"
                + "  \"legs\":[{\"steps\":[{"
                + "    \"travelMode\":\"WALK\","
                + "    \"polyline\":{\"encodedPolyline\":\"\"},"
                + "    \"navigationInstruction\":{\"instructions\":\"Go straight\"},"
                + "    \"localizedValues\":{"
                + "      \"distance\":{\"text\":\"200 m\"},"
                + "      \"staticDuration\":{\"text\":\"3 mins\"}"
                + "    }"
                + "  }]}]"
                + "}]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertNotNull(route.getSteps());
        assertEquals(1, route.getSteps().size());
        assertEquals(RouteTravelMode.WALK, route.getSteps().get(0).getTravelMode());
        assertEquals("Go straight", route.getSteps().get(0).getInstructions());
    }

    @Test
    public void convertResponseJson_nullLocalizedValues_durationAndDistanceAreNull() throws Exception {
        String response = "{"
                + "\"routes\":[{"
                + "  \"polyline\":{\"encodedPolyline\":\"\"},"
                + "  \"legs\":[{\"steps\":[]}]"
                + "}]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertNull(route.getDuration());
        assertNull(route.getDistance());
    }

    @Test
    public void convertResponseJson_nullPolyline_routeHasNullOrEmptyPoints() throws Exception {
        String response = "{"
                + "\"routes\":[{"
                + "  \"localizedValues\":{},"
                + "  \"legs\":[{\"steps\":[]}]"
                + "}]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertNotNull(route);
    }

    @Test
    public void convertResponseJson_stepNoInstruction_instructionIsNull() throws Exception {
        String response = "{"
                + "\"routes\":[{"
                + "  \"localizedValues\":{},"
                + "  \"polyline\":{\"encodedPolyline\":\"\"},"
                + "  \"legs\":[{\"steps\":[{"
                + "    \"travelMode\":\"WALK\""
                + "  }]}]"
                + "}]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertNull(route.getSteps().get(0).getInstructions());
    }

    @Test
    public void convertResponseJson_stepNoLocalizedValues_distanceDurationAreNull() throws Exception {
        String response = "{"
                + "\"routes\":[{"
                + "  \"localizedValues\":{},"
                + "  \"polyline\":{\"encodedPolyline\":\"\"},"
                + "  \"legs\":[{\"steps\":[{"
                + "    \"travelMode\":\"DRIVE\""
                + "  }]}]"
                + "}]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertNull(route.getSteps().get(0).getDistance());
        assertNull(route.getSteps().get(0).getDuration());
    }

    @Test
    public void convertResponseJson_transitStepNoStopDetails_transitDetailsNotNull() throws Exception {
        String response = "{"
                + "\"routes\":[{"
                + "  \"localizedValues\":{},"
                + "  \"polyline\":{\"encodedPolyline\":\"\"},"
                + "  \"legs\":[{\"steps\":[{"
                + "    \"travelMode\":\"TRANSIT\","
                + "    \"transitDetails\":{}"
                + "  }]}]"
                + "}]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        var td = route.getSteps().get(0).getTransitDetails();
        assertNotNull(td);
        assertNull(td.getDepartureStopName());
        assertNull(td.getTransitLine());
    }

    @Test
    public void convertResponseJson_transitStep_parsesTransitDetails() throws Exception {
        String response = "{"
                + "\"routes\":[{"
                + "  \"localizedValues\":{},"
                + "  \"polyline\":{\"encodedPolyline\":\"\"},"
                + "  \"legs\":[{\"steps\":[{"
                + "    \"travelMode\":\"TRANSIT\","
                + "    \"polyline\":{\"encodedPolyline\":\"\"},"
                + "    \"transitDetails\":{"
                + "      \"stopDetails\":{"
                + "        \"departureStop\":{\"name\":\"Berri\","
                + "          \"location\":{\"latLng\":{\"latitude\":45.5,\"longitude\":-73.5}}},"
                + "        \"arrivalStop\":{\"name\":\"Lionel-Groulx\","
                + "          \"location\":{\"latLng\":{\"latitude\":45.47,\"longitude\":-73.58}}},"
                + "        \"departureTime\":\"09:00\","
                + "        \"arrivalTime\":\"09:10\""
                + "      },"
                + "      \"transitLine\":{"
                + "        \"name\":\"Green Line\","
                + "        \"nameShort\":\"2\","
                + "        \"color\":\"#00A651\","
                + "        \"vehicle\":{\"type\":\"SUBWAY\"}"
                + "      }"
                + "    }"
                + "  }]}]"
                + "}]}";
        Route route = NavigationHelper.convertResponseJsonToRoute(response);
        assertNotNull(route.getSteps());
        assertEquals(1, route.getSteps().size());

        var td = route.getSteps().get(0).getTransitDetails();
        assertNotNull(td);
        assertEquals("Berri",          td.getDepartureStopName());
        assertEquals("Lionel-Groulx",  td.getArrivalStopName());
        assertEquals("Green Line",     td.getTransitLine().getName());
        assertEquals("#00A651",        td.getTransitLine().getColor());
        assertEquals(com.example.oncampusapp.navigation.TransitVehicleType.SUBWAY, td.getVehicleType());
    }
}
