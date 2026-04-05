package com.example.oncampusapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class MapsActivityTest {

    private MapsActivity activity;

    @Before
    public void setUp() {
        // Build the activity. We stop at create() because calling resume()
        // triggers complex Google Maps API calls and permissions checks.
        activity = Robolectric.buildActivity(MapsActivity.class)
                .create()
                .get();
    }

    @Test
    public void testActivity_isCreatedSuccessfully() {
        assertNotNull("Activity should be successfully created", activity);
    }

    // --- Bottom Navigation Tests ---

    @Test
    public void testBottomNav_clickSettings_startsSettingsActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_settings);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent expectedIntent = new Intent(activity, SettingsActivity.class);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Settings intent should be fired", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        assertTrue("Activity should finish to prevent backstack buildup", activity.isFinishing());
    }

    @Test
    public void testBottomNav_clickAccount_startsAuthActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_account);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent expectedIntent = new Intent(activity, GoogleCalendarAuthActivity.class);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Auth intent should be fired", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        assertTrue("Activity should finish to prevent backstack buildup", activity.isFinishing());
    }

    @Test
    public void testBottomNav_clickHome_doesNotStartNewActivity() {
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        // Clear any intents that were automatically fired during onCreate()
        shadowActivity.clearNextStartedActivities();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNull("No new activity should start since we are already on Home", actualIntent);
        assertFalse("Activity should NOT finish", activity.isFinishing());
    }

    // --- Campus Switcher (SharedPreferences) Tests ---

    @Test
    public void testSwitchCampus_updatesSharedPreferencesAndButtonText() throws Exception {
        com.google.android.gms.maps.GoogleMap mockMap = Mockito.mock(com.google.android.gms.maps.GoogleMap.class);
        com.google.android.gms.maps.UiSettings mockUiSettings = Mockito.mock(com.google.android.gms.maps.UiSettings.class);
        Mockito.when(mockMap.getUiSettings()).thenReturn(mockUiSettings);

        // Mock LocationPermissionManager to avoid CameraUpdateFactory native crash
        LocationPermissionManager mockPermManager = Mockito.mock(LocationPermissionManager.class);
        Field permManagerField = MapsActivity.class.getDeclaredField("locationPermManager");
        permManagerField.setAccessible(true);
        permManagerField.set(activity, mockPermManager);

        // Mock GeoJsonMapLoader to avoid BitmapDescriptorFactory native crash
        GeoJsonMapLoader mockGeoLoader = Mockito.mock(GeoJsonMapLoader.class);
        Field geoLoaderField = MapsActivity.class.getDeclaredField("geoJsonMapLoader");
        geoLoaderField.setAccessible(true);
        geoLoaderField.set(activity, mockGeoLoader);

        // Now we can safely call onMapReady!
        activity.onMapReady(mockMap);

        TextView btnSgwLoy = activity.findViewById(R.id.btn_campus_switch);
        SharedPreferences sharedPref = activity.getSharedPreferences("OnCampusPrefs", Context.MODE_PRIVATE);

        // 1. Initial state check (Should default to SGW logic in onMapReady)
        assertEquals("LOY", btnSgwLoy.getText().toString());

        // Action 1: Click to switch to LOY
        btnSgwLoy.performClick();

        // Assertion 1
        assertEquals("SGW", btnSgwLoy.getText().toString());
        assertEquals("LOY", sharedPref.getString("campus", ""));

        // Action 2: Click to switch back to SGW
        btnSgwLoy.performClick();

        // Assertion 2
        assertEquals("LOY", btnSgwLoy.getText().toString());
        assertEquals("SGW", sharedPref.getString("campus", ""));
    }

    // --- POI and Navigation State Tests ---

    @Test
    public void testPoiNavigationState_GettersAndSetters() {
        assertFalse("POI navigation should be false by default", activity.isPoiNavigationActive());

        activity.setPoiNavigationActive(true);
        assertTrue("POI navigation should be true after setting", activity.isPoiNavigationActive());
    }

    @Test
    public void testHandleIncomingPoiIntent_ExtractsDataCorrectly() throws Exception {
        // Create an intent with POI extras
        Intent poiIntent = new Intent();
        poiIntent.putExtra("OPEN_POI_ROUTE", true);
        poiIntent.putExtra("POI_LAT", 45.497);
        poiIntent.putExtra("POI_LNG", -73.579);
        poiIntent.putExtra("POI_NAME", "Tim Hortons");

        // Inject the intent into the activity
        activity.setIntent(poiIntent);

        // Trigger the private method via reflection
        Method handlePoiIntent = MapsActivity.class.getDeclaredMethod("handleIncomingPoiIntent");
        handlePoiIntent.setAccessible(true);
        handlePoiIntent.invoke(activity);

        // Verify the private fields were set correctly
        Field pendingNameField = MapsActivity.class.getDeclaredField("pendingPoiName");
        pendingNameField.setAccessible(true);
        String name = (String) pendingNameField.get(activity);

        assertEquals("Tim Hortons", name);
    }

    // --- Cross-Building State Machine Tests ---

    @Test
    public void testCrossBuildingState_initializesCorrectly() {
        // Assertion: By default, there should be no pending final indoor route
        assertFalse("Should not have pending indoor route initially",
                activity.hasPendingFinalIndoorAfterOutdoor());
    }

    @Test
    public void testTryLaunchPendingFinalIndoorRoute_failsGracefullyWhenNotReady() {
        // Setup: Inject mock nodes
        IndoorNode mockDoor = Mockito.mock(IndoorNode.class);
        IndoorNode mockRoom = Mockito.mock(IndoorNode.class);

        // Action: Set the pending state, but DO NOT simulate the outdoor route completion
        // (which means pendingFinalIndoorAfterOutdoor is still false)
        activity.setPendingCrossBuilding(mockDoor, mockRoom, "H", "MB");

        // Action: Try to launch the final route
        boolean didLaunch = activity.tryLaunchPendingFinalIndoorRoute();

        // Assertion: It should immediately return false because the outdoor portion isn't done
        assertFalse("Should abort launch if outdoor portion is not marked as completed", didLaunch);
    }

    @Test
    public void testClearPendingCrossBuildingData_ResetsAllFields() throws Exception {
        // Setup mock data
        activity.setPendingCrossBuilding(
                Mockito.mock(IndoorNode.class),
                Mockito.mock(IndoorNode.class),
                "H", "MB");

        // Manually set the final outdoor flag
        Field finalIndoorField = MapsActivity.class.getDeclaredField("pendingFinalIndoorAfterOutdoor");
        finalIndoorField.setAccessible(true);
        finalIndoorField.set(activity, true);

        // Call the clear method via reflection
        Method clearMethod = MapsActivity.class.getDeclaredMethod("clearPendingCrossBuildingData");
        clearMethod.setAccessible(true);
        clearMethod.invoke(activity);

        // Verify all fields are reset
        assertFalse(activity.hasPendingFinalIndoorAfterOutdoor());

        Field pendingOutdoorField = MapsActivity.class.getDeclaredField("pendingCrossBuildingOutdoor");
        pendingOutdoorField.setAccessible(true);
        assertFalse((Boolean) pendingOutdoorField.get(activity));

        Field fromBuildingField = MapsActivity.class.getDeclaredField("pendingCrossFromBuilding");
        fromBuildingField.setAccessible(true);
        assertNull(fromBuildingField.get(activity));
    }

    // --- Notification Handling Tests ---

    @Test
    public void testHandleNotificationDirectionsIntent_FlagsCorrectly() throws Exception {
        Intent notificationIntent = new Intent();
        notificationIntent.putExtra("OPEN_DIRECTIONS", true);

        Method handleNotification = MapsActivity.class.getDeclaredMethod("handleNotificationDirectionsIntent", Intent.class);
        handleNotification.setAccessible(true);

        // We expect it to try generating directions, which will safely abort inside the test
        // because the eventsJson is null, but we can verify the pending flag was set.
        handleNotification.invoke(activity, notificationIntent);

        Field pendingDirectionsField = MapsActivity.class.getDeclaredField("pendingNotificationDirections");
        pendingDirectionsField.setAccessible(true);

        // It gets set to true, then tryGenerateDirectionsFromNotification resets it to false if it aborts
        // Because eventsJson is null in the test, it aborts but DOES NOT reset the flag to false
        // (The flag is only reset to false if it successfully launches the route)
        assertTrue((Boolean) pendingDirectionsField.get(activity));
    }
}