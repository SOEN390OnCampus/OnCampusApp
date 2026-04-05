package com.example.oncampusapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import com.example.oncampusapp.location.ILocationProvider;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowNotificationManager;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33) // Required because POST_NOTIFICATIONS permission requires API 33+
public class LocationTrackingServiceTest {

    private ServiceController<LocationTrackingService> controller;
    private LocationTrackingService service;
    private ILocationProvider mockLocationProvider;

    @Before
    public void setUp() {
        // Grab the Application context (used for both permissions and injection)
        OnCampusApplication app = ApplicationProvider.getApplicationContext();

        // 1. Grant the POST_NOTIFICATIONS permission so notifications don't get blocked
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);

        // 2. Mock the ILocationProvider and inject it into the Application class
        mockLocationProvider = Mockito.mock(ILocationProvider.class);
        app.setLocationProvider(mockLocationProvider);

        // 3. Reset the static buildings map so tests don't bleed into each other
        MapsActivity.buildingsMap = new HashMap<>();

        // 4. Build and start the Service
        controller = Robolectric.buildService(LocationTrackingService.class);
        service = controller.create().startCommand(0, 0).get();
    }

    @After
    public void tearDown() {
        // Clean up the service and static variables
        controller.destroy();
        MapsActivity.buildingsMap.clear();
    }

    // --- Binder and Lifecycle Tests ---

    @Test
    public void testOnBind_returnsLocalBinderWithServiceInstance() {
        Intent intent = new Intent();
        IBinder binder = service.onBind(intent);

        assertNotNull("Binder should not be null", binder);
        assertTrue("Binder should be instance of LocalBinder", binder instanceof LocationTrackingService.LocalBinder);

        LocationTrackingService boundService = ((LocationTrackingService.LocalBinder) binder).getService();
        assertEquals("Bound service should be the same instance", service, boundService);
    }

    @Test
    public void testOnCreate_requestsLocationUpdates() {
        // Verify that the service requested location updates from our injected mock
        Mockito.verify(mockLocationProvider).requestLocationUpdates(
                Mockito.any(LocationRequest.class),
                Mockito.any(LocationCallback.class),
                Mockito.eq(Looper.getMainLooper())
        );
    }

    @Test
    public void testOnDestroy_removesLocationUpdates() {
        // We need to capture the LocationCallback that was created in onCreate()
        ArgumentCaptor<LocationCallback> captor = ArgumentCaptor.forClass(LocationCallback.class);
        Mockito.verify(mockLocationProvider).requestLocationUpdates(
                Mockito.any(), captor.capture(), Mockito.any());

        LocationCallback registeredCallback = captor.getValue();

        // Action: Destroy the service
        controller.destroy();

        // Assertion: Verify it cleaned up the specific callback it registered
        Mockito.verify(mockLocationProvider).removeLocationUpdates(registeredCallback);
    }

    // --- Geofencing & Notification Tests ---

    @Test
    public void testGeofence_enteringBuilding_sendsNotification() {
        // Setup: Create a polygon from (0,0) to (2,2)
        List<LatLng> squarePolygon = Arrays.asList(
                new LatLng(0.0, 0.0),
                new LatLng(0.0, 2.0),
                new LatLng(2.0, 2.0),
                new LatLng(2.0, 0.0)
        );

        // Use the required constructor: String, String, List<LatLng>
        Building testBuilding = new Building("H", "Hall Building", squarePolygon);
        testBuilding.setCurrentlyInside(false); // User starts OUTSIDE

        MapsActivity.buildingsMap.put("H", testBuilding);

        // Action: Trigger a location update that is INSIDE the square (1,1)
        LatLng insideLocation = new LatLng(1.0, 1.0);
        service.triggerLocationUpdate(insideLocation);

        // Assertion 1: State flag should be updated
        assertTrue("Building state should update to inside", testBuilding.isCurrentlyInside());

        // Assertion 2: Verify the notification was sent
        assertNotificationSentWithText("You have entered Hall Building");
    }

    @Test
    public void testGeofence_exitingBuilding_sendsNotification() {
        // Setup: Create the polygon
        List<LatLng> squarePolygon = Arrays.asList(
                new LatLng(0.0, 0.0),
                new LatLng(0.0, 2.0),
                new LatLng(2.0, 2.0),
                new LatLng(2.0, 0.0)
        );

        // Use the constructor and set user to INSIDE
        Building testBuilding = new Building("MB", "MB Building", squarePolygon);
        testBuilding.setCurrentlyInside(true);

        MapsActivity.buildingsMap.put("MB", testBuilding);

        // Action: Trigger a location update that is OUTSIDE the square (5,5)
        LatLng outsideLocation = new LatLng(5.0, 5.0);
        service.triggerLocationUpdate(outsideLocation);

        // Assertion 1: State flag should be updated
        assertFalse("Building state should update to outside", testBuilding.isCurrentlyInside());

        // Assertion 2: Verify the notification was sent
        assertNotificationSentWithText("You have exited MB Building");
    }

    @Test
    public void testGeofence_stayingInside_doesNotSpamNotifications() {
        // Setup: Create the polygon
        List<LatLng> squarePolygon = Arrays.asList(
                new LatLng(0.0, 0.0),
                new LatLng(0.0, 2.0),
                new LatLng(2.0, 2.0),
                new LatLng(2.0, 0.0)
        );

        // User is ALREADY inside
        Building testBuilding = new Building("CC", "CC Building", squarePolygon);
        testBuilding.setCurrentlyInside(true);

        MapsActivity.buildingsMap.put("CC", testBuilding);

        // Action: Trigger another location update that is still INSIDE the square (1,1)
        LatLng insideLocation = new LatLng(1.0, 1.0);
        service.triggerLocationUpdate(insideLocation);

        // Assertion: Ensure no new push notifications were generated
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider
                .getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        ShadowNotificationManager shadowNM = Shadows.shadowOf(notificationManager);

        assertEquals("Should only have the Foreground notification, no entry/exit notifications",
                1, shadowNM.getAllNotifications().size());
    }
    // --- Helper Method ---

    private void assertNotificationSentWithText(String expectedText) {
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider
                .getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        ShadowNotificationManager shadowNM = Shadows.shadowOf(notificationManager);

        boolean found = false;
        for (Notification notification : shadowNM.getAllNotifications()) {
            CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            if (text != null && text.toString().equals(expectedText)) {
                found = true;
                break;
            }
        }
        assertTrue("Expected notification with text '" + expectedText + "' was not found", found);
    }
}