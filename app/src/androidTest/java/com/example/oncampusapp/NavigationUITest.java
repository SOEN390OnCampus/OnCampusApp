package com.example.oncampusapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.android.gms.maps.model.LatLng;
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
    public void testOpenRoutePicker_ShowsInputs() {
        onView(withId(R.id.search_bar_container))
                .perform(click());

        onView(withId(R.id.route_picker_container))
                .check(matches(isDisplayed()));

        onView(withId(R.id.layout_inputs))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testInvalidAddress_DoesNotCrashOrNavigate() {

        onView(withId(R.id.search_bar_container)).perform(click());

        // Type gibberish without clicking the dropdown
        onView(withId(R.id.et_start))
                .perform(typeText("Fake Building 123"), closeSoftKeyboard());
        onView(withId(R.id.et_destination))
                .perform(typeText("Nowhere"), closeSoftKeyboard());

        // Try to trigger preview
        onView(withId(R.id.btn_mode_walking)).perform(click());

        // Try to click GO
        onView(withId(R.id.btn_go)).perform(click());

        // Verify the app safely blocked navigation and the Setup UI is still visible
        onView(withId(R.id.layout_inputs))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testNavigationModeToggle_GoAndExitButtons_demo() {

        // 1. Open the Picker
        Espresso.onView(withId(R.id.search_bar_container))
                .perform(click());


        // 2. Type a partial name ("Henry") like a human, then close the keyboard
        Espresso.onView(withId(R.id.et_start))
                .perform(typeText("Henry"), closeSoftKeyboard());

        // Click the first item in the Start dropdown

        Espresso.onView(withText("H - Henry F. Hall Building"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
                .perform(click());

        // 3. Type a partial name ("Molson") in Destination
        Espresso.onView(withId(R.id.et_destination))
                .perform(click(), typeText("Molson"), closeSoftKeyboard());

        // Click the destination from the dropdown
        Espresso.onView(withText("MB - John Molson School of Business"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
                .perform(click());
        // 4. Wait for the API call to return and draw the polyline.
        Espresso.onView(withId(R.id.btn_go))
                .check(matches(isEnabled()));

        // 5. Click the GO Button
        Espresso.onView(withId(R.id.btn_go))
                .perform(click());
        // 6. VERIFY: The big inputs should be hidden (GONE)
        Espresso.onView(withId(R.id.layout_inputs))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        Espresso.onView(withId(R.id.layout_navigation_active))
                .check(matches(isDisplayed()));

        // 7. VERIFY: The small Navigation Bar should be visible
        Espresso.onView(withId(R.id.btn_end_trip))
                .perform(click());

        // 8. Click the EXIT Button
        Espresso.onView(withId(R.id.layout_inputs))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));


        // 9. VERIFY: The UI restored to the setup mode
        Espresso.onView(withId(R.id.layout_navigation_active))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }
    @Test
    public void testTransportationModeButtons_PolylineUpdate_demo() {
        AtomicReference<List<LatLng>> previousPoints = new AtomicReference<>();


        // 1. Open the Picker
        onView(withId(R.id.search_bar_container))
                .perform(click());

        // 2. Set start and destination
        onView(withId(R.id.et_start))
                .perform(click(), replaceText("H - Henry F. Hall Building"), closeSoftKeyboard());

        onView(withId(R.id.et_destination))
                .perform(click(), replaceText("Richard J Renaud Science Complex (SP)"), closeSoftKeyboard());

        // 3. Test for each transportation mode button
        checkModeBtnAndPolyline(previousPoints, R.id.btn_mode_walking);
        checkModeBtnAndPolyline(previousPoints, R.id.btn_mode_driving);
        checkModeBtnAndPolyline(previousPoints, R.id.btn_mode_transit);
    }
    // Helper function for testing the transportation mode buttons
    private void checkModeBtnAndPolyline(AtomicReference<List<LatLng>> previousPoints, int btnId) {
        // Click the mode
        onView(withId(btnId)).perform(click());

        // Verify polyline
        activityRule.getScenario().onActivity(activity -> {
            List<Polyline> polylines = activity.getRoutePolylines();

            assertNotNull(polylines);
            assertFalse(polylines.isEmpty());

            List<LatLng> currentPoints = polylines.get(0).getPoints();

            if (previousPoints.get() != null) {
                assertNotEquals(previousPoints.get(), currentPoints);
            }

            previousPoints.set(currentPoints);
        });
    }
}