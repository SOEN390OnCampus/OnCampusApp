package com.example.oncampusapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;

import com.example.oncampusapp.util.EspressoIdlingResource;

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

    @Before
    public void setUp() {
        // Initialize Intents globally
        Intents.init();

        // Register the IdlingResource before each test starts so Espresso knows to listen to it
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource);
    }

    @After
    public void tearDown() {
        // Release Intents after each test
        Intents.release();

        // Unregister to prevent memory leaks between different test classes
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource);
    }

    // ─── EXISTING TEST: BROWSE MAP VIA DIALOG ───────────────────────────────

    @Test
    public void testNavigateToIndoorMapViaDialog() {
        try (ActivityScenario<MapsActivity> scenario = ActivityScenario.launch(MapsActivity.class)) {
            onView(withId(R.id.btn_indoor_map)).perform(click());
            onView(withId(R.id.rv_indoor_building)).check(matches(isDisplayed()));

            onView(withId(R.id.rv_indoor_building))
                    .perform(RecyclerViewActions.actionOnItem(
                            hasDescendant(withText("H")), click()
                    ));

            onView(withText("8")).perform(click());

            onView(withId(R.id.indoor_map_view)).check(matches(isDisplayed()));
            onView(withId(R.id.et_indoor_search)).check(matches(isDisplayed()));
        }
    }

    // ─── NEW TEST: ISSUE #25 - SAME FLOOR ROUTING & DISPLAY ─────────────────

    @Test
    public void testShortestPath_SameFloor_DisplaysMap() { // Removed throws InterruptedException
        try (ActivityScenario<IndoorDirectionsActivity> scenario = ActivityScenario.launch(IndoorDirectionsActivity.class)) {

            // Removed Thread.sleep(2000). Espresso will wait automatically if JSON parsing increments the IdlingResource.

            onView(withId(R.id.et_from_room)).perform(replaceText("H-820"), closeSoftKeyboard());
            onView(withId(R.id.et_to_room)).perform(replaceText("H-867"), closeSoftKeyboard());
            onView(withId(R.id.btn_find_route)).perform(click());

            // Removed Thread.sleep(2000). Espresso will wait automatically for Dijkstra to finish.

            intended(hasComponent(IndoorMapActivity.class.getName()));
            onView(withId(R.id.indoor_map_view)).check(matches(isDisplayed()));
        }
    }

    // ─── NEW TEST: ISSUE #28 - CROSS-FLOOR NAVIGATION & STEPPING ────────────

    @Test
    public void testCrossFloorNavigation_UpdatesFloorOnDoneClicked() { // Removed throws InterruptedException
        try (ActivityScenario<IndoorDirectionsActivity> scenario = ActivityScenario.launch(IndoorDirectionsActivity.class)) {

            // Removed Thread.sleep(2000)

            onView(withId(R.id.et_from_room)).perform(replaceText("LB-207"), closeSoftKeyboard());
            onView(withId(R.id.et_to_room)).perform(replaceText("LB-526"), closeSoftKeyboard());
            onView(withId(R.id.btn_find_route)).perform(click());

            // Removed Thread.sleep(1500)

            onView(withId(R.id.tv_floor_title)).check(matches(withText(containsString("2"))));

            // Removed stepDelay()

            boolean reachedDestination = false;
            for (int i = 0; i < 50; i++) {
                try {
                    onView(withId(R.id.tv_floor_title)).check(matches(withText(containsString("5"))));
                    onView(withId(R.id.tv_current_step_title)).check(matches(withText("You have arrived!")));

                    reachedDestination = true;
                    break;
                } catch (AssertionError e) {
                    onView(withId(R.id.btn_step_confirm)).perform(click());
                    // Removed Thread.sleep(200). Espresso automatically waits for the UI to be idle after a click.
                }
            }

            assertTrue("Failed to reach Floor 5 after 50 clicks", reachedDestination);
            // Removed stepDelay()
        }
    }
}