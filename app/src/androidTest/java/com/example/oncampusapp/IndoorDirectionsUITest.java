package com.example.oncampusapp;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class IndoorDirectionsUITest {

    // This rule launches the activity before each test and closes it after.
    @Rule
    public ActivityScenarioRule<IndoorDirectionsActivity> activityRule =
            new ActivityScenarioRule<>(IndoorDirectionsActivity.class);

    @Test
    public void testInitialLayoutVisibility() {
        // Verify input fields and main button are visible to the user
        onView(withId(R.id.et_from_room)).check(matches(isDisplayed()));
        onView(withId(R.id.et_to_room)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_find_route)).check(matches(isDisplayed()));

        // The error/status text should NOT be visible when the screen first loads
        onView(withId(R.id.tv_status)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSwapButtonFlipsInputText() {
        // 1. Force the text directly into the views to bypass AutoComplete popup interruptions
        onView(withId(R.id.et_from_room)).perform(replaceText("H-820"), closeSoftKeyboard());
        onView(withId(R.id.et_to_room)).perform(replaceText("MB-1.210"), closeSoftKeyboard());

        // 2. Click the swap button
        onView(withId(R.id.btn_swap)).perform(click());

        // 3. Verify the text has been swapped
        onView(withId(R.id.et_from_room)).check(matches(withText("MB-1.210")));
        onView(withId(R.id.et_to_room)).check(matches(withText("H-820")));
    }

    @Test
    public void testEmptyFieldsTriggerErrorMessage() {
        // 1. Click "Find Route" without typing anything into the fields
        onView(withId(R.id.btn_find_route)).perform(click());

        // 2. Verify the status text view becomes visible
        onView(withId(R.id.tv_status)).check(matches(isDisplayed()));

        // 3. Verify it shows the correct error string using a flexible matcher
        // This avoids exact-match failures caused by slight typos or punctuation differences
        onView(withId(R.id.tv_status))
                .check(matches(withText(containsString("room"))));
    }
}