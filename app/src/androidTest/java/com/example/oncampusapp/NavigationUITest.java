package com.example.oncampusapp;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NavigationUITest {

    // Grant ALL permissions before the activity launches
    @Rule(order = 0)
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.POST_NOTIFICATIONS
    );

    // order = 1 ensures permissions are granted before the activity starts (or the test fails)
    @Rule(order = 1)
    public ActivityScenarioRule<MapsActivity> activityRule =
            new ActivityScenarioRule<>(MapsActivity.class);

    @Test
    public void testOpenRoutePicker_ShowsInputs() throws InterruptedException {
        Thread.sleep(4000); // Wait for map + GeoJSON

        Espresso.onView(ViewMatchers.withId(R.id.search_bar_container))
                .perform(ViewActions.click());

        Thread.sleep(1000); // Wait for slide-down animation

        Espresso.onView(ViewMatchers.withId(R.id.route_picker_container))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.layout_inputs))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testInvalidAddress_DoesNotCrashOrNavigate() throws InterruptedException {
        Thread.sleep(6000);

        Espresso.onView(ViewMatchers.withId(R.id.search_bar_container)).perform(ViewActions.click());
        Thread.sleep(1000);

        // Type gibberish without clicking the dropdown
        Espresso.onView(ViewMatchers.withId(R.id.et_start))
                .perform(ViewActions.typeText("Fake Building 123"), ViewActions.closeSoftKeyboard());
        Espresso.onView(ViewMatchers.withId(R.id.et_destination))
                .perform(ViewActions.typeText("Nowhere"), ViewActions.closeSoftKeyboard());

        // Try to trigger preview
        Espresso.onView(ViewMatchers.withId(R.id.btn_mode_walking)).perform(ViewActions.click());
        Thread.sleep(1000);

        // Try to click GO
        Espresso.onView(ViewMatchers.withId(R.id.btn_go)).perform(ViewActions.click());
        Thread.sleep(500);

        // Verify the app safely blocked navigation and the Setup UI is still visible
        Espresso.onView(ViewMatchers.withId(R.id.layout_inputs))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testNavigationModeToggle_GoAndExitButtons() throws InterruptedException {
        Thread.sleep(4000); // Wait for map + GeoJSON

        // 1. Open the Picker
        Espresso.onView(ViewMatchers.withId(R.id.search_bar_container))
                .perform(ViewActions.click());
        Thread.sleep(1000);

        // 2. Type a partial name ("Henry") like a human, then close the keyboard
        Espresso.onView(ViewMatchers.withId(R.id.et_start))
                .perform(ViewActions.typeText("Henry"), ViewActions.closeSoftKeyboard());

        Thread.sleep(1500); // Give the dropdown a moment to animate in

        // Click the first item in the Start dropdown
        Espresso.onView(ViewMatchers.withText("H - Henry F. Hall Building"))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup())
                .perform(ViewActions.click());

        // 3. Type a partial name ("Molson") in Destination
        Espresso.onView(ViewMatchers.withId(R.id.et_destination))
                .perform(ViewActions.typeText("Molson"), ViewActions.closeSoftKeyboard());

        Thread.sleep(1500); // Wait for dropdown

        // Click the destination from the dropdown
        Espresso.onView(ViewMatchers.withText("MB - John Molson School of Business"))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup())
                .perform(ViewActions.click());

        // 4. Wait for the API call to return and draw the polyline.
        Thread.sleep(3000);

        // 5. Click the GO Button
        Espresso.onView(ViewMatchers.withId(R.id.btn_go))
                .perform(ViewActions.click());
        Thread.sleep(2500);

        // 6. VERIFY: The big inputs should be hidden (GONE)
        Espresso.onView(ViewMatchers.withId(R.id.layout_inputs))
                .check(ViewAssertions.matches(
                        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // 7. VERIFY: The small Navigation Bar should be visible
        Espresso.onView(ViewMatchers.withId(R.id.layout_navigation_active))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        // 8. Click the EXIT Button
        Espresso.onView(ViewMatchers.withId(R.id.btn_end_trip))
                .perform(ViewActions.click());

        // Just a standard wait for the layout to pop back up
        Thread.sleep(1000);

        // 9. VERIFY: The UI restored to the setup mode
        Espresso.onView(ViewMatchers.withId(R.id.layout_inputs))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.layout_navigation_active))
                .check(ViewAssertions.matches(
                        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }
}