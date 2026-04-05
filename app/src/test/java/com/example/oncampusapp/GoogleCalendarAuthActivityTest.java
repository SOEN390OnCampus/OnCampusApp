package com.example.oncampusapp;

import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GoogleCalendarAuthActivityTest {

    private GoogleCalendarAuthActivity activity;

    @Before
    public void setUp() {
        // Build, create, and resume the activity.
        // Robolectric gracefully handles the default GoogleSignIn.getLastSignedInAccount()
        // by returning null, allowing the Activity to initialize normally.
        activity = Robolectric.buildActivity(GoogleCalendarAuthActivity.class)
                .create()
                .resume()
                .get();
    }

    @Test
    public void testActivity_isNotNull() {
        assertNotNull("Activity should be successfully created", activity);
    }

    @Test
    public void testInitialUIState_isCorrect() {
        MaterialButton connectButton = activity.findViewById(R.id.btn_calendar_signin);
        ProgressBar progressBar = activity.findViewById(R.id.progressBar);
        TextView statusText = activity.findViewById(R.id.statusText);

        // Verify initial visibility and states
        assertNotNull(connectButton);
        assertTrue("Connect button should be enabled initially", connectButton.isEnabled());

        assertEquals("Progress bar should be hidden initially",
                View.GONE, progressBar.getVisibility());

        // Status text might be GONE or INVISIBLE initially, depending on your XML.
        // But it shouldn't be showing an error right off the bat.
        if (statusText.getVisibility() == View.VISIBLE) {
            assertEquals("Status text should be empty if visible initially",
                    "", statusText.getText().toString());
        }
    }

    // --- Bottom Navigation Routing Tests ---

    @Test
    public void testBottomNav_isSetToAccountInitially() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);

        assertEquals("Bottom nav should highlight the Account tab",
                R.id.nav_account, bottomNav.getSelectedItemId());
    }

    @Test
    public void testBottomNav_clickHome_startsMapsActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);

        // Action: Click the Home tab
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Assertion: Verify MapsActivity intent was fired
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent expectedIntent = new Intent(activity, MapsActivity.class);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Should start MapsActivity", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());

        // Assertion: Verify the current activity finishes so we don't build a massive backstack
        assertTrue("Activity should finish after navigating away", activity.isFinishing());
    }

    @Test
    public void testBottomNav_clickSettings_startsSettingsActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);

        // Action: Click the Settings tab
        bottomNav.setSelectedItemId(R.id.nav_settings);

        // Assertion: Verify SettingsActivity intent was fired
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent expectedIntent = new Intent(activity, SettingsActivity.class);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Should start SettingsActivity", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        assertTrue("Activity should finish after navigating away", activity.isFinishing());
    }

    @Test
    public void testBottomNav_clickAccount_doesNotStartNewActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);

        // Action: Click the Account tab (the current tab)
        bottomNav.setSelectedItemId(R.id.nav_account);

        // Assertion: Verify NO intent was fired because we are already here
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        // Updated to use assertNull
        assertNull("No new activity should be started", actualIntent);
        assertFalse("Activity should NOT finish", activity.isFinishing());
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Test
    public void testOnDestroy_doesNotThrow() {
        ActivityController<GoogleCalendarAuthActivity> controller =
                Robolectric.buildActivity(GoogleCalendarAuthActivity.class).create().resume();
        controller.destroy();
        assertNotNull(controller.get());
    }

    @Test
    public void testOnResume_doesNotThrow() {
        ActivityController<GoogleCalendarAuthActivity> controller =
                Robolectric.buildActivity(GoogleCalendarAuthActivity.class).create().resume();
        controller.pause().resume();
        assertNotNull(controller.get());
    }

    // ── showLoading ───────────────────────────────────────────────────────────

    @Test
    public void testShowLoading_true_showsProgressBarAndDisablesButton() throws Exception {
        invokeShowLoading(activity, true);

        ProgressBar progressBar = activity.findViewById(R.id.progressBar);
        MaterialButton connectButton = activity.findViewById(R.id.btn_calendar_signin);

        assertEquals(View.VISIBLE, progressBar.getVisibility());
        assertFalse(connectButton.isEnabled());
    }

    @Test
    public void testShowLoading_false_hidesProgressBarAndEnablesButton() throws Exception {
        invokeShowLoading(activity, true);  // first show
        invokeShowLoading(activity, false); // then hide

        ProgressBar progressBar = activity.findViewById(R.id.progressBar);
        MaterialButton connectButton = activity.findViewById(R.id.btn_calendar_signin);

        assertEquals(View.GONE, progressBar.getVisibility());
        assertTrue(connectButton.isEnabled());
    }

    // ── setStatusText ─────────────────────────────────────────────────────────

    @Test
    public void testSetStatusText_isError_makesTextVisible() throws Exception {
        invokeSetStatusText(activity, "Something went wrong", true);

        TextView statusText = activity.findViewById(R.id.statusText);
        assertEquals(View.VISIBLE, statusText.getVisibility());
        assertEquals("Something went wrong", statusText.getText().toString());
    }

    @Test
    public void testSetStatusText_notError_makesTextVisible() throws Exception {
        invokeSetStatusText(activity, "Signed in successfully", false);

        TextView statusText = activity.findViewById(R.id.statusText);
        assertEquals(View.VISIBLE, statusText.getVisibility());
        assertEquals("Signed in successfully", statusText.getText().toString());
    }

    // ── showCalendarFetchError ────────────────────────────────────────────────

    @Test
    public void testShowCalendarFetchError_updatesStatusText() throws Exception {
        Method method = GoogleCalendarAuthActivity.class
                .getDeclaredMethod("showCalendarFetchError", String.class);
        method.setAccessible(true);
        method.invoke(activity, "Network timeout");

        TextView statusText = activity.findViewById(R.id.statusText);
        assertEquals(View.VISIBLE, statusText.getVisibility());
        assertTrue(statusText.getText().toString().contains("Network timeout"));
    }

    @Test
    public void testShowCalendarFetchError_hidesProgressBar() throws Exception {
        invokeShowLoading(activity, true); // simulate loading started

        Method method = GoogleCalendarAuthActivity.class
                .getDeclaredMethod("showCalendarFetchError", String.class);
        method.setAccessible(true);
        method.invoke(activity, "timeout");

        ProgressBar progressBar = activity.findViewById(R.id.progressBar);
        assertEquals(View.GONE, progressBar.getVisibility());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void invokeShowLoading(GoogleCalendarAuthActivity act, boolean loading)
            throws Exception {
        Method m = GoogleCalendarAuthActivity.class.getDeclaredMethod("showLoading", boolean.class);
        m.setAccessible(true);
        m.invoke(act, loading);
    }

    private static void invokeSetStatusText(GoogleCalendarAuthActivity act,
                                            String message, boolean isError) throws Exception {
        Method m = GoogleCalendarAuthActivity.class
                .getDeclaredMethod("setStatusText", String.class, boolean.class);
        m.setAccessible(true);
        m.invoke(act, message, isError);
    }
}