package com.example.oncampusapp;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
public class AccountPageBannerE2ETest {

    @Before
    public void setUp() throws Exception {
        // Inject mock data before the activity launches
        long now = System.currentTimeMillis();
        long tenMinsFromNow = now + (10 * 60 * 1000);
        long oneHourFromNow = now + (60 * 60 * 1000);

        SimpleDateFormat exactTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

        JSONObject event = new JSONObject();
        event.put("summary", "E2E Test Class");
        event.put("location", "H-110");

        JSONObject start = new JSONObject();
        start.put("dateTime", exactTimeFormat.format(new Date(tenMinsFromNow)));
        event.put("start", start);

        JSONObject end = new JSONObject();
        end.put("dateTime", exactTimeFormat.format(new Date(oneHourFromNow)));
        event.put("end", end);

        JSONArray array = new JSONArray();
        array.put(event);

        CalendarEventManager.setGlobalEventsJson(array.toString());
    }

    @Test
    public void testBannerAppearsWithCorrectData(){
        // Launch the Activity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AccountPage.class);
        intent.putExtra("email", "test@concordia.ca");
        ActivityScenario<AccountPage> scenario = ActivityScenario.launch(intent);


        // Verify the banner is visible
        Espresso.onView(withId(R.id.included_banner))
                .check(matches(isDisplayed()));

        // Verify the title is correct
        Espresso.onView(withId(R.id.banner_event_title))
                .check(matches(withText("E2E Test Class")));

        // Verify the smart location parser updated the text to the full Hall building name
        Espresso.onView(withId(R.id.banner_event_details))
                .check(matches(withText("Henry F. Hall Building - Room 110")));

        // Verify the countdown logic contains "mins"
        Espresso.onView(withId(R.id.banner_time_status))
                .check(matches(withText(containsString("mins"))));


        scenario.close();
    }

    @After
    public void tearDown() {
        // Clean up so it doesn't affect other tests
        CalendarEventManager.setGlobalEventsJson("");
    }
}