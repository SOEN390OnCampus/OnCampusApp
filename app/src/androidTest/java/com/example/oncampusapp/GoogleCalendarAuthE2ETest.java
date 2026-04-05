package com.example.oncampusapp;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GoogleCalendarAuthE2ETest {

    @Before
    public void setUp() {
        // Seed mock calendar data so AccountPage renders without real Google auth
        CalendarEventManager.globalCalendarListJson = "{\"items\":["
                + "{\"id\":\"primary\",\"summary\":\"My Calendar\",\"backgroundColor\":\"#4285F4\"},"
                + "{\"id\":\"work\",\"summary\":\"Work Calendar\",\"backgroundColor\":\"#0F9D58\"}"
                + "]}";
        CalendarEventManager.globalEventsJson = "[]";

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("OnCampusPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("selected_calendar", null);
        editor.apply();
    }



    @Test
    public void googleCalendarAuthE2ETest() {
        try (ActivityScenario<AccountPage> scenario = ActivityScenario.launch(AccountPage.class)) {
            onView(withId(R.id.refreshCalendar))
                    .perform(scrollTo(), click());

            onView(allOf(withId(R.id.btn_allow), withText("Allow"), isDisplayed()))
                    .perform(click());

            // Replaced lambda with method reference
            scenario.onActivity(Assert::assertNotNull);
        }
    }





    @Ignore("Requires a live Google OAuth token — skipped in CI/automated runs")
    @Test
    public void OpenCalendarTest() throws InterruptedException {
        try (ActivityScenario<AccountPage> scenario = ActivityScenario.launch(AccountPage.class)) {

            onView(childAtPosition(withId(R.id.calendarListContainer), 0))
                    .perform(click());


            onView(withId(R.id.nav_right))
                    .perform(click());

            onView(withId(R.id.nav_left))
                    .perform(click());

            onView(withId(R.id.btn_select_main_calendar))
                    .perform(click());

            onView(withId(R.id.btn_back))
                    .perform(click());


            onView(childAtPosition(withId(R.id.calendarListContainer), 1))
                    .perform(click());

        }
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
