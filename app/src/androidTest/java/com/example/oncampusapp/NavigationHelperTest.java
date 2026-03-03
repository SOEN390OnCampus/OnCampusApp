package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.oncampusapp.navigation.NavigationHelper;
import com.example.oncampusapp.navigation.Route;
import com.example.oncampusapp.navigation.RouteTravelMode;
import com.example.oncampusapp.navigation.Step;
import com.example.oncampusapp.navigation.TransitDetails;
import com.example.oncampusapp.navigation.TransitVehicleType;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class NavigationHelperTest {
    private static String MOCK_RESPONSE;
    private static final LatLng start = new LatLng(45.496, -73.577);
    private static final LatLng end = new LatLng(45.457, -73.641);


    @BeforeClass
    public static void setup() throws IOException {
        MOCK_RESPONSE = loadMockResponse();
    }

    @Test
    public void testResponseConvert_RouteDistanceAndDuration() throws JSONException {
        Route route = NavigationHelper.convertResponseJsonToRoute(MOCK_RESPONSE);
        assertEquals("8.0 km", route.getDistance());
        assertEquals("37 mins", route.getDuration());
    }

    @Test
    public void testResponseConvert_RoutePointsNotNull() throws JSONException {
        Route route = NavigationHelper.convertResponseJsonToRoute(MOCK_RESPONSE);
        assertNotNull(route.getPoints());
        assertFalse(route.getPoints().isEmpty());
    }

    @Test
    public void testResponseConvert_StepCount() throws JSONException {
        Route route = NavigationHelper.convertResponseJsonToRoute(MOCK_RESPONSE);
        assertEquals(3, route.getSteps().size());
    }

    @Test
    public void testResponseConvert_WalkStep() throws JSONException {
        Route route = NavigationHelper.convertResponseJsonToRoute(MOCK_RESPONSE);
        Step firstStep = route.getSteps().get(0);
        assertEquals(RouteTravelMode.WALK, firstStep.getTravelMode());
        assertNotNull(firstStep.getPoints());
        assertNull(firstStep.getTransitDetails());
    }

    @Test
    public void testResponseConvert_TransitStep() throws JSONException {
        Route route = NavigationHelper.convertResponseJsonToRoute(MOCK_RESPONSE);
        Step transitStep = route.getSteps().get(1);

        assertEquals(RouteTravelMode.TRANSIT, transitStep.getTravelMode());
        assertNotNull(transitStep.getTransitDetails());

        TransitDetails details = transitStep.getTransitDetails();
        assertEquals("Sherbrooke / Redpath", details.getDepartureStopName());
        assertEquals("Sherbrooke / West Broadway", details.getArrivalStop());
        assertEquals("2026-03-01T08:13:25Z", details.getDepartureTime());
        assertEquals("2026-03-01T08:42:39Z", details.getArrivalTime());
        assertEquals(TransitVehicleType.BUS, details.getVehicleType());
    }

    //===========================================Build Request Json===================================================

    @Test
    public void testBuildRequestJson_walk() throws JSONException {
        String json = NavigationHelper.buildRequestJson(start, end, RouteTravelMode.WALK);
        JSONObject obj = new JSONObject(json);

        assertEquals("WALK", obj.getString("travelMode"));
        assertFalse(obj.has("transitPreferences"));
        assertEquals(start.latitude, obj.getJSONObject("origin")
                .getJSONObject("location")
                .getJSONObject("latLng")
                .getDouble("latitude"), 0.001);
        assertEquals(start.longitude, obj.getJSONObject("origin")
                .getJSONObject("location")
                .getJSONObject("latLng")
                .getDouble("longitude"), 0.001);
    }

    @Test
    public void testBuildRequestJson_transit() throws JSONException {
        String json = NavigationHelper.buildRequestJson(start, end, RouteTravelMode.TRANSIT);
        JSONObject obj = new JSONObject(json);

        assertEquals("TRANSIT", obj.getString("travelMode"));
        assertTrue(obj.has("transitPreferences"));
        assertEquals("LESS_WALKING", obj.getJSONObject("transitPreferences")
                .getString("routingPreference"));
    }

    @Test
    public void testBuildRequestJson_drive() throws JSONException {

        String json = NavigationHelper.buildRequestJson(start, end, RouteTravelMode.DRIVE);
        JSONObject obj = new JSONObject(json);

        assertEquals("DRIVE", obj.getString("travelMode"));
        assertFalse(obj.has("transitPreferences"));
    }

    @Test
    public void testBuildRequestJson_correctCoordinates() throws JSONException {
        String json = NavigationHelper.buildRequestJson(start, end, RouteTravelMode.WALK);
        JSONObject obj = new JSONObject(json);

        JSONObject originLatLng = obj.getJSONObject("origin")
                .getJSONObject("location")
                .getJSONObject("latLng");
        JSONObject destLatLng = obj.getJSONObject("destination")
                .getJSONObject("location")
                .getJSONObject("latLng");

        assertEquals(start.latitude, originLatLng.getDouble("latitude"), 0.001);
        assertEquals(start.longitude, originLatLng.getDouble("longitude"), 0.001);
        assertEquals(end.latitude, destLatLng.getDouble("latitude"), 0.001);
        assertEquals(end.longitude, destLatLng.getDouble("longitude"), 0.001);
    }
    // ===================================== fetchRoute ============================================

    @Test
    public void testFetchRoute_success() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final Route[] result = {null};
        final Exception[] error = {null};

        NavigationHelper.fetchRoute(
                start,
                end,
                RouteTravelMode.TRANSIT,
                BuildConfig.MAPS_API_KEY,
                new NavigationHelper.RoutesCallback() {
                    @Override
                    public void onSuccess(Route route) {
                        result[0] = route;
                        latch.countDown();
                    }
                    @Override
                    public void onError(Exception e) {
                        error[0] = e;
                        latch.countDown();
                    }
                }
        );

        latch.await(15, TimeUnit.SECONDS);
        assertNull("Expected no error but got: " + (error[0] != null ? error[0].getMessage() : ""), error[0]);
        assertNotNull("Route should not be null", result[0]);
        assertNotNull("Points should not be null", result[0].getPoints());
        assertFalse("Points should not be empty", result[0].getPoints().isEmpty());
        assertNotNull(result[0].getDuration());
        assertNotNull(result[0].getDistance());
    }

    @Test
    public void testFetchRoute_invalidKey() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = {null};

        NavigationHelper.fetchRoute(
                start,
                end,
                RouteTravelMode.TRANSIT,
                "INVALID_KEY",
                new NavigationHelper.RoutesCallback() {
                    @Override
                    public void onSuccess(Route route) {
                        latch.countDown();
                    }
                    @Override
                    public void onError(Exception e) {
                        error[0] = e;
                        latch.countDown();
                    }
                }
        );

        latch.await(15, TimeUnit.SECONDS);
        assertNotNull("Should have failed with invalid key", error[0]);
    }
    private static String loadMockResponse() throws IOException {
        InputStream is = Objects.requireNonNull(NavigationHelperTest.class
                        .getClassLoader())
                .getResourceAsStream("mock_route_response.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

}
