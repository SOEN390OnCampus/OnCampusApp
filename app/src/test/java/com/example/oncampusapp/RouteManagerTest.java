package com.example.oncampusapp;

import android.graphics.Color;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.oncampusapp.navigation.Direction;
import com.example.oncampusapp.navigation.NavigationHelper;
import com.example.oncampusapp.navigation.RouteTravelMode;
import com.example.oncampusapp.navigation.Step;
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
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

        // Assert: Weight expands, margin is added (4px on density 1.0)
        assertEquals(1.3f, params.weight, 0.01f);
        assertEquals(4, params.getMarginEnd());

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

    @Test
    public void testTryUpdateShuttleTotal_WhenAllDone_UpdatesTextView() throws Exception {
        TextView mockTv = new TextView(activity);
        mockTv.setId(R.id.txt_duration);
        activity.setContentView(mockTv);

        // Using reflection to invoke the private helper
        Method method = RouteManager.class.getDeclaredMethod("tryUpdateShuttleTotal",
                int[].class, int[].class, int[].class, boolean[].class, boolean[].class, boolean[].class);
        method.setAccessible(true);

        int[] to = {5}, shut = {15}, from = {5};
        boolean[] toD = {true}, shutD = {true}, fromD = {true};

        // Action: All boolean flags are true
        method.invoke(routeManager, to, shut, from, toD, shutD, fromD);

        // Assertion: 5 + 15 + 5 = 25
        assertEquals("25 MIN", mockTv.getText().toString());
    }

    @Test
    public void testTryUpdateShuttleTotal_WhenNotDone_DoesNothing() throws Exception {
        TextView mockTv = new TextView(activity);
        mockTv.setId(R.id.txt_duration);
        mockTv.setText("OLD VALUE");

        Method method = RouteManager.class.getDeclaredMethod("tryUpdateShuttleTotal",
                int[].class, int[].class, int[].class, boolean[].class, boolean[].class, boolean[].class);
        method.setAccessible(true);

        // One flag is false
        method.invoke(routeManager, new int[]{0}, new int[]{0}, new int[]{0},
                new boolean[]{true}, new boolean[]{false}, new boolean[]{true});

        assertEquals("OLD VALUE", mockTv.getText().toString());
    }

    @Test
    public void testBuildPolylineOptions_WalkMode_SetsDottedPattern() throws Exception {
        Step walkStep = mock(Step.class);
        when(walkStep.getTravelMode()).thenReturn(RouteTravelMode.WALK);
        when(walkStep.getPoints()).thenReturn(Arrays.asList(new LatLng(0,0)));

        Method method = RouteManager.class.getDeclaredMethod("buildPolylineOptions", Step.class);
        method.setAccessible(true);

        PolylineOptions options = (PolylineOptions) method.invoke(routeManager, walkStep);

        assertNotNull(options.getPattern());
        assertEquals(Color.parseColor("#4285F4"), options.getColor());
    }

    @Test
    public void testBuildPolylineOptions_TransitMode_SetsLineColor() throws Exception {
        Step transitStep = mock(Step.class);
        com.example.oncampusapp.navigation.TransitDetails details = mock(com.example.oncampusapp.navigation.TransitDetails.class);
        com.example.oncampusapp.navigation.TransitLine line = mock(com.example.oncampusapp.navigation.TransitLine.class);

        when(transitStep.getTravelMode()).thenReturn(RouteTravelMode.TRANSIT);
        when(transitStep.getTransitDetails()).thenReturn(details);
        when(details.getTransitLine()).thenReturn(line);
        when(line.getColor()).thenReturn("#FF0000"); // Red line
        when(transitStep.getPoints()).thenReturn(Arrays.asList(new LatLng(0,0)));

        Method method = RouteManager.class.getDeclaredMethod("buildPolylineOptions", Step.class);
        method.setAccessible(true);

        PolylineOptions options = (PolylineOptions) method.invoke(routeManager, transitStep);

        assertEquals(Color.RED, options.getColor());
        assertNull("Transit should have solid line (no pattern)", options.getPattern());
    }

    @Test
    public void testShowCurrentDirection_UpdatesTextView() throws Exception {
        TextView mockTextDir = new TextView(activity);
        mockTextDir.setId(R.id.textDir);
        activity.setContentView(mockTextDir);

        Direction mockDir = mock(Direction.class);
        when(mockDir.getInstructions()).thenReturn("Turn Left");
        when(mockDir.getDistance()).thenReturn("100m");

        List<Direction> dirs = new ArrayList<>();
        dirs.add(mockDir);

        setPrivateField(routeManager, "directionsList", dirs);
        setPrivateField(routeManager, "currentDirectionIndex", 0);

        routeManager.showCurrentDirection();

        assertTrue(mockTextDir.getText().toString().contains("Turn Left"));
        assertTrue(mockTextDir.getText().toString().contains("100m"));
    }

    @Test
    public void testUpdateRouteProgress_UpdatesPolylinePoints() throws Exception {
        Polyline mockPoly = mock(Polyline.class);
        routeManager.getRoutePolylines().add(mockPoly);

        // 1. Give currentRoutePoints a size of 2
        List<LatLng> originalPoints = new ArrayList<>(Arrays.asList(
                new LatLng(0, 0),
                new LatLng(1, 1)
        ));
        setPrivateField(routeManager, "currentRoutePoints", originalPoints);
        setPrivateField(routeManager, "isPreviewActive", true);

        // 2. Intercept the static NavigationHelper class to force our desired outcome
        try (MockedStatic<NavigationHelper> mockedHelper = mockStatic(NavigationHelper.class)) {

            // Force getUpdatedPath to return a list of size 1
            // (This guarantees updatedPath.size() != currentRoutePoints.size() will be TRUE)
            List<LatLng> smallerPath = new ArrayList<>(Arrays.asList(new LatLng(1, 1)));
            mockedHelper.when(() -> NavigationHelper.getUpdatedPath(
                    any(LatLng.class), anyList(), anyDouble()
            )).thenReturn(smallerPath);

            // Prevent the hasArrived method from triggering unwanted UI logic in this test
            mockedHelper.when(() -> NavigationHelper.hasArrived(
                    any(LatLng.class), anyList(), anyDouble()
            )).thenReturn(false);

            // 3. Trigger the progress update
            routeManager.updateRouteProgress(new LatLng(1, 1));
        }

        // 4. Verify setPoints was FINALLY called!
        verify(mockPoly, atLeastOnce()).setPoints(anyList());
    }

    @Test
    public void initiateRoutePreview_validInputs_callsStandardRoute() {
        Map<String, Building> map = new HashMap<>();
        map.put("A", new Building("A", "A", new ArrayList<>()));
        map.put("B", new Building("B", "B", new ArrayList<>()));

        routeManager.setBuildingsMap(map);
        routeManager.setSelectedMode(RouteTravelMode.WALK);

        // Mock static BuildingLookup
        try (MockedStatic<BuildingLookup> mocked = mockStatic(BuildingLookup.class)) {
            mocked.when(() -> BuildingLookup.getLatLngFromBuildingName(eq("A"), any()))
                    .thenReturn(new LatLng(1,1));
            mocked.when(() -> BuildingLookup.getLatLngFromBuildingName(eq("B"), any()))
                    .thenReturn(new LatLng(2,2));

            // Mock NavigationHelper to avoid real call
            try (MockedStatic<NavigationHelper> navMock = mockStatic(NavigationHelper.class)) {
                navMock.when(() -> NavigationHelper.fetchRoute(
                        any(), any(), any(), any(), any()
                )).thenAnswer(invocation -> null);

                routeManager.initiateRoutePreview("A", "B");

                navMock.verify(() -> NavigationHelper.fetchRoute(
                        any(), any(), eq(RouteTravelMode.WALK), any(), any()
                ));
            }
        }
    }

    @Test
    public void initiateRoutePreview_shuttleMode_callsShuttleRoute() {
        routeManager.setSelectedMode(RouteTravelMode.SHUTTLE);

        Map<String, Building> map = new HashMap<>();
        map.put("A", new Building("A", "A", new ArrayList<>()));
        map.put("B", new Building("B", "B", new ArrayList<>()));
        routeManager.setBuildingsMap(map);

        try (MockedStatic<BuildingLookup> mocked = mockStatic(BuildingLookup.class)) {
            mocked.when(() -> BuildingLookup.getLatLngFromBuildingName(any(), any()))
                    .thenReturn(new LatLng(1,1));

            // Prevent crash inside shuttle logic
            routeManager.setMap(mock(GoogleMap.class));
            routeManager.setShuttleMarkers(new Marker[]{mock(Marker.class), mock(Marker.class)});

            routeManager.initiateRoutePreview("A", "B");
        }
    }
    @Test
    public void drawRouteOnMap_emptyPath_throwsException() {
        GoogleMap map = mock(GoogleMap.class);
        routeManager.setMap(map);

        // Extract the lists outside the assertThrows lambda
        List<LatLng> emptyPath = new ArrayList<>();
        List<Step> emptySteps = new ArrayList<>();
        String duration = "10 mins";

        // Now the lambda only contains the single method invocation
        assertThrows(IllegalStateException.class, () -> {
            routeManager.drawRouteOnMap(emptyPath, duration, emptySteps);
        });

        // Ensure the map was never touched before the crash
        verifyNoInteractions(map);
    }

    @Test
    public void drawRouteOnMap_nullMap_returnsEarly() {
        routeManager.setMap(null);

        routeManager.drawRouteOnMap(
                Arrays.asList(new LatLng(0,0)),
                "10 mins",
                new ArrayList<>()
        );
        // Will throw NPE if early return fails, so just running this without exception is passing
    }

    @Test
    public void drawSegmentPolyline_nullMap_returnsNull() {
        routeManager.setMap(null);

        Polyline result = routeManager.drawSegmentPolyline(
                Arrays.asList(new LatLng(1,1)),
                true
        );

        assertNull(result);
    }

    @Test
    public void startNavigationUpdates_removesOldCallback() throws Exception {
        ILocationProvider provider = mock(ILocationProvider.class);
        LocationCallback oldCallback = mock(LocationCallback.class);

        routeManager.setLocationClient(provider);
        setPrivateField(routeManager, "navigationLocationCallback", oldCallback);

        routeManager.startNavigationUpdates();

        verify(provider).removeLocationUpdates(oldCallback);
    }

    @Test
    public void showCurrentDirection_emptyList_doesNothing() {
        routeManager.showCurrentDirection();
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void showCurrentDirection_nullTextView_doesNothing() throws Exception {
        List<Direction> dirs = Arrays.asList(mock(Direction.class));
        setPrivateField(routeManager, "directionsList", dirs);

        // No TextView in layout
        activity.setContentView(new View(activity));

        routeManager.showCurrentDirection();
    }

    @Test
    public void clearShuttleRoute_nullFields_doesNotCrash() throws Exception {
        setPrivateField(routeManager, "walkToStopPolyline", null);
        setPrivateField(routeManager, "shuttlePolyline", null);
        setPrivateField(routeManager, "walkFromStopPolyline", null);

        routeManager.clearShuttleRoute();
    }

    @Test
    public void parseDuration_edgeCases_moreCoverage() {
        assertEquals(120, RouteManager.parseDurationToMinutes("2 hours"));
        assertEquals(30, RouteManager.parseDurationToMinutes("30 mins extra text"));
        assertEquals(0, RouteManager.parseDurationToMinutes("mins"));
        // Multiple hours and minutes
        assertEquals(145, RouteManager.parseDurationToMinutes("2 hours 25 mins"));
        // Malformed strings that should return 0
        assertEquals(0, RouteManager.parseDurationToMinutes("just some text"));
        assertEquals(0, RouteManager.parseDurationToMinutes("hours 10")); // Wrong order
    }

    @Test
    public void navigateToNextDirection_emptyList_showsToast() {
        routeManager.navigateToNextDirection();
        assertEquals("You are at the last step", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void navigateToPreviousDirection_emptyList_showsToast() {
        routeManager.navigateToPreviousDirection();
        assertEquals("You are at the first step", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void resetRouteState_clearsEverything() throws Exception {
        setPrivateField(routeManager, "isPreviewActive", true);
        setPrivateField(routeManager, "currentRoutePoints", new ArrayList<>(Arrays.asList(new LatLng(1,1))));

        routeManager.resetRouteState();

        assertFalse(routeManager.isPreviewActive());
        assertNull(routeManager.getFirstRoutePoint());
    }

    @Test
    public void applySameCampusCheck_nullButtons_noCrash() {
        routeManager.setSelectedMode(RouteTravelMode.SHUTTLE);

        LatLng a = new LatLng(45.4972, -73.5790);
        LatLng b = new LatLng(45.4960, -73.5780);

        boolean result = routeManager.applySameCampusCheck(a, b);
        assertTrue(result);
        assertEquals(RouteTravelMode.WALK, routeManager.getSelectedMode());
    }

    @Test
    public void testShowWalkToStopResult_TogglesVisibility() throws Exception {
        LinearLayout layout = new LinearLayout(activity);
        layout.setId(R.id.layout_walk_to_shuttle);
        TextView tv = new TextView(activity);
        tv.setId(R.id.txt_walk_to_shuttle);

        activity.setContentView(layout);
        layout.addView(tv);

        // Reflection to call private method
        Method method = RouteManager.class.getDeclaredMethod("showWalkToStopResult", List.class, int.class);
        method.setAccessible(true);

        List<Step> emptyList = new ArrayList<>();

        // Test with 5 minutes (Should be VISIBLE)
        method.invoke(routeManager, emptyList, 5);
        assertEquals(View.VISIBLE, layout.getVisibility());
        assertEquals("5 MIN TO STOP", tv.getText().toString());

        // Test with 0 minutes (Should be GONE)
        method.invoke(routeManager, emptyList, 0);
        assertEquals(View.GONE, layout.getVisibility());
    }

    @Test
    public void testApplySameCampusCheck_DifferentCampus_DoesNothing() {
        routeManager.setSelectedMode(RouteTravelMode.SHUTTLE);

        // SGW vs LOY (Far apart)
        LatLng sgw = new LatLng(45.4961, -73.5773);
        LatLng loy = new LatLng(45.4582, -73.6405);

        boolean switched = routeManager.applySameCampusCheck(sgw, loy);

        assertFalse("Should NOT switch to walk if they are on different campuses", switched);
        assertEquals(RouteTravelMode.SHUTTLE, routeManager.getSelectedMode());
    }

    @Test
    public void testDrawRouteOnMap_TransitBranch() throws Exception {
        GoogleMap mockMap = mock(GoogleMap.class);
        routeManager.setMap(mockMap);

        // Build a transit step
        Step transitStep = mock(Step.class);
        com.example.oncampusapp.navigation.TransitDetails details = mock(com.example.oncampusapp.navigation.TransitDetails.class);
        com.example.oncampusapp.navigation.TransitLine line = mock(com.example.oncampusapp.navigation.TransitLine.class);

        when(transitStep.getTravelMode()).thenReturn(RouteTravelMode.TRANSIT);
        when(transitStep.getTransitDetails()).thenReturn(details);
        when(details.getTransitLine()).thenReturn(line);
        when(line.getColor()).thenReturn("#ABCDEF");
        when(transitStep.getPoints()).thenReturn(Arrays.asList(new LatLng(10, 10), new LatLng(11, 11)));

        // Mock locations for transit stops
        when(details.getDepartureStopLocation()).thenReturn(new LatLng(10, 10));
        when(details.getArrivalStopLocation()).thenReturn(new LatLng(11, 11));

        // Extract list creation OUTSIDE the assertThrows lambda
        List<LatLng> path = Arrays.asList(new LatLng(10, 10));
        String duration = "10 mins";
        List<Step> steps = Arrays.asList(transitStep);

        // Action: assert throws NPE because CameraUpdateFactory is unmocked in Robolectric
        assertThrows(NullPointerException.class, () -> {
            // Only one single invocation exists inside the lambda now!
            routeManager.drawRouteOnMap(path, duration, steps);
        });

        // Verify map successfully added the transit-colored line prior to the camera movement crash
        verify(mockMap, atLeastOnce()).addPolyline(any(PolylineOptions.class));
    }

    @Test
    public void testNavigateToNextDirection_CrossBuildingFinish() throws Exception {
        // Setup activity to return true for pending indoor
        MapsActivity spyActivity = spy(activity);
        doReturn(true).when(spyActivity).hasPendingFinalIndoorAfterOutdoor();

        // Create new manager with spy
        RouteManager spyManager = new RouteManager(spyActivity);

        // Inject directions so we are at the end
        Direction mockDir = mock(Direction.class);
        setPrivateField(spyManager, "directionsList", Arrays.asList(mockDir));
        setPrivateField(spyManager, "currentDirectionIndex", 0);

        // Mock the "End Trip" button to simulate the click
        Button btnEndTrip = new Button(spyActivity);
        btnEndTrip.setId(R.id.btn_end_trip);
        spyActivity.setContentView(btnEndTrip);

        // Action: navigate at the last step
        spyManager.navigateToNextDirection();

        // Verify it checked the cross-building transition on the activity
        verify(spyActivity, atLeastOnce()).hasPendingFinalIndoorAfterOutdoor();
    }

    @Test
    public void testHideKeyboard_Branch() throws Exception {
        // Setup a view that has focus
        View view = new View(activity);
        activity.setContentView(view);
        view.requestFocus();

        // Invoke private hideKeyboard
        java.lang.reflect.Method method = RouteManager.class.getDeclaredMethod("hideKeyboard");
        method.setAccessible(true);

        // Action
        method.invoke(routeManager);
    }

    @Test
    public void testClearNormalRoute_HandlesNulls() throws Exception {
        setPrivateField(routeManager, "startDot", null);
        setPrivateField(routeManager, "endMarker", null);

        // Should not crash
        routeManager.clearNormalRoute();
        routeManager.removeStartDot();
    }

    @Test
    public void testInitialization_AndSimpleSetters() {
        // Constructor test
        RouteManager manager = new RouteManager(activity);
        assertNotNull(manager);

        // Setters for UI components to hit those lines
        manager.setTransportButtons(mock(ImageButton.class), mock(Button.class), mock(Button.class));
        manager.setBuildingsMap(new HashMap<>());

        // Verifying the getter after setting
        manager.setSelectedMode(RouteTravelMode.WALK);
        assertEquals(RouteTravelMode.WALK, manager.getSelectedMode());
    }

    @Test
    public void testUpdateRouteProgress_ArrivedBranch_NoButton() throws Exception {
        // This hits the branch where btnEndTrip is NULL
        List<LatLng> points = new ArrayList<>(Arrays.asList(new LatLng(0,0), new LatLng(1,1)));
        setPrivateField(routeManager, "currentRoutePoints", points);
        routeManager.getRoutePolylines().add(mock(Polyline.class));

        // Ensure the layout has NO button with id btn_end_trip
        activity.setContentView(new View(activity));

        routeManager.updateRouteProgress(new LatLng(1,1));
        assertEquals("You have arrived!", ShadowToast.getTextOfLatestToast());
    }
}