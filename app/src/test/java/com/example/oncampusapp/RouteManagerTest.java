package com.example.oncampusapp;

import com.example.oncampusapp.navigation.RouteTravelMode;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RouteManagerTest {

    @Mock MapsActivity mockActivity;

    private RouteManager routeManager;

    @Before
    public void setUp() {
        routeManager = new RouteManager(mockActivity);
    }

    // ── Constructor & default state ───────────────────────────────────────────

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

    @Test
    public void defaultFirstRoutePoint_isNull() {
        assertNull(routeManager.getFirstRoutePoint());
    }

    // ── Setters / Getters ─────────────────────────────────────────────────────

    @Test
    public void setSelectedMode_drive_returnsCorrectMode() {
        routeManager.setSelectedMode(RouteTravelMode.DRIVE);
        assertEquals(RouteTravelMode.DRIVE, routeManager.getSelectedMode());
    }

    @Test
    public void setSelectedMode_transit_returnsCorrectMode() {
        routeManager.setSelectedMode(RouteTravelMode.TRANSIT);
        assertEquals(RouteTravelMode.TRANSIT, routeManager.getSelectedMode());
    }

    @Test
    public void setSelectedMode_shuttle_returnsCorrectMode() {
        routeManager.setSelectedMode(RouteTravelMode.SHUTTLE);
        assertEquals(RouteTravelMode.SHUTTLE, routeManager.getSelectedMode());
    }

    @Test
    public void setShuttleMarkers_updatesArray() {
        com.google.android.gms.maps.model.Marker[] markers = new com.google.android.gms.maps.model.Marker[2];
        routeManager.setShuttleMarkers(markers);
        assertSame(markers, routeManager.getShuttleMarkers());
    }

    @Test
    public void setBuildingsMap_storesMap() {
        Map<String, Building> map = new HashMap<>();
        routeManager.setBuildingsMap(map);
        // Verify indirectly: initiateRoutePreview with empty start/dest returns without crash
        routeManager.initiateRoutePreview("", "dest");
        routeManager.initiateRoutePreview("start", "");
    }

    // ── initiateRoutePreview early-exit paths ─────────────────────────────────

    @Test
    public void initiateRoutePreview_emptyStart_returnsImmediately() {
        routeManager.setBuildingsMap(new HashMap<>());
        // Should not throw — exits at the isEmpty() guard
        routeManager.initiateRoutePreview("", "Hall Building");
    }

    @Test
    public void initiateRoutePreview_emptyDest_returnsImmediately() {
        routeManager.setBuildingsMap(new HashMap<>());
        routeManager.initiateRoutePreview("Hall Building", "");
    }

    @Test
    public void initiateRoutePreview_bothEmpty_returnsImmediately() {
        routeManager.setBuildingsMap(new HashMap<>());
        routeManager.initiateRoutePreview("", "");
    }

    // ── parseDurationToMinutes ────────────────────────────────────────────────

    @Test
    public void parseDuration_null_returnsZero() {
        assertEquals(0, RouteManager.parseDurationToMinutes(null));
    }

    @Test
    public void parseDuration_empty_returnsZero() {
        assertEquals(0, RouteManager.parseDurationToMinutes(""));
        assertEquals(0, RouteManager.parseDurationToMinutes("   "));
    }

    @Test
    public void parseDuration_minutesOnly() {
        assertEquals(15, RouteManager.parseDurationToMinutes("15 mins"));
        assertEquals(1,  RouteManager.parseDurationToMinutes("1 min"));
        assertEquals(45, RouteManager.parseDurationToMinutes("45 minutes"));
    }

    @Test
    public void parseDuration_hoursOnly() {
        assertEquals(60,  RouteManager.parseDurationToMinutes("1 hour"));
        assertEquals(120, RouteManager.parseDurationToMinutes("2 hours"));
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
    public void parseDuration_noRecognizedTokens_returnsZero() {
        assertEquals(0, RouteManager.parseDurationToMinutes("unknown format"));
    }

    // ── checkForInsideRooms ───────────────────────────────────────────────────

    @Test
    public void checkForInsideRooms_regularBuildingName_returnsTrue() {
        assertTrue(RouteManager.checkForInsideRooms("Hall Building"));
        assertTrue(RouteManager.checkForInsideRooms("JMSB"));
        assertTrue(RouteManager.checkForInsideRooms("Loyola Campus"));
    }

    @Test
    public void checkForInsideRooms_hallRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("H-867"));
        assertFalse(RouteManager.checkForInsideRooms("H-110"));
    }

    @Test
    public void checkForInsideRooms_veRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("VE-101"));
    }

    @Test
    public void checkForInsideRooms_ccRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("CC-310"));
    }

    @Test
    public void checkForInsideRooms_lbRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("LB-207"));
    }

    @Test
    public void checkForInsideRooms_mbRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("MB-S2.440"));
    }

    @Test
    public void checkForInsideRooms_vlRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("VL-105"));
    }
}
