package com.example.oncampusapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.oncampusapp.CalendarEventManager;
import com.example.oncampusapp.MapsActivity;
import com.example.oncampusapp.TestDateUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.Manifest;
import androidx.test.rule.GrantPermissionRule;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapsActivityBannerE2ETest {

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            );

    @Test
    public void clickingBannerDirectionsButton_opensDirectionsFlow() throws Exception {
        ActivityScenario<MapsActivity> scenario =
                ActivityScenario.launch(MapsActivity.class);

        scenario.onActivity(activity -> {
            try {
                long now = System.currentTimeMillis();

                JSONObject nextEvent = new JSONObject();
                nextEvent.put("summary", "SOEN 341");
                nextEvent.put("location", "Henry F. Hall Building");

                JSONObject nextStart = new JSONObject();
                nextStart.put("dateTime", TestDateUtils.toIso(now + 10 * 60 * 1000));
                nextEvent.put("start", nextStart);

                JSONObject nextEnd = new JSONObject();
                nextEnd.put("dateTime", TestDateUtils.toIso(now + 70 * 60 * 1000));
                nextEvent.put("end", nextEnd);

                JSONObject prevEvent = new JSONObject();
                prevEvent.put("summary", "COMP 346");
                prevEvent.put("location", "John Molson School of Business");

                JSONObject prevStart = new JSONObject();
                prevStart.put("dateTime", TestDateUtils.toIso(now - 50 * 60 * 1000));
                prevEvent.put("start", prevStart);

                JSONObject prevEnd = new JSONObject();
                prevEnd.put("dateTime", TestDateUtils.toIso(now));
                prevEvent.put("end", prevEnd);

                JSONArray events = new JSONArray();
                events.put(prevEvent);
                events.put(nextEvent);

                CalendarEventManager.globalEventsJson = events.toString();

                activity.checkAndDisplayNextEventBannerForTest();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(3000);

        // Verify banner button is shown
        onView(withId(R.id.banner_btn_go)).check(matches(isDisplayed()));
        Thread.sleep(3000);

        // Click the banner directions button
        onView(withId(R.id.banner_btn_go)).perform(click());
        Thread.sleep(4000);

        // Assert that route UI appeared
        onView(withId(R.id.route_picker_container)).check(matches(isDisplayed()));
        Thread.sleep(4000);

        onView(withId(R.id.et_destination))
                .check(matches(withText("Henry F. Hall Building")));
    }
}