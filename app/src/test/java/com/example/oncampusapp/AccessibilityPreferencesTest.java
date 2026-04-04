package com.example.oncampusapp;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityPreferencesTest {

    private Context context;
    private static final String PREFS_NAME = "OnCampusAccessibilityPrefs";

    @Before
    public void setUp() {
        // Grab the Robolectric-simulated application context
        context = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() {
        // Clear preferences after every test so they don't bleed into each other!
        // This ensures true test isolation.
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testIsReducedMobilityEnabled_defaultIsFalse() {
        // Action: Check the value before anything has been explicitly set
        boolean isEnabled = AccessibilityPreferences.isReducedMobilityEnabled(context);

        // Assertion: The default value defined in your class is false
        assertFalse("Default reduced mobility preference should be false", isEnabled);
    }

    @Test
    public void testSetReducedMobilityEnabled_setToTrue() {
        // Action: Update the preference to true
        AccessibilityPreferences.setReducedMobilityEnabled(context, true);

        // Assertion: Retrieve the value and verify it updated correctly
        boolean isEnabled = AccessibilityPreferences.isReducedMobilityEnabled(context);
        assertTrue("Preference should be true after setting to true", isEnabled);
    }

    @Test
    public void testSetReducedMobilityEnabled_setToFalse() {
        // Setup: Set it to true first to ensure a state change will occur
        AccessibilityPreferences.setReducedMobilityEnabled(context, true);

        // Action: Set it back to false
        AccessibilityPreferences.setReducedMobilityEnabled(context, false);

        // Assertion: Verify it flipped back correctly
        boolean isEnabled = AccessibilityPreferences.isReducedMobilityEnabled(context);
        assertFalse("Preference should be false after setting to false", isEnabled);
    }
}