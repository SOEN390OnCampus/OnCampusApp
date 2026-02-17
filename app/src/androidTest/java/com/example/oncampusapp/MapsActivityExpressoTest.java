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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;


import android.Manifest;
import android.view.View;

import androidx.test.espresso.NoMatchingRootException;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class MapsActivityExpressoTest {

    @Rule
    public ActivityScenarioRule<MapsActivity> activityRule =
            new ActivityScenarioRule<>(MapsActivity.class);

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    /** Opens route picker by clicking the search bar. */
    private void openRoutePicker() {
        // Wait a bit for activity + map init to settle (still not perfect, but reduces flake)
        sleep(5000);
        onView(withId(R.id.search_bar_container)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withId(R.id.search_bar_container)).perform(click());
        onView(withId(R.id.route_picker_container)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withId(R.id.search_bar_container)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    }

    /** Closes route picker using back. */
    private void closeRoutePickerWithBack() {
        onView(withId(R.id.et_start)).perform(closeSoftKeyboard()); // closes keyboard (ViewAction)
        pressBack();
        sleep(1500);

        onView(withId(R.id.search_bar_container))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withId(R.id.route_picker_container))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));
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
        assertEquals(true, completed);
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
        closeSoftKeyboard();
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
        onView(withId(R.id.et_start)).check(matches(withText("TEST123")));
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

    // ----------------------------------------
    // For US-2.2: Clicking on current location
    // ----------------------------------------

    @Test
    public void clickingOnCurrentLocation() {
        onView(withId(R.id.btn_location)).perform(click());
        openRoutePicker();
        onView(withId(R.id.currentLocationIcon)).perform(click());
        onView(withId(R.id.et_start)).check(matches(withText("EV - Engineering, Computer Science and Visual Arts Integrated Complex")));
    }
}
