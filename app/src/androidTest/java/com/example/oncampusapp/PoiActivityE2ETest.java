package com.example.oncampusapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.Instrumentation;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PoiActivityE2ETest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void poiActivity_opens_withDefaultRestaurantsVisible() {
        ActivityScenario.launch(PoiActivity.class);

        onView(withText("Cafe Van Houtte")).check(matches(isDisplayed()));
        onView(withText("Ganadara")).check(matches(isDisplayed()));
    }

    @Test
    public void clickingBookstoresTab_showsBookstores() {
        ActivityScenario.launch(PoiActivity.class);

        onView(withId(R.id.tab_bookstores)).perform(click());

        onView(withText("Paragraphe Bookstore")).check(matches(isDisplayed()));
        onView(withText("Indigo")).check(matches(isDisplayed()));
        onView(withText("Cafe Van Houtte")).check(doesNotExist());
    }

    @Test
    public void clickingShoppingTab_showsShoppingCenters() {
        ActivityScenario.launch(PoiActivity.class);

        onView(withId(R.id.tab_shopping)).perform(click());

        onView(withText("Eaton Centre")).check(matches(isDisplayed()));
        onView(withText("Alexis Nihon")).check(matches(isDisplayed()));
        onView(withText("Ganadara")).check(doesNotExist());
    }

    @Test
    public void clickingPoi_opensMapsActivity_withCorrectExtras() {
        Instrumentation.ActivityResult result =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);

        intending(hasComponent(MapsActivity.class.getName())).respondWith(result);

        ActivityScenario.launch(PoiActivity.class);

        onView(withText("Cafe Van Houtte")).check(matches(isDisplayed()));
        onView(withText("Cafe Van Houtte")).perform(click());

        intended(hasComponent(MapsActivity.class.getName()));
        intended(hasExtra("POI_NAME", "Cafe Van Houtte"));
        intended(hasExtra("POI_LAT", 45.4958));
        intended(hasExtra("POI_LNG", -73.5785));
        intended(hasExtra("OPEN_POI_ROUTE", true));
    }

    @Test
    public void bottomNavHome_opensMapsActivity() {
        Instrumentation.ActivityResult result =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);

        intending(hasComponent(MapsActivity.class.getName())).respondWith(result);

        ActivityScenario<PoiActivity> scenario = ActivityScenario.launch(PoiActivity.class);
        assertNotNull(scenario);

        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_home)).perform(click());

        intended(hasComponent(MapsActivity.class.getName()));
    }

    @Test
    public void bottomNavAccount_opensGoogleCalendarAuthActivity() {
        Instrumentation.ActivityResult result =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);

        intending(hasComponent(GoogleCalendarAuthActivity.class.getName())).respondWith(result);

       ActivityScenario<PoiActivity> scenario = ActivityScenario.launch(PoiActivity.class);
        assertNotNull(scenario);

        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_account)).perform(click());

        intended(hasComponent(GoogleCalendarAuthActivity.class.getName()));
    }

    @Test
    public void bottomNavSettings_opensSettingsActivity() {
        Instrumentation.ActivityResult result =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);

        intending(hasComponent(SettingsActivity.class.getName())).respondWith(result);

        ActivityScenario<PoiActivity> scenario = ActivityScenario.launch(PoiActivity.class);
        assertNotNull(scenario);

        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_settings)).perform(click());

        intended(hasComponent(SettingsActivity.class.getName()));
    }

    @Test
    public void backButton_finishesActivity() {
        ActivityScenario<PoiActivity> scenario = ActivityScenario.launch(PoiActivity.class);

        onView(withId(R.id.btn_back)).perform(click());

        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }
}