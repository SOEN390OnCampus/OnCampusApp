package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.location.Location;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MapsActivityTest {

    @Rule
    public ActivityScenarioRule<MapsActivity> activityRule = new ActivityScenarioRule<>(MapsActivity.class);

    @Test
    public void testCampusToggle_SwitchesText() {
        // Check if initial text is SGW
        onView(withId(R.id.btn_campus_switch)).check(matches(withText("LOY")));

        // Click the button
        onView(withId(R.id.btn_campus_switch)).perform(click());

        // Check if text changed to LOY
        onView(withId(R.id.btn_campus_switch)).check(matches(withText("SGW")));

        // Click again to toggle back
        onView(withId(R.id.btn_campus_switch)).perform(click());
        onView(withId(R.id.btn_campus_switch)).check(matches(withText("LOY")));
    }

    @Test
    public void testLocationButton_WithMockedLocation() {
        // 1. Setup the mock location
        double mockLat = 45.4950;
        double mockLng = -73.5780;
        Location mockLocation = mock(Location.class);
        when(mockLocation.getLatitude()).thenReturn(mockLat);
        when(mockLocation.getLongitude()).thenReturn(mockLng);

        // 2. Click the location button in the UI
        onView(withId(R.id.btn_location)).perform(click());

        // 3. Verify the camera moved to the mock location
        activityRule.getScenario().onActivity(activity -> {
            // Access the map from your activity (ensure mMap is public or has a getter)
            LatLng cameraPos = activity.getMap().getCameraPosition().target;

            // Assert the coordinates (using a small delta for float comparison)
            assertEquals(mockLat, cameraPos.latitude, 0.01);
            assertEquals(mockLng, cameraPos.longitude, 0.01);
        });
    }
}
