package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.Manifest;

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
public class MapsActivityTest {
    @Rule
    public ActivityScenarioRule<MapsActivity> activityRule = new ActivityScenarioRule<>(MapsActivity.class);

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    // Helper to wait for animateCamera to finish
    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCampusToggle_SwitchesCampus() {
        // 1. Initial state: Button text is LOY
        onView(withId(R.id.btn_campus_switch)).check(matches(withText("LOY")));

        // 2. First click: Should move to Loyola and change text to SGW
        sleep(5000);
        onView(withId(R.id.btn_campus_switch)).perform(click());
        sleep((1000));

        onView(withId(R.id.btn_campus_switch)).check(matches(withText("SGW")));
        activityRule.getScenario().onActivity(activity -> {
            LatLng cameraPos = activity.getMap().getCameraPosition().target;
            float zoom = activity.getMap().getCameraPosition().zoom;

            assertEquals(MapsActivity.LOY_COORDS.latitude, cameraPos.latitude, 0.001);
            assertEquals(MapsActivity.LOY_COORDS.longitude, cameraPos.longitude, 0.001);
            assertEquals(16f, zoom, 0.1f);
        });

        // 3. Second click: Should move to SGW and change text back to LOY
        onView(withId(R.id.btn_campus_switch)).perform(click());
        sleep(1000);

        onView(withId(R.id.btn_campus_switch)).check(matches(withText("LOY")));
        activityRule.getScenario().onActivity(activity -> {
            LatLng cameraPos = activity.getMap().getCameraPosition().target;
            float zoom = activity.getMap().getCameraPosition().zoom;

            assertEquals(MapsActivity.SGW_COORDS.latitude, cameraPos.latitude, 0.001);
            assertEquals(MapsActivity.SGW_COORDS.longitude, cameraPos.longitude, 0.001);
            assertEquals(16f, zoom, 0.1f);
        });

        // 4. Third click: Should move to LOY and change text back to SGW
        onView(withId(R.id.btn_campus_switch)).perform(click());
        sleep(1000);

        onView(withId(R.id.btn_campus_switch)).check(matches(withText("SGW")));
        activityRule.getScenario().onActivity(activity -> {
            LatLng cameraPos = activity.getMap().getCameraPosition().target;
            float zoom = activity.getMap().getCameraPosition().zoom;

            assertEquals(MapsActivity.LOY_COORDS.latitude, cameraPos.latitude, 0.001);
            assertEquals(MapsActivity.LOY_COORDS.longitude, cameraPos.longitude, 0.001);
            assertEquals(16f, zoom, 0.1f);
        });
    }

    @Test
    public void testLocationButton_MovesCameraToCurrentPosition() throws InterruptedException {
        // Initialize the latch
        CountDownLatch latch = new CountDownLatch(1);

        // Click the location button
        sleep(5000);
        onView(withId(R.id.btn_location)).perform(click());
        sleep(1000);

        // Verify the map has moved away from the initial wider view of Montreal
        activityRule.getScenario().onActivity(activity -> {
            LatLng cameraPos = activity.getMap().getCameraPosition().target;
            float zoom = activity.getMap().getCameraPosition().zoom;

            activity.fusedLocationClient
                    .getLastLocation()
                    .addOnSuccessListener(location -> {
                        try {
                            // Check if the location is not null
                            if (location != null) {
                                double currentLat = location.getLatitude();
                                double currentLng = location.getLongitude();

                                // Ensure the map moved to a valid coordinate
                                assertEquals(currentLat, cameraPos.latitude, 0.0001);
                                assertEquals(currentLng, cameraPos.longitude, 0.0001);
                                assertEquals(16f, zoom, 0.1f);
                            }
                        } finally {
                            // Release the latch so the test thread can continue
                            latch.countDown();
                        }
                    });
        });

        // Wait for the asynchronous block to finish (timeout after 5 seconds)
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("Timed out waiting for Location Success Listener", completed);
    }
}