package com.example.oncampusapp;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GoogleCalendarAuthE2ETest {

    @Rule
    public ActivityScenarioRule<MapsActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MapsActivity.class);

    @Before
    public void resetSelectedCalendarPreference() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("OnCampusPrefs", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("selected_calendar", null);
        editor.apply();
    }

    @Test
    public void googleCalendarAuthE2ETest() {

        // Step 1 - navigate to account page
        onView(allOf(withId(R.id.nav_account),
                withContentDescription("Favorites"),
                isDisplayed()))
                .perform(click());

        // Step 2 - wait for AccountPage to load, then click refresh
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        onView(withId(R.id.refreshCalendar))
                .perform(scrollTo(), click());

        // Step 3 - wait for dialog then click Allow
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        onView(allOf(withId(R.id.btn_allow), withText("Allow"), isDisplayed()))
                .perform(click());
    }

    @Test
    public void OpenCalendarTest() throws InterruptedException {
        onView(allOf(withId(R.id.nav_account),
                withContentDescription("Favorites"),
                isDisplayed()))
                .perform(click());

        Thread.sleep(3000);

        onView(childAtPosition(withId(R.id.calendarListContainer), 0))
                .perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.nav_right))
                .perform(click());

        onView(withId(R.id.nav_left))
                .perform(click());

        onView(withId(R.id.btn_select_main_calendar))
                .perform(click());

        onView(withId(R.id.btn_back))
                .perform(click());

        Thread.sleep(3000);

        onView(childAtPosition(withId(R.id.calendarListContainer), 1))
                .perform(click());

        Thread.sleep(3000);
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
