package com.example.oncampusapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class TextSizePreferencesTest {

    private Context context;
    private Activity activity;
    private static final String PREFS_NAME = "OnCampusAccessibilityPrefs";

    @Before
    public void setUp() {
        // Grab a safe Context for preference testing
        context = ApplicationProvider.getApplicationContext();

        // Build a raw Activity specifically for testing the apply() method
        activity = Robolectric.buildActivity(Activity.class).get();
    }

    @After
    public void tearDown() {
        // Clear preferences after every test so they don't bleed into each other!
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    // --- SharedPreferences Toggles ---

    @Test
    public void testIsTextSizeEnabled_defaultIsFalse() {
        assertFalse("Default text size enabled should be false",
                TextSizePreferences.isTextSizeEnabled(context));
    }

    @Test
    public void testSetTextSizeEnabled_updatesCorrectly() {
        TextSizePreferences.setTextSizeEnabled(context, true);
        assertTrue("Preference should be true after setting it",
                TextSizePreferences.isTextSizeEnabled(context));

        TextSizePreferences.setTextSizeEnabled(context, false);
        assertFalse("Preference should be false after flipping it back",
                TextSizePreferences.isTextSizeEnabled(context));
    }

    // --- Percentage Clamping Logic ---

    @Test
    public void testGetTextSizePercent_defaultIs100() {
        assertEquals("Default percentage should be 100",
                100, TextSizePreferences.getTextSizePercent(context));
    }

    @Test
    public void testSetTextSizePercent_updatesCorrectlyWithinBounds() {
        TextSizePreferences.setTextSizePercent(context, 120);
        assertEquals("Percentage should be exactly what was set",
                120, TextSizePreferences.getTextSizePercent(context));
    }

    @Test
    public void testSetTextSizePercent_clampsMinimumTo50() {
        // Action: Try to set a number way below the minimum bounds
        TextSizePreferences.setTextSizePercent(context, 10);

        // Assertion: It should clamp to 50
        assertEquals("Percentage should not go below 50",
                50, TextSizePreferences.getTextSizePercent(context));
    }

    @Test
    public void testSetTextSizePercent_clampsMaximumTo200() {
        // Action: Try to set a number way above the maximum bounds
        TextSizePreferences.setTextSizePercent(context, 300);

        // Assertion: It should clamp to 200
        assertEquals("Percentage should not go above 200",
                200, TextSizePreferences.getTextSizePercent(context));
    }

    // --- Font Scale Application (Activity Context) ---

    @Test
    public void testApply_whenDisabled_usesDefaultScale() {
        // Setup: Force the Activity's font scale to something random (e.g., 1.5)
        Configuration config = activity.getResources().getConfiguration();
        config.fontScale = 1.5f;
        activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());

        // Action: Run apply() while text sizing is disabled (default state)
        boolean didChange = TextSizePreferences.apply(activity);

        // Assertion: It should revert the scale back to 1.0f (100%) and return true because it made a change
        assertTrue("Apply should return true because the font scale was changed", didChange);
        assertEquals("Font scale should be reset to 1.0f",
                1.0f, activity.getResources().getConfiguration().fontScale, 0.001f);
    }

    @Test
    public void testApply_whenEnabled_usesCustomScale() {
        // Setup: Enable custom sizing and set it to 150%
        TextSizePreferences.setTextSizeEnabled(activity, true);
        TextSizePreferences.setTextSizePercent(activity, 150);

        // Action
        boolean didChange = TextSizePreferences.apply(activity);

        // Assertion: It should update the Activity's configuration to 1.5f
        assertTrue("Apply should return true because the font scale was changed", didChange);
        assertEquals("Font scale should be set to 1.5f",
                1.5f, activity.getResources().getConfiguration().fontScale, 0.001f);
    }

    @Test
    public void testApply_whenNoChangeNeeded_returnsFalse() {
        // Setup: Enable and set to 120%
        TextSizePreferences.setTextSizeEnabled(activity, true);
        TextSizePreferences.setTextSizePercent(activity, 120);

        // Action 1: Apply it the first time (this will update the scale to 1.2f)
        TextSizePreferences.apply(activity);

        // Action 2: Apply it a second time
        boolean didChangeSecondTime = TextSizePreferences.apply(activity);

        // Assertion: The math check `Math.abs(configuration.fontScale - targetScale) < 0.001f`
        // should trigger and return false to prevent redundant activity recreations
        assertFalse("Apply should return false if the font scale is already correct", didChangeSecondTime);
        assertEquals("Font scale should still be 1.2f",
                1.2f, activity.getResources().getConfiguration().fontScale, 0.001f);
    }
}