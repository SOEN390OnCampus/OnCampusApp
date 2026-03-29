package com.example.oncampusapp;

import com.example.oncampusapp.navigation.RouteTravelMode;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // ── Null-safe setters ─────────────────────────────────────────────────────

    @Test
    public void setMap_null_doesNotThrow() {
        routeManager.setMap(null);
    }

    @Test
    public void setLocationClient_null_doesNotThrow() {
        routeManager.setLocationClient(null);
    }

    // ── removeStartDot ────────────────────────────────────────────────────────

    @Test
    public void removeStartDot_nullStartDot_doesNotThrow() {
        // startDot is null by default — null check inside the method guards it
        routeManager.removeStartDot();
    }

    // ── stopNavigation ────────────────────────────────────────────────────────

    @Test
    public void stopNavigation_nullCallback_doesNotThrow() {
        // navigationLocationCallback is null by default — null check guards it
        routeManager.stopNavigation();
    }

    // ── clearNormalRoute ──────────────────────────────────────────────────────

    @Test
    public void clearNormalRoute_emptyState_doesNotThrow() {
        // routePolylines is empty, all markers null — all null-checks in place
        routeManager.clearNormalRoute();
    }

    @Test
    public void clearNormalRoute_doesNotLeaveStalePolylines() {
        routeManager.clearNormalRoute();
        assertTrue(routeManager.getRoutePolylines().isEmpty());
    }

    // ── clearShuttleRoute ─────────────────────────────────────────────────────

    @Test
    public void clearShuttleRoute_emptyState_doesNotThrow() {
        // all polylines null, activity.findViewById returns null — guarded by null check
        routeManager.clearShuttleRoute();
    }

    // ── resetRouteState ───────────────────────────────────────────────────────

    @Test
    public void resetRouteState_doesNotThrow() {
        routeManager.resetRouteState();
    }

    @Test
    public void resetRouteState_previewRemainsInactive() {
        routeManager.resetRouteState();
        assertFalse(routeManager.isPreviewActive());
    }

    // ── drawSegmentPolyline (null/empty guards) ───────────────────────────────

    @Test
    public void drawSegmentPolyline_nullMap_returnsNull() {
        // mMap is never set → null → method returns null immediately
        assertNull(routeManager.drawSegmentPolyline(new ArrayList<>(), false));
    }

    @Test
    public void drawSegmentPolyline_nullPath_returnsNull() {
        assertNull(routeManager.drawSegmentPolyline(null, false));
    }

    @Test
    public void drawSegmentPolyline_emptyPath_returnsNull() {
        List<LatLng> empty = new ArrayList<>();
        assertNull(routeManager.drawSegmentPolyline(empty, true));
    }

    // ── drawRouteOnMap (null/empty guards) ────────────────────────────────────

    @Test
    public void drawRouteOnMap_nullMap_returnsImmediately() {
        // mMap is null → first guard returns early, no crash
        routeManager.drawRouteOnMap(new ArrayList<>(), "5 mins", new ArrayList<>());
    }

    @Test
    public void drawRouteOnMap_nullPath_returnsImmediately() {
        routeManager.drawRouteOnMap(null, "5 mins", new ArrayList<>());
    }

    // ── updateRouteProgress (null guard) ─────────────────────────────────────

    @Test
    public void updateRouteProgress_nullRoutePoints_doesNotThrow() {
        // currentRoutePoints is null by default → first guard returns early
        routeManager.updateRouteProgress(new LatLng(45.4972, -73.5790));
    }

    // ── showCurrentDirection (empty guard) ───────────────────────────────────

    @Test
    public void showCurrentDirection_emptyDirections_doesNotThrow() {
        // directionsList is empty → guard returns early before any Android call
        routeManager.showCurrentDirection();
    }

    // ── applySameCampusCheck — non-shuttle modes return false immediately ─────

    @Test
    public void applySameCampusCheck_walkMode_returnsFalse() {
        routeManager.setSelectedMode(RouteTravelMode.WALK);
        LatLng sgw = new LatLng(45.4972, -73.5790);
        assertFalse(routeManager.applySameCampusCheck(sgw, sgw));
    }

    @Test
    public void applySameCampusCheck_driveMode_returnsFalse() {
        routeManager.setSelectedMode(RouteTravelMode.DRIVE);
        LatLng loy = new LatLng(45.4582, -73.6405);
        assertFalse(routeManager.applySameCampusCheck(loy, loy));
    }

    @Test
    public void applySameCampusCheck_transitMode_returnsFalse() {
        routeManager.setSelectedMode(RouteTravelMode.TRANSIT);
        LatLng sgw = new LatLng(45.4972, -73.5790);
        assertFalse(routeManager.applySameCampusCheck(sgw, sgw));
    }

    // ── getFirstRoutePoint after clear ────────────────────────────────────────

    @Test
    public void getFirstRoutePoint_afterClearNormalRoute_remainsNull() {
        routeManager.clearNormalRoute();
        assertNull(routeManager.getFirstRoutePoint());
    }
}
