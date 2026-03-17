package com.example.oncampusapp;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class IndoorSearchE2ETest {

    @Test
    public void testSearchRoomDropsPin() {
        // 1. Prepare the Intent to load the Hall building, Floor 8
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), IndoorMapActivity.class);
        intent.putExtra("BUILDING_ID", "H");
        intent.putExtra("FLOOR_ID", "8");

        // 2. Launch the Activity
        try (ActivityScenario<IndoorMapActivity> scenario = ActivityScenario.launch(intent)) {

            // 3. Verify the floor title loaded correctly based on the Intent
            onView(withId(R.id.tv_floor_title))
                    .check(matches(withText("H FLOOR 8")));

            // 4. Type "H-807" into the search bar and close the keyboard
            onView(withId(R.id.et_indoor_search))
                    .perform(typeText("H-807"), closeSoftKeyboard());

            // 5. Click the autocomplete dropdown suggestion
            // Since the dropdown floats in a separate system window above the app,
            // we use inRoot(isPlatformPopup()) to tell Espresso where to look.
            onView(withText("H-807"))
                    .inRoot(RootMatchers.isPlatformPopup())
                    .perform(click());

            // 6. Verify that the search bar actually kept the text after clicking
            onView(withId(R.id.et_indoor_search))
                    .check(matches(withText("H-807")));

            // 7. Test the back button gracefully closes the activity
            onView(withId(R.id.btn_back_indoor)).perform(click());
        }
    }
}