package com.example.oncampusapp;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

        Espresso.onView(withId(R.id.search_bar_container))
                .perform(click());

        Thread.sleep(1000); // Wait for slide-down animation

        Espresso.onView(withId(R.id.route_picker_container))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(withId(R.id.layout_inputs))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testInvalidAddress_DoesNotCrashOrNavigate() throws InterruptedException {
        Thread.sleep(6000);

        Espresso.onView(withId(R.id.search_bar_container)).perform(click());
        Thread.sleep(1000);

        // Type gibberish without clicking the dropdown
        Espresso.onView(withId(R.id.et_start))
                .perform(ViewActions.typeText("Fake Building 123"), closeSoftKeyboard());
        Espresso.onView(withId(R.id.et_destination))
                .perform(ViewActions.typeText("Nowhere"), closeSoftKeyboard());

        // Try to trigger preview
        Espresso.onView(withId(R.id.btn_mode_walking)).perform(click());
        Thread.sleep(1000);

        // Try to click GO
        Espresso.onView(withId(R.id.btn_go)).perform(click());
        Thread.sleep(500);

        // Verify the app safely blocked navigation and the Setup UI is still visible
        Espresso.onView(withId(R.id.layout_inputs))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testNavigationModeToggle_GoAndExitButtons() throws InterruptedException {
        Thread.sleep(4000); // Wait for map + GeoJSON

        // 1. Open the Picker
        Espresso.onView(withId(R.id.search_bar_container))
                .perform(click());
        Thread.sleep(1000);

        // 2. Type a partial name ("Henry") like a human, then close the keyboard
        Espresso.onView(withId(R.id.et_start))
                .perform(ViewActions.typeText("Henry"), closeSoftKeyboard());

        Thread.sleep(1500); // Give the dropdown a moment to animate in

        // Click the first item in the Start dropdown
        Espresso.onView(ViewMatchers.withText("H - Henry F. Hall Building"))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup())
                .perform(click());

        // 3. Type a partial name ("Molson") in Destination
        Espresso.onView(ViewMatchers.withId(R.id.et_destination))
                .perform(ViewActions.typeText("Molson"), ViewActions.closeSoftKeyboard());

        Thread.sleep(1500); // Wait for dropdown

        // Click the destination from the dropdown
        Espresso.onView(ViewMatchers.withText("MB - John Molson School of Business"))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup())
                .perform(click());

        // 4. Wait for the API call to return and draw the polyline.
        Thread.sleep(3000);

        // 5. Click the GO Button
        Espresso.onView(withId(R.id.btn_go))
                .perform(click());
        Thread.sleep(2500);

        // 6. VERIFY: The big inputs should be hidden (GONE)
        Espresso.onView(withId(R.id.layout_inputs))
                .check(ViewAssertions.matches(
                        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // 7. VERIFY: The small Navigation Bar should be visible
        Espresso.onView(withId(R.id.layout_navigation_active))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        // 8. Click the EXIT Button
        Espresso.onView(withId(R.id.btn_end_trip))
                .perform(click());

        // Just a standard wait for the layout to pop back up
        Thread.sleep(1000);

        // 9. VERIFY: The UI restored to the setup mode
        Espresso.onView(ViewMatchers.withId(R.id.layout_inputs))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(withId(R.id.layout_navigation_active))
                .check(ViewAssertions.matches(
                        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }
    @Test
    public void testTransportationModeButtons_PolylineUpdate() throws InterruptedException {
        AtomicReference<Polyline> navigationLine = new AtomicReference<>();

        Thread.sleep(4000); // Wait for map + GeoJSON

        // 1. Open the Picker
        Espresso.onView(withId(R.id.search_bar_container))
                .perform(click());
        Thread.sleep(1000);

        // 2. Set start and destination
        Espresso.onView(withId(R.id.et_start))
                .perform(click(), replaceText("H - Henry F. Hall Building"), closeSoftKeyboard());

        Espresso.onView(withId(R.id.et_destination))
                .perform(click(), replaceText("MB - John Molson School of Business"), closeSoftKeyboard());

        // 3. Test for each transportation mode button
        checkModeBtnAndPolyline(navigationLine, R.id.btn_mode_walking, true);
        checkModeBtnAndPolyline(navigationLine, R.id.btn_mode_driving, false);
        checkModeBtnAndPolyline(navigationLine, R.id.btn_mode_transit, false);
    }
    // Helper function for testing the transportation mode buttons
    private void checkModeBtnAndPolyline(AtomicReference<Polyline> navigationLine, int btnId, boolean expectDotted) throws InterruptedException {
        // Click the mode
        Espresso.onView(withId(btnId)).perform(click());
        Thread.sleep(3000); // Wait for route to draw

        // Verify polyline
        activityRule.getScenario().onActivity(activity -> {
            Polyline polyline = activity.getBluePolyline();
            assertNotNull(polyline);
            assertNotEquals(polyline, navigationLine.get()); // Ensure updated

            List<PatternItem> pattern = polyline.getPattern();
            if (expectDotted) { // Check for dotted line if expected
                assertNotNull(pattern);
                assertFalse(pattern.isEmpty());
            } else {
                assertNull(pattern);
            }
            navigationLine.set(polyline); // Set new polyline for next button test
        });
    }
}