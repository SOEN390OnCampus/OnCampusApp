package com.example.oncampusapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AccessibilityPreferencesTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Reset before each test
        AccessibilityPreferences.setReducedMobilityEnabled(context, false);
    }

    @Test
    public void defaultValue_isFalse() {
        assertFalse(AccessibilityPreferences.isReducedMobilityEnabled(context));
    }

    @Test
    public void setTrue_returnsTrue() {
        AccessibilityPreferences.setReducedMobilityEnabled(context, true);
        assertTrue(AccessibilityPreferences.isReducedMobilityEnabled(context));
    }

    @Test
    public void setFalse_returnsFalse() {
        AccessibilityPreferences.setReducedMobilityEnabled(context, true);
        AccessibilityPreferences.setReducedMobilityEnabled(context, false);
        assertFalse(AccessibilityPreferences.isReducedMobilityEnabled(context));
    }

    @Test
    public void toggle_persistsCorrectly() {
        AccessibilityPreferences.setReducedMobilityEnabled(context, true);
        assertTrue(AccessibilityPreferences.isReducedMobilityEnabled(context));

        AccessibilityPreferences.setReducedMobilityEnabled(context, false);
        assertFalse(AccessibilityPreferences.isReducedMobilityEnabled(context));

        AccessibilityPreferences.setReducedMobilityEnabled(context, true);
        assertTrue(AccessibilityPreferences.isReducedMobilityEnabled(context));
    }
}