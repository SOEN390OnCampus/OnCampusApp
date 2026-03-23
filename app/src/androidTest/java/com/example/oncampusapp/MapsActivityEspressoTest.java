package com.example.oncampusapp;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;

import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.idling.CountingIdlingResource;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.NoMatchingRootException;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.example.oncampusapp.location.FakeLocationProvider;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class MapsActivityEspressoTest {

    @Rule
    public ActivityScenarioRule<MapsActivity> activityRule =
            new ActivityScenarioRule<>(MapsActivity.class);

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(
                    // Location
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            );

    @Before
    public void grantNotificationPermission() {
        // Only attempt to grant notification permission if the device is running API 33 (Tiramisu) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(
                            InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName(),
                            Manifest.permission.POST_NOTIFICATIONS
                    );
        }
    }

    @Before
    public void setUp() {
        activityRule.getScenario().onActivity(activity -> {
            // Call your method here
            activity.setLocationProvider(new FakeLocationProvider(activity));
            activity.fusedLocationClient.setFakeLocation(45.5009, -73.5724); // Set a random default location
        });
    }

    @Before
    public void registerIdlingResource() {
        activityRule.getScenario().onActivity(activity -> {
            IdlingRegistry.getInstance().register(activity.mapIdlingResource);
        });
    }

    @After
    public void unregisterIdlingResource() {
        activityRule.getScenario().onActivity(activity -> {
            IdlingRegistry.getInstance().unregister(activity.mapIdlingResource);
        });
    }

    /**
     * Check if the map is displayed properly
     */
    private void checkMapDisplayed(){
        onView(withId(R.id.map))
            .check(matches(isDisplayed()));
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private void switchToSGWCampus() {
        onView(withId(R.id.btn_campus_switch)).check(matches(withText("SGW")));

        onView(withId(R.id.btn_campus_switch)).perform(click());

        onView(withId(R.id.btn_campus_switch)).check(matches(withText("LOY")));
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.getMap());
            LatLng cameraPos = activity.getMap().getCameraPosition().target;
            float zoom = activity.getMap().getCameraPosition().zoom;
            assertEquals(MapsActivity.SGW_COORDS.latitude, cameraPos.latitude, 0.001);
            assertEquals(MapsActivity.SGW_COORDS.longitude, cameraPos.longitude, 0.001);
            assertEquals(16f, zoom, 0.1f);
        });
    }

    private void switchToLoyolaCampus() {
        onView(withId(R.id.btn_campus_switch)).check(matches(withText("LOY")));

        onView(withId(R.id.btn_campus_switch)).perform(click());

        onView(withId(R.id.btn_campus_switch)).check(matches(withText("SGW")));
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.getMap());
            LatLng cameraPos = activity.getMap().getCameraPosition().target;
            float zoom = activity.getMap().getCameraPosition().zoom;
            assertEquals(MapsActivity.LOY_COORDS.latitude, cameraPos.latitude, 0.001);
            assertEquals(MapsActivity.LOY_COORDS.longitude, cameraPos.longitude, 0.001);
            assertEquals(16f, zoom, 0.1f);
        });
    }

    /**
     * Opens route picker by clicking the search bar.
     */
    private void openRoutePicker() {
        // Verify initial state: Search Bar is visible, Route Picker is hidden
        onView(withId(R.id.search_bar_container)).check(matches(isDisplayed()));
        onView(withId(R.id.route_picker_container)).check(matches(not(isDisplayed())));

        // Click Search Bar to open Route Picker
        onView(withId(R.id.search_bar_container)).perform(click());

        // Verify Route Picker is now visible and Search Bar is hidden
        onView(withId(R.id.route_picker_container)).check(matches(isDisplayed()));
        onView(withId(R.id.search_bar_container)).check(matches(not(isDisplayed())));
        onView(withId(R.id.et_start)).check(matches(hasFocus()));
    }

    /**
     * Closes route picker using back.
     */
    private void closeRoutePickerWithBack() {
        // Verify if the close route section is displayed
        onView(withId(R.id.close_search)).check(matches(isDisplayed()));

        // Close the keyboard if it is open
        Espresso.closeSoftKeyboard();

        // Perform click on the route picker close section
        onView(withId(R.id.close_search)).perform(click());

        // Verify it closed via the animation logic
        onView(withId(R.id.search_bar_container)).check(matches(isDisplayed()));
        onView(withId(R.id.route_picker_container)).check(matches(not(isDisplayed())));
    }

    /**
     * Tries to select the first dropdown suggestion if the popup is present and has items.
     * Returns true if a click happened, false if suggestions were not available.
     */
    private boolean trySelectFirstSuggestion() {
        try {
            onData(anything())
                    .inRoot(RootMatchers.isPlatformPopup()) // popup root [web:145]
                    .atPosition(0)
                    .perform(click()); // common AutoCompleteTextView pattern [web:6]
            return true;
        } catch (NoMatchingRootException | NoMatchingViewException | PerformException e) {
            return false;
        }
    }

    /**
     * Creates a click action on the map, on the device, based off a provided lat and long
     *
     * @param map The view map instance
     * @param targetLatLng location on the map to be clicked
     * @return click action
     */
    private ViewAction clickOnLatLng(GoogleMap map, LatLng targetLatLng, CountingIdlingResource idleResource) {
        CoordinatesProvider coordinatesProvider = view -> {
            Point screenPoint = map.getProjection().toScreenLocation(targetLatLng);
            int[] viewLocation = new int[2];
            view.getLocationOnScreen(viewLocation);
            return new float[]{viewLocation[0] + screenPoint.x, viewLocation[1] + screenPoint.y};
        };

        ViewAction clickAction = new GeneralClickAction(
            Tap.SINGLE, coordinatesProvider, Press.FINGER,
            InputDevice.SOURCE_UNKNOWN, MotionEvent.BUTTON_PRIMARY
        );

        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return clickAction.getConstraints();
            }

            @Override
            public String getDescription() {
                return "Clicking on LatLng and incrementing IdlingResource";
            }

            @Override
            public void perform(UiController uiController, View view) {
                clickAction.perform(uiController, view);
                idleResource.increment();
            }
        };
    }

    @Test
    public void testCampusToggle_SwitchesCampus() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("OnCampusPrefs", Context.MODE_PRIVATE);
        String savedCampus = prefs.getString("campus", "SGW");

        if (savedCampus.equals("SGW")) {
            switchToLoyolaCampus();
            switchToSGWCampus();
            switchToLoyolaCampus();

        } else {
            switchToSGWCampus();
            switchToLoyolaCampus();
            switchToSGWCampus();
        }
    }

    @Test
    public void testLocationButton_MovesCameraToCurrentPosition() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        sleep(5000);
        onView(withId(R.id.btn_location)).perform(click());
        sleep(1000);

        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.getMap());
            LatLng cameraPos = activity.getMap().getCameraPosition().target;
            float zoom = activity.getMap().getCameraPosition().zoom;

            activity.fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                try {
                    if (location != null) {
                        assertEquals(location.getLatitude(), cameraPos.latitude, 0.0001);
                        assertEquals(location.getLongitude(), cameraPos.longitude, 0.0001);
                        assertEquals(16f, zoom, 0.1f);
                    }
                } finally {
                    latch.countDown();
                }
            });
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed);
    }

    // -------------------------------------------------------------------------------------------
    // For US-2.1: Display search options and select buildings as start destination for navigation
    // -------------------------------------------------------------------------------------------

    @Test
    public void displayNavigationSearch() {
        checkMapDisplayed();
        openRoutePicker();

        // Type into et_start
        onView(withId(R.id.et_start))
            .perform(typeText("Building"), closeSoftKeyboard());

        // Check if suggestions are displayed and click the first one
        onData(anything())
            .inRoot(RootMatchers.isPlatformPopup())
            .atPosition(0)
            .perform(click());

        // Type into et_destination
        onView(withId(R.id.et_destination))
            .perform(typeText("john"), closeSoftKeyboard());

        // Click the first suggestion for the destination
        onData(anything())
            .inRoot(RootMatchers.isPlatformPopup())
            .atPosition(0)
            .perform(click());

        closeRoutePickerWithBack();

        openRoutePicker();

        // Check if the text in both et_start and et_destination was cleared after route picker was closed
        onView(withId(R.id.et_start))
            .check(matches(withText(isEmptyString())));

        onView(withId(R.id.et_destination))
            .check(matches(withText(isEmptyString())));

        // Click on start textview to set focus
        onView(withId(R.id.et_start))
            .perform(click());
        Espresso.closeSoftKeyboard();

        final GoogleMap[] mapInstance = new GoogleMap[1];
        final CountingIdlingResource[] mapIdleResource = new CountingIdlingResource[1];
        LatLng jmsbCoords = new LatLng(45.49547770602248, -73.57914911481745); // Coordinates to the center of the JMSB building
        LatLng vanierCoords = new LatLng(45.45886468564086, -73.63880278032387); // Coordinates to the center of the Vanier library

        // Get map instance and move to the JMSB building
        activityRule.getScenario().onActivity(activity -> {
            mapInstance[0] = activity.getMap();
            mapIdleResource[0] = activity.mapIdlingResource;

            activity.moveMapToLocation(jmsbCoords, 18f);
        });

        // Click on the JMSB building
        onView(withId(R.id.map))
            .perform(clickOnLatLng(mapInstance[0], jmsbCoords, mapIdleResource[0]));

        // Assert that text in start textview changed to JMSB
        onView(withId(R.id.et_start))
            .check(matches(withText("MB - John Molson School of Business")));

        // Click on destination textview to set focus
        onView(withId(R.id.et_destination))
            .perform(click());
        Espresso.closeSoftKeyboard();

        // Move to the vanier building
        activityRule.getScenario().onActivity(activity -> {
            activity.moveMapToLocation(vanierCoords, 18f);
        });

        // Click on the vanier building
        onView(withId(R.id.map))
            .perform(clickOnLatLng(mapInstance[0], vanierCoords, mapIdleResource[0]));

        // Assert that text in destination textview changed to vanier library
        onView(withId(R.id.et_destination))
            .check(matches(withText("Concordia Vanier Library")));

        // Test the device back press button click and verify if it closes the route search
        pressBack();
        onView(withId(R.id.search_bar_container)).check(matches(isDisplayed()));
        onView(withId(R.id.route_picker_container)).check(matches(not(isDisplayed())));
    }

    // ----------------------------------------
    // For US-2.2: Clicking on current location
    // ----------------------------------------

    // Location Tracking Service is inconsistent in calling the update function, need to trigger it directly
    @Test
    public void clickingOnCurrentLocation() {
        AtomicReference<Building> ref = new AtomicReference<>();
        AtomicReference<LocationTrackingService> serviceRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        activityRule.getScenario().onActivity(activity -> {
            Intent intent = new Intent(activity, LocationTrackingService.class);
            activity.bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    LocationTrackingService.LocalBinder localBinder =
                            (LocationTrackingService.LocalBinder) binder;
                    serviceRef.set(localBinder.getService());
                    latch.countDown(); // signal that service is connected
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {}
            }, Context.BIND_AUTO_CREATE);
        });

        // Wait for service to connect
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Service did not connect in time");
        }

        // Trigger the location update directly
        activityRule.getScenario().onActivity(activity -> {
            activity.fusedLocationClient.setFakeLocation(45.4973, -73.5789); // Set the location to Henry F Hall Building

            // Trigger the location update using the set location
            activity.fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            serviceRef.get().triggerLocationUpdate(
                                    new LatLng(location.getLatitude(), location.getLongitude())
                            );
                        }
                    });
        });

        sleep(1000);
        onView(withId(R.id.btn_location)).perform(click());
        sleep(1000);

        activityRule.getScenario().onActivity(activity ->
                ref.set(activity.buildingManager.getCurrentBuilding()));

        assertNotNull("Building not found", ref.get());
        assertEquals("H - Henry F. Hall Building", ref.get().getName());
    }

}
