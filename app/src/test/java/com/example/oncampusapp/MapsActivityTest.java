package com.example.oncampusapp;

import android.app.NotificationManager;
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
import org.robolectric.shadows.ShadowNotificationManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.example.oncampusapp.location.ILocationProvider;

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

    // ==========================================
    // Bottom Navigation Tests
    // ==========================================

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
        shadowActivity.clearNextStartedActivities();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNull("No new activity should start since we are already on Home", actualIntent);
        assertFalse("Activity should NOT finish", activity.isFinishing());
    }

    // ==========================================
    // Campus Switcher Tests
    // ==========================================

    @Test
    public void testSwitchCampus_updatesSharedPreferencesAndButtonText() throws Exception {
        com.google.android.gms.maps.GoogleMap mockMap = Mockito.mock(com.google.android.gms.maps.GoogleMap.class);
        com.google.android.gms.maps.UiSettings mockUiSettings = Mockito.mock(com.google.android.gms.maps.UiSettings.class);
        Mockito.when(mockMap.getUiSettings()).thenReturn(mockUiSettings);

        LocationPermissionManager mockPermManager = Mockito.mock(LocationPermissionManager.class);
        setPrivateField(activity, "locationPermManager", mockPermManager);

        GeoJsonMapLoader mockGeoLoader = Mockito.mock(GeoJsonMapLoader.class);
        setPrivateField(activity, "geoJsonMapLoader", mockGeoLoader);

        activity.onMapReady(mockMap);

        TextView btnSgwLoy = activity.findViewById(R.id.btn_campus_switch);
        SharedPreferences sharedPref = activity.getSharedPreferences("OnCampusPrefs", Context.MODE_PRIVATE);

        assertEquals("LOY", btnSgwLoy.getText().toString());

        btnSgwLoy.performClick();
        assertEquals("SGW", btnSgwLoy.getText().toString());
        assertEquals("LOY", sharedPref.getString("campus", ""));

        btnSgwLoy.performClick();
        assertEquals("LOY", btnSgwLoy.getText().toString());
        assertEquals("SGW", sharedPref.getString("campus", ""));
    }

    // ==========================================
    // Lifecycle & Utility Tests
    // ==========================================

    @Test
    public void testGetMap_ReturnsNullBeforeReady() {
        assertNull("Map should be null before onMapReady is called", activity.getMap());
    }

    @Test
    public void testSetLocationProvider_UpdatesInternalFields() throws Exception {
        ILocationProvider mockProvider = Mockito.mock(ILocationProvider.class);
        activity.setLocationProvider(mockProvider);

        Field fusedClientField = MapsActivity.class.getDeclaredField("fusedLocationClient");
        fusedClientField.setAccessible(true);
        assertEquals(mockProvider, fusedClientField.get(activity));
    }

    @Test
    public void testOnPause_StopsBannerManager() throws Exception {
        EventBannerManager mockBannerManager = Mockito.mock(EventBannerManager.class);
        setPrivateField(activity, "bannerManager", mockBannerManager);

        // Trigger lifecycle
        activity.onPause();

        Mockito.verify(mockBannerManager, Mockito.times(1)).stop();
    }

    @Test
    public void testReloadForPoiExit_StartsNewActivityAndFinishes() {
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        activity.reloadForPoiExit();

        Intent actualIntent = shadowActivity.getNextStartedActivity();
        assertNotNull(actualIntent);
        assertEquals(MapsActivity.class.getName(), actualIntent.getComponent().getClassName());
        assertTrue("Activity should finish to reload", activity.isFinishing());
    }

    // ==========================================
    // POI Intent Handling Tests
    // ==========================================

    @Test
    public void testPoiNavigationState_GettersAndSetters() {
        assertFalse(activity.isPoiNavigationActive());
        activity.setPoiNavigationActive(true);
        assertTrue(activity.isPoiNavigationActive());
    }

    @Test
    public void testHandleIncomingPoiIntent_ValidIntent_ExtractsData() throws Exception {
        Intent poiIntent = new Intent();
        poiIntent.putExtra("OPEN_POI_ROUTE", true);
        poiIntent.putExtra("POI_LAT", 45.497);
        poiIntent.putExtra("POI_LNG", -73.579);
        poiIntent.putExtra("POI_NAME", "Tim Hortons");

        activity.setIntent(poiIntent);
        invokePrivateMethod(activity, "handleIncomingPoiIntent");

        String name = (String) getPrivateField(activity, "pendingPoiName");
        assertEquals("Tim Hortons", name);
    }

    @Test
    public void testHandleIncomingPoiIntent_NullIntent_DoesNothing() throws Exception {
        activity.setIntent(null);
        invokePrivateMethod(activity, "handleIncomingPoiIntent");

        assertNull(getPrivateField(activity, "pendingPoiName"));
        assertNull(getPrivateField(activity, "pendingPoiLatLng"));
    }

    @Test
    public void testHandleIncomingPoiIntent_MissingBooleanFlag_DoesNothing() throws Exception {
        Intent poiIntent = new Intent();
        poiIntent.putExtra("POI_NAME", "Tim Hortons"); // Missing OPEN_POI_ROUTE

        activity.setIntent(poiIntent);
        invokePrivateMethod(activity, "handleIncomingPoiIntent");

        assertNull("Should abort if OPEN_POI_ROUTE is not true", getPrivateField(activity, "pendingPoiName"));
    }

    // ==========================================
    // Notification Handling Tests
    // ==========================================

    @Test
    public void testCancelNotificationIfPresent_ValidId_CancelsNotification() throws Exception {
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        ShadowNotificationManager shadowManager = Shadows.shadowOf(manager);

        // Setup: Add a dummy notification first so we can "cancel" it
        android.app.Notification notification = new android.app.Notification.Builder(activity, "GEOFENCE_CHANNEL")
                .setContentTitle("Test")
                .build();
        manager.notify(123, notification);

        // Verify it exists in the shadow first
        assertEquals(1, shadowManager.getAllNotifications().size());

        Intent intent = new Intent();
        intent.putExtra("notification_id", 123);

        // Action
        invokePrivateMethod(activity, "cancelNotificationIfPresent", Intent.class, intent);

        // Assertion: The list of active notifications should now be empty (or not contain ID 123)
        assertEquals("Notification should be removed from the manager", 0, shadowManager.getAllNotifications().size());
    }

    @Test
    public void testCancelNotificationIfPresent_NullIntent_DoesNothing() throws Exception {
        // Should not throw an exception
        invokePrivateMethod(activity, "cancelNotificationIfPresent", Intent.class, null);
    }

    @Test
    public void testCancelNotificationIfPresent_MissingIdExtra_DoesNothing() throws Exception {
        Intent intent = new Intent();
        intent.putExtra("some_other_extra", 123);

        // Should not throw an exception
        invokePrivateMethod(activity, "cancelNotificationIfPresent", Intent.class, intent);
    }

    @Test
    public void testOnNewIntent_SetsIntentAndHandlesNotifications() {
        Intent newIntent = new Intent();
        newIntent.putExtra("OPEN_DIRECTIONS", true);

        activity.onNewIntent(newIntent);

        assertEquals("Intent should be updated", newIntent, activity.getIntent());
    }

    @Test
    public void testHandleNotificationDirectionsIntent_NullIntent_Aborts() throws Exception {
        invokePrivateMethod(activity, "handleNotificationDirectionsIntent", Intent.class, null);

        boolean isPending = (Boolean) getPrivateField(activity, "pendingNotificationDirections");
        assertFalse("Should remain false if intent is null", isPending);
    }

    @Test
    public void testHandleNotificationDirectionsIntent_MissingFlag_Aborts() throws Exception {
        Intent intent = new Intent();
        invokePrivateMethod(activity, "handleNotificationDirectionsIntent", Intent.class, intent);

        boolean isPending = (Boolean) getPrivateField(activity, "pendingNotificationDirections");
        assertFalse("Should remain false if flag is missing", isPending);
    }

    // ==========================================
    // Cross-Building State Machine Tests
    // ==========================================

    @Test
    public void testCrossBuildingState_initializesCorrectly() {
        assertFalse(activity.hasPendingFinalIndoorAfterOutdoor());
    }

    @Test
    public void testTryLaunchPendingFinalIndoorRoute_FailsWhenFlagIsFalse() {
        activity.setPendingCrossBuilding(Mockito.mock(IndoorNode.class), Mockito.mock(IndoorNode.class), "H", "MB");

        // pendingFinalIndoorAfterOutdoor is false by default
        boolean didLaunch = activity.tryLaunchPendingFinalIndoorRoute();

        assertFalse("Should abort if outdoor portion is not finished", didLaunch);
    }

    @Test
    public void testTryLaunchPendingFinalIndoorRoute_NullDoor_ClearsDataAndFails() throws Exception {
        // Set up invalid state (null door)
        activity.setPendingCrossBuilding(null, Mockito.mock(IndoorNode.class), "H", "MB");
        setPrivateField(activity, "pendingFinalIndoorAfterOutdoor", true);

        boolean didLaunch = activity.tryLaunchPendingFinalIndoorRoute();

        assertFalse("Should fail due to missing data", didLaunch);
        assertFalse("Data should be cleared", activity.hasPendingFinalIndoorAfterOutdoor());
    }

    @Test
    public void testTryLaunchPendingFinalIndoorRoute_EmptyBuildingString_ClearsDataAndFails() throws Exception {
        activity.setPendingCrossBuilding(Mockito.mock(IndoorNode.class), Mockito.mock(IndoorNode.class), "H", "");
        setPrivateField(activity, "pendingFinalIndoorAfterOutdoor", true);

        boolean didLaunch = activity.tryLaunchPendingFinalIndoorRoute();

        assertFalse("Should fail due to empty building string", didLaunch);
        assertNull("Building string should be reset", getPrivateField(activity, "pendingCrossToBuilding"));
    }

    @Test
    public void testClearPendingCrossBuildingData_ResetsAllFields() throws Exception {
        activity.setPendingCrossBuilding(Mockito.mock(IndoorNode.class), Mockito.mock(IndoorNode.class), "H", "MB");
        setPrivateField(activity, "pendingFinalIndoorAfterOutdoor", true);

        invokePrivateMethod(activity, "clearPendingCrossBuildingData");

        assertFalse(activity.hasPendingFinalIndoorAfterOutdoor());
        assertFalse((Boolean) getPrivateField(activity, "pendingCrossBuildingOutdoor"));
        assertNull(getPrivateField(activity, "pendingCrossFromBuilding"));
        assertNull(getPrivateField(activity, "pendingCrossToDoor"));
    }

    // ==========================================
    // Helper Reflection Methods
    // ==========================================

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void invokePrivateMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    private void invokePrivateMethod(Object target, String methodName, Class<?> paramType, Object paramValue) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, paramType);
        method.setAccessible(true);
        method.invoke(target, paramValue);
    }
}