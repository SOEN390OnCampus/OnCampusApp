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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

        // FIX 1: Clear any intents that were automatically fired during onCreate()
        // (e.g., Permission request intents) so we have a clean slate to test navigation.
        shadowActivity.clearNextStartedActivities();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertTrue("No new activity should start since we are already on Home", actualIntent == null);
        assertFalse("Activity should NOT finish", activity.isFinishing());
    }

    // --- Campus Switcher (SharedPreferences) Tests ---

    @Test
    public void testSwitchCampus_updatesSharedPreferencesAndButtonText() throws Exception {
        com.google.android.gms.maps.GoogleMap mockMap = Mockito.mock(com.google.android.gms.maps.GoogleMap.class);
        com.google.android.gms.maps.UiSettings mockUiSettings = Mockito.mock(com.google.android.gms.maps.UiSettings.class);
        Mockito.when(mockMap.getUiSettings()).thenReturn(mockUiSettings);

        // FIX 1: Mock LocationPermissionManager to avoid CameraUpdateFactory native crash
        LocationPermissionManager mockPermManager = Mockito.mock(LocationPermissionManager.class);
        java.lang.reflect.Field permManagerField = MapsActivity.class.getDeclaredField("locationPermManager");
        permManagerField.setAccessible(true);
        permManagerField.set(activity, mockPermManager);

        // FIX 2: Mock GeoJsonMapLoader to avoid BitmapDescriptorFactory native crash
        GeoJsonMapLoader mockGeoLoader = Mockito.mock(GeoJsonMapLoader.class);
        java.lang.reflect.Field geoLoaderField = MapsActivity.class.getDeclaredField("geoJsonMapLoader");
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
}