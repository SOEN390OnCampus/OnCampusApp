package com.example.oncampusapp;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityActivityTest {

    private AccessibilityActivity activity;

    @Before
    public void setUp() {
        // Builds, creates, and makes the activity visible, triggering onCreate()
        activity = Robolectric.buildActivity(AccessibilityActivity.class)
                .create()
                .visible()
                .get();
    }

    @Test
    public void testActivityIsNotNull() {
        assertNotNull("Activity should not be null", activity);
    }

    @Test
    public void testBackButton_finishesActivity() {
        ImageView backButton = activity.findViewById(R.id.btn_back_accessibility);
        backButton.performClick();

        assertTrue("Activity should be finishing after back button click", activity.isFinishing());
    }

    // --- Bottom Navigation Tests ---

    @Test
    public void testBottomNav_clickHome_startsMapsActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent expectedIntent = new Intent(activity, MapsActivity.class);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Should start a new activity", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        assertTrue(activity.isFinishing());
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
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testBottomNav_clickSettings_startsSettingsActivity() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_settings);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent expectedIntent = new Intent(activity, SettingsActivity.class);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull(actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        assertTrue(activity.isFinishing());
    }

    // --- Interactive Controls Tests ---

    @Test
    public void testReducedMobilityButton_togglesStateOnClick() {
        View reducedMobilityButton = activity.findViewById(R.id.btn_reduced_mobility);

        boolean initialState = reducedMobilityButton.isSelected();
        reducedMobilityButton.performClick();
        boolean newState = reducedMobilityButton.isSelected();

        // Updated to use assertNotEquals
        assertNotEquals("Button selection state should toggle", initialState, newState);
    }

    @Test
    public void testHighContrastSwitch_updatesStateOnClick() {
        SwitchMaterial highContrastSwitch = activity.findViewById(R.id.switch_high_contrast);

        boolean initialState = highContrastSwitch.isChecked();
        highContrastSwitch.performClick();

        // Updated to use assertNotEquals
        assertNotEquals("High contrast switch state should toggle", initialState, highContrastSwitch.isChecked());

        // Note: Full verification would check HighContrastPreferences, but Robolectric
        // handles standard SharedPreferences automatically if your custom utility uses them.
    }

    @Test
    public void testTextSizeSwitch_disablesZoomControlsWhenOff() {
        SwitchMaterial textSizeSwitch = activity.findViewById(R.id.switch_text_size_control);
        View zoomOut = activity.findViewById(R.id.btn_text_zoom_out);
        View zoomIn = activity.findViewById(R.id.btn_text_zoom_in);

        // Turn it off
        if (textSizeSwitch.isChecked()) {
            textSizeSwitch.performClick();
        }

        assertFalse("Zoom out should be disabled", zoomOut.isEnabled());
        assertFalse("Zoom in should be disabled", zoomIn.isEnabled());
        // Verify alpha is set to 0.45f
        assertEquals(0.45f, zoomOut.getAlpha(), 0.001);
        assertEquals(0.45f, zoomIn.getAlpha(), 0.001);
    }

    @Test
    public void testTextSizeSwitch_enablesZoomControlsWhenOn() {
        SwitchMaterial textSizeSwitch = activity.findViewById(R.id.switch_text_size_control);
        View zoomOut = activity.findViewById(R.id.btn_text_zoom_out);
        View zoomIn = activity.findViewById(R.id.btn_text_zoom_in);

        // Turn it on
        if (!textSizeSwitch.isChecked()) {
            textSizeSwitch.performClick();
        }

        assertTrue("Zoom out should be enabled", zoomOut.isEnabled());
        assertTrue("Zoom in should be enabled", zoomIn.isEnabled());
        // Verify alpha is set to 1.0f
        assertEquals(1.0f, zoomOut.getAlpha(), 0.001);
        assertEquals(1.0f, zoomIn.getAlpha(), 0.001);
    }

    @Test
    public void testZoomInButton_increasesTextSize() {
        SwitchMaterial textSizeSwitch = activity.findViewById(R.id.switch_text_size_control);
        View zoomIn = activity.findViewById(R.id.btn_text_zoom_in);
        TextView textSizeValue = activity.findViewById(R.id.txt_text_size_value);

        // Ensure text size control is enabled
        if (!textSizeSwitch.isChecked()) {
            textSizeSwitch.performClick();
        }

        String initialText = textSizeValue.getText().toString();
        int initialValue = Integer.parseInt(initialText.replace("%", ""));

        // Prevent test from failing if we are already at MAX size
        if (initialValue < 200) {
            zoomIn.performClick();

            // Assert that the text value was updated (which happens right before recreate() is called)
            int expectedValue = initialValue + 10; // TEXT_SIZE_STEP is 10
            assertEquals(expectedValue + "%", textSizeValue.getText().toString());
        }
    }
}