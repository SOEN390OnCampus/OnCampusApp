package com.example.oncampusapp;

import android.graphics.Color;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.oncampusapp.navigation.Direction;
import com.example.oncampusapp.navigation.RouteTravelMode;
import com.example.oncampusapp.location.ILocationProvider;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Helper method to use Reflection to set private fields in RouteManager
     * without needing to trigger complex public methods to populate them.
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Getters & Setters Tests ───────────────────────────────────────────────

    @Test
    public void testGettersAndSetters() {
        Marker[] mockMarkers = new Marker[]{mock(Marker.class), mock(Marker.class)};
        routeManager.setShuttleMarkers(mockMarkers);
        assertArrayEquals(mockMarkers, routeManager.getShuttleMarkers());

        routeManager.setSelectedMode(RouteTravelMode.TRANSIT);
        assertEquals(RouteTravelMode.TRANSIT, routeManager.getSelectedMode());
    }

    // ── Original State & Logic Tests ──────────────────────────────────────────

    @Test
    public void defaultSelectedMode_isWalk() {
        assertEquals(RouteTravelMode.WALK, routeManager.getSelectedMode());
        assertFalse(routeManager.isPreviewActive());
        assertTrue(routeManager.getRoutePolylines().isEmpty());
    }

    @Test
    public void setSelectedMode_updatesCorrectly() {
        routeManager.setSelectedMode(RouteTravelMode.DRIVE);
        assertEquals(RouteTravelMode.DRIVE, routeManager.getSelectedMode());
    }

    @Test
    public void parseDuration_formatsCorrectly() {
        assertEquals(0, RouteManager.parseDurationToMinutes(""));
        assertEquals(0, RouteManager.parseDurationToMinutes(null));
        assertEquals(15, RouteManager.parseDurationToMinutes("15 mins"));
        assertEquals(90, RouteManager.parseDurationToMinutes("1 hour 30 mins"));
        assertEquals(75, RouteManager.parseDurationToMinutes("1 HOUR 15 MINS"));
    }

    @Test
    public void checkForInsideRooms_handlesLogic() {
        assertTrue(RouteManager.checkForInsideRooms("Hall Building"));
        assertTrue(RouteManager.checkForInsideRooms("Loyola Campus"));
        assertFalse(RouteManager.checkForInsideRooms("H-867"));
        assertFalse(RouteManager.checkForInsideRooms("MB-2.130"));
    }

    // ── UI Integration Tests ──────────────────────────────────────────────────

    @Test
    public void initiateRoutePreview_emptyNames_returnsEarlyWithoutToast() {
        routeManager.initiateRoutePreview("", "H-867");
        assertNull("Should return early without toasting", ShadowToast.getTextOfLatestToast());

        routeManager.initiateRoutePreview("H-867", "");
        assertNull("Should return early without toasting", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void initiateRoutePreview_emptyBuildingsMap_showsLoadingToast() {
        routeManager.setBuildingsMap(new HashMap<>()); // Empty map simulates loading state

        routeManager.initiateRoutePreview("Hall Building", "Loyola Campus");

        assertEquals("Map is still loading, please wait", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void initiateRoutePreview_withUnknownBuilding_showsToast() {
        Map<String, Building> mockMap = new HashMap<>();
        mockMap.put("H", new Building("H", "Hall Building", new ArrayList<>()));
        routeManager.setBuildingsMap(mockMap);

        routeManager.initiateRoutePreview("Unknown Building", "H-867");

        assertEquals("Could not find: \"Unknown Building\"", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void applySameCampusCheck_shuttleMode_sameCampus_updatesUIAndMode() {
        ImageButton mockBtnWalk = new ImageButton(activity);
        Button mockBtnShuttle = new Button(activity);
        Button mockBtnGo = new Button(activity);
        mockBtnGo.setLayoutParams(new LinearLayout.LayoutParams(0, 0));

        routeManager.setTransportButtons(mockBtnWalk, mockBtnShuttle, mockBtnGo);
        routeManager.setSelectedMode(RouteTravelMode.SHUTTLE);

        // Coordinates close to each other (SGW)
        LatLng sgw1 = new LatLng(45.4972, -73.5790);
        LatLng sgw2 = new LatLng(45.4960, -73.5780);

        boolean didSwitch = routeManager.applySameCampusCheck(sgw1, sgw2);

        assertTrue(didSwitch);
        assertEquals(RouteTravelMode.WALK, routeManager.getSelectedMode());
        assertEquals("Both locations are on the same campus — switched to walking", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void applySameCampusCheck_walkMode_doesNothing() {
        routeManager.setSelectedMode(RouteTravelMode.WALK);
        LatLng sgw1 = new LatLng(45.4972, -73.5790);
        LatLng sgw2 = new LatLng(45.4960, -73.5780);

        boolean didSwitch = routeManager.applySameCampusCheck(sgw1, sgw2);

        assertFalse("Should not apply campus check if not in Shuttle mode", didSwitch);
    }

    @Test
    public void testAdjustGoButtonWidth() {
        Button btnGo = new Button(activity);
        btnGo.setLayoutParams(new LinearLayout.LayoutParams(0, 0));

        // Action: Adjust for Timetable visible
        routeManager.adjustGoButtonWidth(btnGo, true);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnGo.getLayoutParams();

        // Assert: Weight expands, margin is added
        assertEquals(1.3f, params.weight, 0.01f);
        assertTrue("Margin should be applied", params.getMarginEnd() > 0);

        // Action: Adjust for Timetable hidden
        routeManager.adjustGoButtonWidth(btnGo, false);
        params = (LinearLayout.LayoutParams) btnGo.getLayoutParams();

        // Assert: Shrinks back, margin removed
        assertEquals(1.0f, params.weight, 0.01f);
        assertEquals(0, params.getMarginEnd());
    }

    // ── Google Maps Mocking Tests ─────────────────────────────────────────────

    @Test
    public void testDrawSegmentPolyline_addsToMap() {
        GoogleMap mockMap = mock(GoogleMap.class);
        Polyline mockPolyline = mock(Polyline.class);
        when(mockMap.addPolyline(any(PolylineOptions.class))).thenReturn(mockPolyline);

        routeManager.setMap(mockMap);
        List<LatLng> path = Arrays.asList(new LatLng(1, 1), new LatLng(2, 2));

        Polyline result = routeManager.drawSegmentPolyline(path, true);

        assertNotNull("Should return the polyline created by the map", result);
        verify(mockMap, times(1)).addPolyline(any(PolylineOptions.class));
    }

    @Test
    public void testDrawSegmentPolyline_nullOrEmptyPath_returnsNull() {
        assertNull(routeManager.drawSegmentPolyline(null, true));
        assertNull(routeManager.drawSegmentPolyline(new ArrayList<>(), true));
    }

    @Test
    public void testClearNormalRoute_removesAllMapObjects() throws Exception {
        Polyline mockPoly = mock(Polyline.class);
        Circle mockCircle = mock(Circle.class);
        Marker mockMarker = mock(Marker.class);

        // Inject objects directly into the manager's private state
        routeManager.getRoutePolylines().add(mockPoly);
        setPrivateField(routeManager, "startDot", mockCircle);
        setPrivateField(routeManager, "endMarker", mockMarker);
        setPrivateField(routeManager, "currentRoutePoints", new ArrayList<>(Arrays.asList(new LatLng(0, 0))));

        // Action
        routeManager.clearNormalRoute();

        // Assertion: Verify Google Maps SDK remove() was triggered to clear the map
        verify(mockPoly, times(1)).remove();
        verify(mockCircle, times(1)).remove();
        verify(mockMarker, times(1)).remove();

        // Assertion: Verify internal state is clean
        assertTrue(routeManager.getRoutePolylines().isEmpty());
        assertNull(routeManager.getFirstRoutePoint());
    }

    @Test
    public void testClearShuttleRoute_removesPolylines() throws Exception {
        Polyline walkTo = mock(Polyline.class);
        Polyline shuttle = mock(Polyline.class);
        Polyline walkFrom = mock(Polyline.class);

        setPrivateField(routeManager, "walkToStopPolyline", walkTo);
        setPrivateField(routeManager, "shuttlePolyline", shuttle);
        setPrivateField(routeManager, "walkFromStopPolyline", walkFrom);

        routeManager.clearShuttleRoute();

        verify(walkTo, times(1)).remove();
        verify(shuttle, times(1)).remove();
        verify(walkFrom, times(1)).remove();
    }

    @Test
    public void testRemoveStartDot() throws Exception {
        Circle mockCircle = mock(Circle.class);
        setPrivateField(routeManager, "startDot", mockCircle);

        routeManager.removeStartDot();

        verify(mockCircle, times(1)).remove();

        // Ensure it doesn't crash if called again when already null
        routeManager.removeStartDot();
        verify(mockCircle, times(1)).remove(); // Count shouldn't go up
    }

    // ── Navigation State Machine Tests ────────────────────────────────────────

    @Test
    public void testStartNavigationUpdates_requestsLocation() {
        ILocationProvider mockProvider = mock(ILocationProvider.class);
        routeManager.setLocationClient(mockProvider);

        routeManager.startNavigationUpdates();

        // Verify requestLocationUpdates was called with correct parameters
        ArgumentCaptor<LocationRequest> requestCaptor = ArgumentCaptor.forClass(LocationRequest.class);
        verify(mockProvider, times(1)).requestLocationUpdates(
                requestCaptor.capture(),
                any(LocationCallback.class),
                eq(Looper.getMainLooper())
        );

        assertNotNull(requestCaptor.getValue());
    }

    @Test
    public void testStopNavigation_removesLocationUpdates() throws Exception {
        ILocationProvider mockProvider = mock(ILocationProvider.class);
        LocationCallback mockCallback = mock(LocationCallback.class);

        routeManager.setLocationClient(mockProvider);
        setPrivateField(routeManager, "navigationLocationCallback", mockCallback);

        routeManager.stopNavigation();

        verify(mockProvider, times(1)).removeLocationUpdates(mockCallback);
    }

    @Test
    public void testNavigateDirection_boundsChecking() throws Exception {
        // Setup: Inject dummy directions
        List<Direction> dummyDirs = Arrays.asList(mock(Direction.class), mock(Direction.class));
        setPrivateField(routeManager, "directionsList", dummyDirs);
        setPrivateField(routeManager, "currentDirectionIndex", 0);

        // Action: Go forward to index 1
        routeManager.navigateToNextDirection();

        // Action: Try to go forward again (out of bounds)
        routeManager.navigateToNextDirection();

        // Assertion
        assertEquals("You are at the last step", ShadowToast.getTextOfLatestToast());

        // Action: Go backward to index 0
        routeManager.navigateToPreviousDirection();

        // Action: Try to go backward again (out of bounds)
        routeManager.navigateToPreviousDirection();

        // Assertion
        assertEquals("You are at the first step", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testUpdateRouteProgress_nullOrEmptyPoints_doesNothing() throws Exception {
        LatLng location = new LatLng(0, 0);

        // Null check
        setPrivateField(routeManager, "currentRoutePoints", null);
        routeManager.updateRouteProgress(location); // Should not crash

        // Empty check
        setPrivateField(routeManager, "currentRoutePoints", new ArrayList<>());
        routeManager.updateRouteProgress(location); // Should not crash
    }

    @Test
    public void testUpdateRouteProgress_whenArrived_showsToast() throws Exception {
        // Setup destination
        LatLng dest = new LatLng(45.4972, -73.5790);
        List<LatLng> points = new ArrayList<>(Arrays.asList(new LatLng(45.4970, -73.5780), dest));

        setPrivateField(routeManager, "currentRoutePoints", points);

        // Add a dummy polyline to bypass the routePolylines emptiness check
        routeManager.getRoutePolylines().add(mock(Polyline.class));

        // Simulate user arriving EXACTLY at the destination
        routeManager.updateRouteProgress(dest);

        assertEquals("You have arrived!", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testGetFirstRoutePoint() throws Exception {
        // Test null
        assertNull(routeManager.getFirstRoutePoint());

        // Test Empty
        setPrivateField(routeManager, "currentRoutePoints", new ArrayList<>());
        assertNull(routeManager.getFirstRoutePoint());

        // Test Populated
        LatLng firstPoint = new LatLng(10, 10);
        setPrivateField(routeManager, "currentRoutePoints", new ArrayList<>(Arrays.asList(firstPoint, new LatLng(20, 20))));
        assertEquals(firstPoint, routeManager.getFirstRoutePoint());
    }

    @Test
    public void testResetRouteState_clearsPreviewAndNavigation() {
        // Setup some dirty state
        routeManager.setSelectedMode(RouteTravelMode.DRIVE); // Mode shouldn't reset

        routeManager.resetRouteState();

        // Assertions
        assertFalse(routeManager.isPreviewActive());
        assertEquals(RouteTravelMode.DRIVE, routeManager.getSelectedMode());
    }
}