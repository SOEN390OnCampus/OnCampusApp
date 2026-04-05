package com.example.oncampusapp;

import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {

    private ActivityController<MainActivity> controller;
    private MainActivity activity;

    @Before
    public void setUp() {
        // Build the activity but use the controller so we can manually trigger lifecycle events
        controller = Robolectric.buildActivity(MainActivity.class);

        // Drive the activity through onCreate, onStart, and onResume
        activity = controller.create().start().resume().get();
    }

    @After
    public void tearDown() {
        // Clean up the activity to prevent memory leaks in the test environment
        controller.destroy();
    }

    @Test
    public void testActivity_isCreatedSuccessfully() {
        // Assertion: If EdgeToEdge or setContentView failed, this would be null or have crashed
        assertNotNull("Activity should be fully initialized", activity);
        assertFalse("Activity should not be finishing", activity.isFinishing());
    }

    @Test
    public void testOnCreate_bindsMainRootView() {
        // Setup: Find the root view that WindowInsetsCompat is applied to
        View mainView = activity.findViewById(R.id.main);

        // Assertion: Verify the layout was inflated properly and the ID exists
        assertNotNull("The root view with ID 'main' should exist in activity_main.xml", mainView);
    }

    @Test
    public void testOnResume_executesWithoutCrashing() {
        // Action: Force the activity to pause and resume again to trigger your onResume block
        controller.pause().resume();

        // Assertion: Verify the activity survived the TextSizePreferences check
        assertNotNull("Activity should still exist after onResume", activity);
        assertFalse("Activity should not be finishing after onResume", activity.isFinishing());
    }
}