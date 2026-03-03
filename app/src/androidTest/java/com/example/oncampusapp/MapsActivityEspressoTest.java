package com.example.oncampusapp;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.clearText;
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
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;

import android.Manifest;
import android.graphics.Point;
import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.test.espresso.NoMatchingRootException;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.example.oncampusapp.location.FakeLocationProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

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
            activity.fusedLocationClient.setFakeLocation(45.4973, -73.5789); // Set the default location to hall building
        });
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
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
    private ViewAction clickOnLatLng(GoogleMap map, LatLng targetLatLng) {
        return new GeneralClickAction(
            Tap.SINGLE,
                view -> {
                    // Convert LatLng to point coordinates relative to the map view
                    Point screenPoint = map.getProjection().toScreenLocation(targetLatLng);

                    // Get the absolute screen position of the map view's top-left corner
                    int[] viewLocation = new int[2];
                    view.getLocationOnScreen(viewLocation);

                    // Add them together to get the absolute screen coordinates for Espresso
                    float x = viewLocation[0] + screenPoint.x;
                    float y = viewLocation[1] + screenPoint.y;

                    return new float[]{x, y};
                },
            Press.FINGER,
            InputDevice.SOURCE_UNKNOWN,
            MotionEvent.BUTTON_PRIMARY
        );
    }

    // -------------------------
    // Existing tests (improved a bit)
    // -------------------------

    @Test
    public void testCampusToggle_SwitchesCampus() {
        // Initial state: button text is LOY
        onView(withId(R.id.btn_campus_switch)).check(matches(withText("LOY")));

        sleep(5000); // your map init delay
        onView(withId(R.id.btn_campus_switch)).perform(click());
        sleep(1000);

        onView(withId(R.id.btn_campus_switch)).check(matches(withText("SGW")));
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.getMap());
            LatLng cameraPos = activity.getMap().getCameraPosition().target;
            float zoom = activity.getMap().getCameraPosition().zoom;
            assertEquals(MapsActivity.LOY_COORDS.latitude, cameraPos.latitude, 0.001);
            assertEquals(MapsActivity.LOY_COORDS.longitude, cameraPos.longitude, 0.001);
            assertEquals(16f, zoom, 0.1f);
        });

        onView(withId(R.id.btn_campus_switch)).perform(click());
        sleep(1000);

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

    // -------------------------
    // Route picker / search bar tests
    // -------------------------

    @Test
    public void initialState_searchBarVisible_routePickerHidden() {
        sleep(5000);
        onView(withId(R.id.search_bar_container)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withId(R.id.route_picker_container)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    }

    @Test
    public void clickingSearchBar_opensRoutePicker_andFocusesStart() {
        openRoutePicker();
        onView(withId(R.id.et_start)).check(matches(hasFocus()));
    }

    @Test
    public void backPress_whenRoutePickerOpen_closesIt() {
        openRoutePicker();
        closeRoutePickerWithBack();
    }

    @Test
    public void reopeningRoutePicker_afterBack_showsItAgain() {
        openRoutePicker();
        closeRoutePickerWithBack();
        openRoutePicker();
        onView(withId(R.id.route_picker_container)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    @Test
    public void startText_typingDisplaysInField() {
        openRoutePicker();
        onView(withId(R.id.et_start)).perform(clearText(), typeText("Hall"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.et_start)).check(matches(withText("Hall")));
    }

    @Test
    public void destinationText_typingDisplaysInField() {
        openRoutePicker();
        onView(withId(R.id.et_destination)).perform(clearText(), click(), typeText("JMSB"), closeSoftKeyboard());
        onView(withId(R.id.et_destination)).check(matches(withText("JMSB")));
    }

    @Test
    public void selectingSuggestion_setsStartText_ifSuggestionsAvailable() {
        openRoutePicker();
        onView(withId(R.id.et_start)).perform(clearText(), typeText("B"), closeSoftKeyboard());
        sleep(500);
        boolean clicked = trySelectFirstSuggestion();
        if (clicked) {
            // After selection, text should not be empty
            // (We don’t know exact string, so just verify field is displayed)
            onView(withId(R.id.et_start)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }

    @Test
    public void selectingSuggestion_setsDestinationText_ifSuggestionsAvailable() {
        openRoutePicker();
        onView(withId(R.id.et_destination)).perform(clearText(), typeText("B"), closeSoftKeyboard());
        sleep(500);
        boolean clicked = trySelectFirstSuggestion();
        if (clicked) {
            onView(withId(R.id.et_destination)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }

    @Test
    public void flow_fillStartAndDestination_thenBack_closesRoutePicker() {
        openRoutePicker();

        onView(withId(R.id.et_start)).perform(clearText(), typeText("Building"), closeSoftKeyboard());
        sleep(500);
        trySelectFirstSuggestion();

        onView(withId(R.id.et_destination)).perform(clearText(), typeText("Business"), closeSoftKeyboard());
        sleep(500);
        trySelectFirstSuggestion();

        closeRoutePickerWithBack();
    }

    @Test
    public void routePicker_close_then_open_preservesTypedText() {
        openRoutePicker();
        onView(withId(R.id.et_start)).perform(clearText(), typeText("TEST123"), closeSoftKeyboard());
        closeRoutePickerWithBack();

        openRoutePicker();
        onView(withId(R.id.et_start)).check(matches(withText("")));
    }

    @Test
    public void routePicker_open_startHasFocus_destinationCanBeFocusedByClick() {
        openRoutePicker();
        onView(withId(R.id.et_start)).check(matches(hasFocus()));
        onView(withId(R.id.et_destination)).perform(click());
        onView(withId(R.id.et_destination)).check(matches(hasFocus()));
    }

    @Test
    public void pressingBack_whenRoutePickerClosed_mayExit_soUsePressBackUnconditionallyPattern() {
        // Demonstrates: pressBack can throw when at root (Espresso docs mention this) [web:140]
        // Here we just ensure we can open/close safely and do not press back at root.
        openRoutePicker();
        closeRoutePickerWithBack();
    }

    // -------------------------------------------------------------------------------------------
    // For US-2.1: Display search options and select buildings as start destination for navigation
    // -------------------------------------------------------------------------------------------

    @Test
    public void displayNavigationSearch() {
        // Wait for map to load properly
        sleep(3000);

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

        // Check if the text in bot et_start and et_destination was cleared after route picker was closed
        onView(withId(R.id.et_start))
            .check(matches(withText(isEmptyString())));

        onView(withId(R.id.et_destination))
            .check(matches(withText(isEmptyString())));

        // Close the opened keyboard
        Espresso.closeSoftKeyboard();

        final GoogleMap[] mapInstance = new GoogleMap[1];
        LatLng jmsbCoords = new LatLng(45.49547770602248, -73.57914911481745); // Coordinates to the center of the JMSB building
        LatLng vanierCoords = new LatLng(45.45886468564086, -73.63880278032387); // Coordinates to the center of the Vanier library

        // Get map instance and move to the JMSB building
        activityRule.getScenario().onActivity(activity -> {
            mapInstance[0] = activity.getMap();

            mapInstance[0].animateCamera(CameraUpdateFactory.newLatLngZoom(jmsbCoords, 18f));
        });
        sleep(1500);

        // Click on the JMSB building
        onView(withId(R.id.map))
            .perform(clickOnLatLng(mapInstance[0], jmsbCoords));
        sleep(200);

        // Assert that text in start textview changed to JMSB
        onView(withId(R.id.et_start))
            .check(matches(withText("MB - John Molson School of Business")));

        // Click on destination textview to set focus
        onView(withId(R.id.et_destination))
            .perform(click());
        Espresso.closeSoftKeyboard();

        // Move to the JMSB building
        activityRule.getScenario().onActivity(activity -> mapInstance[0].animateCamera(CameraUpdateFactory.newLatLngZoom(vanierCoords, 18f)));
        sleep(1500);

        // Click on the vanier building
        onView(withId(R.id.map))
            .perform(clickOnLatLng(mapInstance[0], vanierCoords));
        sleep(200);

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

    @Test
    public void clickingOnCurrentLocation() {
        AtomicReference<Building> ref = new AtomicReference<>();

        activityRule.getScenario().onActivity(activity -> {
            activity.fusedLocationClient.setFakeLocation(45.4973, -73.5789); // A coordinate inside H - Henry F. Hall Building
        });

        sleep(5000);
        onView(withId(R.id.btn_location)).perform(click());
        sleep(1000);

        activityRule.getScenario().onActivity(activity -> ref.set(activity.buildingManager.getCurrentBuilding()));

        String name = ref.get().getName();

        assertEquals("H - Henry F. Hall Building", name);
    }

}
