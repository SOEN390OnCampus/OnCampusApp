package com.example.oncampusapp;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SettingsActivityTest {

    private SettingsActivity activity;

    @Before
    public void setUp() {
        // Build, create, and resume the activity to trigger both onCreate and onResume
        activity = Robolectric.buildActivity(SettingsActivity.class)
                .create()
                .resume()
                .get();
    }

    @Test
    public void testActivity_isCreatedSuccessfully() {
        assertNotNull("Activity should not be null", activity);
    }

    // --- Header / Button Tests ---

    @Test
    public void testBackButton_finishesActivity() {
        ImageView backButton = activity.findViewById(R.id.btn_back_settings);
        backButton.performClick();

        assertTrue("Activity should be finishing after back button click", activity.isFinishing());
    }

    @Test
    public void testAccessibilityButton_startsAccessibilityActivity() {
        View accessibilityButton = activity.findViewById(R.id.btn_accessibility_action);
        accessibilityButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Should start a new activity", actualIntent);
        assertEquals(AccessibilityActivity.class.getName(), actualIntent.getComponent().getClassName());
    }

    @Test
    public void testAboutButton_showsComingSoonToast() {
        View aboutButton = activity.findViewById(R.id.btn_about_action);

        // Action: Click the "About" button
        aboutButton.performClick();

        // Assertion: Intercept the Toast using Robolectric's ShadowToast
        String latestToast = ShadowToast.getTextOfLatestToast();
        assertNotNull("A Toast should have been shown", latestToast);
        assertEquals("About content coming soon", latestToast);
    }

    // --- Bottom Navigation Tests ---

    @Test
    public void testBottomNav_isSetToSettingsInitially() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        assertEquals("Bottom nav should highlight the Settings tab",
                R.id.nav_settings, bottomNav.getSelectedItemId());
    }

    @Test
    public void testBottomNav_clickHome_startsMapsActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent expectedIntent = new Intent(activity, MapsActivity.class);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull(actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        assertTrue("Activity should finish after navigating away", activity.isFinishing());
    }

    @Test
    public void testBottomNav_clickAccount_startsAuthActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_account);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent expectedIntent = new Intent(activity, GoogleCalendarAuthActivity.class);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull(actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        assertTrue("Activity should finish after navigating away", activity.isFinishing());
    }

    @Test
    public void testBottomNav_clickSettings_doesNotStartNewActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);

        // Click the tab we are already on
        bottomNav.setSelectedItemId(R.id.nav_settings);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertTrue("No new activity should be started", actualIntent == null);
        assertFalse("Activity should NOT finish", activity.isFinishing());
    }
}