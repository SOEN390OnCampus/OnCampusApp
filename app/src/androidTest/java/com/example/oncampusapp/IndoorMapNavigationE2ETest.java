package com.example.oncampusapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;



import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;



@RunWith(AndroidJUnit4.class)
public class IndoorMapNavigationE2ETest {
    /**
     * Pauses the test for 2 seconds so you can actually watch the UI steps.
     */
    private void stepDelay() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setUp() {
        // Initialize Intents globally so we can intercept and verify Activity transitions
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Intents after each test to prevent memory leaks and test cross-contamination
        Intents.release();
    }

    // ─── EXISTING TEST: BROWSE MAP VIA DIALOG ───────────────────────────────

    @Test
    public void testNavigateToIndoorMapViaDialog() {
        // Launch MapsActivity specifically for this test
        try (ActivityScenario<MapsActivity> scenario = ActivityScenario.launch(MapsActivity.class)) {
            // 1. Open the Building Selection Dialog
            onView(withId(R.id.btn_indoor_map)).perform(click());

            // 2. Ensure Dialog is displayed, then click the "H" building row
            onView(withId(R.id.rv_indoor_building)).check(matches(isDisplayed()));

            onView(withId(R.id.rv_indoor_building))
                    .perform(RecyclerViewActions.actionOnItem(
                            hasDescendant(withText("H")), click()
                    ));

            // 3. Click the Chip for floor "8"
            onView(withText("8")).perform(click());

            // 4. Verify that the IndoorMapActivity launched successfully
            onView(withId(R.id.indoor_map_view)).check(matches(isDisplayed()));
            onView(withId(R.id.et_indoor_search)).check(matches(isDisplayed()));
        }
    }

    // ─── NEW TEST: ISSUE #25 - SAME FLOOR ROUTING & DISPLAY ─────────────────

    @Test
    public void testShortestPath_SameFloor_DisplaysMap() throws InterruptedException {
        // Launch IndoorDirectionsActivity specifically for this test
        try (ActivityScenario<IndoorDirectionsActivity> scenario = ActivityScenario.launch(IndoorDirectionsActivity.class)) {

            // Wait briefly for the background thread to load the JSON rooms
            Thread.sleep(2000);

            // Enter two rooms on the same floor
            onView(withId(R.id.et_from_room)).perform(replaceText("H-820"), closeSoftKeyboard());
            onView(withId(R.id.et_to_room)).perform(replaceText("H-867"), closeSoftKeyboard());

            // Click Find Route
            onView(withId(R.id.btn_find_route)).perform(click());

            // Wait for Dijkstra's background thread to calculate the path
            Thread.sleep(2000);

            // Verify it successfully launched IndoorMapActivity
            intended(hasComponent(IndoorMapActivity.class.getName()));

            // Verify the custom map view is visible (Proxy for "path visually displayed")
            onView(withId(R.id.indoor_map_view)).check(matches(isDisplayed()));
        }
    }

    // ─── NEW TEST: ISSUE #28 - CROSS-FLOOR NAVIGATION & STEPPING ────────────

    @Test
    public void testCrossFloorNavigation_UpdatesFloorOnDoneClicked() throws InterruptedException {
        try (ActivityScenario<IndoorDirectionsActivity> scenario = ActivityScenario.launch(IndoorDirectionsActivity.class)) {

            Thread.sleep(2000);

            onView(withId(R.id.et_from_room)).perform(replaceText("LB-207"), closeSoftKeyboard());
            onView(withId(R.id.et_to_room)).perform(replaceText("LB-526"), closeSoftKeyboard());
            onView(withId(R.id.btn_find_route)).perform(click());

            Thread.sleep(1500);

            // 1. Verify starts on Floor 2
            onView(withId(R.id.tv_floor_title)).check(matches(withText(containsString("2"))));
            stepDelay();

            // 2. THE SPEED LOOP: Click confirm until we hit floor 5 or Arrived
            boolean reachedDestination = false;
            for (int i = 0; i < 50; i++) {
                try {
                    // Check for the specific UI text in your screenshot
                    onView(withId(R.id.tv_floor_title)).check(matches(withText(containsString("5"))));
                    onView(withId(R.id.tv_current_step_title)).check(matches(withText("You have arrived!")));

                    reachedDestination = true;
                    break; // EXIT THE LOOP IMMEDIATELY
                } catch (AssertionError e) {
                    // Not there yet, click the checkmark
                    onView(withId(R.id.btn_step_confirm)).perform(click());
                    // Small sleep to let the UI update its text
                    Thread.sleep(200);
                }
            }

            assertTrue("Failed to reach Floor 5 after 50 clicks", reachedDestination);
            stepDelay(); // Final look at the arrived state
        }
    }
}