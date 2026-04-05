package com.example.oncampusapp;

import android.graphics.Color;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.oncampusapp.navigation.RouteTravelMode;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RouteManagerTest {

    private MapsActivity activity;
    private RouteManager routeManager;

    @Before
    public void setUp() {
        // Build the Activity so we have a real Android Context for Toasts and Views
        activity = Robolectric.buildActivity(MapsActivity.class).get();
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);

        routeManager = new RouteManager(activity);
    }

    // ── Your Existing Constructor & Default State Tests ───────────────────────

    @Test
    public void defaultSelectedMode_isWalk() {
        assertEquals(RouteTravelMode.WALK, routeManager.getSelectedMode());
    }

    @Test
    public void defaultIsPreviewActive_isFalse() {
        assertFalse(routeManager.isPreviewActive());
    }

    @Test
    public void defaultRoutePolylines_isEmpty() {
        assertNotNull(routeManager.getRoutePolylines());
        assertTrue(routeManager.getRoutePolylines().isEmpty());
    }

    @Test
    public void defaultShuttleMarkers_hasTwoNullSlots() {
        assertNotNull(routeManager.getShuttleMarkers());
        assertEquals(2, routeManager.getShuttleMarkers().length);
        assertNull(routeManager.getShuttleMarkers()[0]);
        assertNull(routeManager.getShuttleMarkers()[1]);
    }

    // ── Your Existing Setters / Getters ───────────────────────────────────────

    @Test
    public void setSelectedMode_drive_returnsCorrectMode() {
        routeManager.setSelectedMode(RouteTravelMode.DRIVE);
        assertEquals(RouteTravelMode.DRIVE, routeManager.getSelectedMode());
    }

    // ── Your Existing Pure Logic Tests ────────────────────────────────────────

    @Test
    public void parseDuration_minutesOnly() {
        assertEquals(15, RouteManager.parseDurationToMinutes("15 mins"));
        assertEquals(1,  RouteManager.parseDurationToMinutes("1 min"));
        assertEquals(45, RouteManager.parseDurationToMinutes("45 minutes"));
    }

    @Test
    public void parseDuration_hoursAndMinutes() {
        assertEquals(90,  RouteManager.parseDurationToMinutes("1 hour 30 mins"));
        assertEquals(125, RouteManager.parseDurationToMinutes("2 hours 5 mins"));
    }

    @Test
    public void parseDuration_uppercaseInput() {
        assertEquals(20, RouteManager.parseDurationToMinutes("20 MINS"));
        assertEquals(75, RouteManager.parseDurationToMinutes("1 HOUR 15 MINS"));
    }

    @Test
    public void checkForInsideRooms_regularBuildingName_returnsTrue() {
        assertTrue(RouteManager.checkForInsideRooms("Hall Building"));
        assertTrue(RouteManager.checkForInsideRooms("JMSB"));
    }

    @Test
    public void checkForInsideRooms_hallRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("H-867"));
    }

    // ── NEW: Robolectric UI & Integration Tests ───────────────────────────────

    @Test
    public void initiateRoutePreview_withUnknownBuilding_showsToast() {
        // FIX: Provide a non-empty buildings map so it bypasses the "Map is still loading" check.
        Map<String, Building> mockMap = new HashMap<>();
        mockMap.put("H", new Building("H", "Hall Building", new ArrayList<>()));
        routeManager.setBuildingsMap(mockMap);

        // Action: Try to route from a missing building.
        // We put "Unknown Building" as the start point so it gets flagged as the missing coordinate.
        routeManager.initiateRoutePreview("Unknown Building", "H-867");

        // Assertion: Verify the specific Toast message was fired
        String latestToast = ShadowToast.getTextOfLatestToast();
        assertEquals("Could not find: \"Unknown Building\"", latestToast);
    }

    @Test
    public void applySameCampusCheck_shuttleMode_sameCampus_updatesUIAndMode() {
        // Setup: Mock the UI buttons that the manager manipulates
        ImageButton mockBtnWalk = new ImageButton(activity);
        Button mockBtnShuttle = new Button(activity);
        Button mockBtnGo = new Button(activity);

        // Buttons must have LayoutParams to avoid crashes when RouteManager tries to adjust them
        mockBtnGo.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, 0));

        routeManager.setTransportButtons(mockBtnWalk, mockBtnShuttle, mockBtnGo);
        routeManager.setSelectedMode(RouteTravelMode.SHUTTLE);

        // Action: Pass two coordinates that are on the same campus (e.g., both SGW)
        LatLng sgw1 = new LatLng(45.4972, -73.5790);
        LatLng sgw2 = new LatLng(45.4960, -73.5780);

        boolean didSwitch = routeManager.applySameCampusCheck(sgw1, sgw2);

        // Assertion 1: Logic should detect same campus and switch to WALK
        assertTrue("Should return true because campuses are the same", didSwitch);
        assertEquals("Mode should auto-switch to WALK", RouteTravelMode.WALK, routeManager.getSelectedMode());

        // Assertion 2: UI should be updated to reflect the Walk state
        assertEquals("Shuttle timetable button should be hidden", android.view.View.GONE, mockBtnShuttle.getVisibility());

        // Assertion 3: Verify the Toast
        assertEquals("Both locations are on the same campus — switched to walking", ShadowToast.getTextOfLatestToast());
    }
}