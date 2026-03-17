package com.example.oncampusapp;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class IndoorMapNavigationE2ETest {

    @Rule
    public ActivityScenarioRule<MapsActivity> activityRule =
            new ActivityScenarioRule<>(MapsActivity.class);

    @Test
    public void testNavigateToIndoorMapViaDialog() {
        // 1. Open the Building Selection Dialog
        onView(withId(R.id.btn_indoor_map)).perform(click());

        // 2. Ensure Dialog is displayed, then click the "H" building row to expand the floor list
        onView(withId(R.id.rv_indoor_building)).check(matches(isDisplayed()));

        onView(withId(R.id.rv_indoor_building))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText("H")), click()
                ));

        // 3. Click the Chip for floor "8"
        // Espresso's internal idling will automatically wait for the RecyclerView to update
        // and display the chip, provided emulator animations are disabled.
        onView(withText("8")).perform(click());

        // 4. Verify that the IndoorMapActivity launched successfully
        // Activity transitions are naturally synchronized by Espresso.
        onView(withId(R.id.indoor_map_view)).check(matches(isDisplayed()));
        onView(withId(R.id.et_indoor_search)).check(matches(isDisplayed()));
    }
}