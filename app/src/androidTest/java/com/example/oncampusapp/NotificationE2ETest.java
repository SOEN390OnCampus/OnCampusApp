package com.example.oncampusapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NotificationE2ETest {

    private UiDevice device;
    private Context context;

    // Automatically grant the notification permission for Android 13+ emulators
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS
    );

    @Before
    public void setUp() {
        // Initialize UI Automator
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testNotificationAppearsBothHeadsUpAndInTray() throws InterruptedException {
        // Manually fire the BroadcastReceiver (simulating the AlarmManager going off)
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("event_title", "COMP 346");
        intent.putExtra("event_location", "Henry F. Hall Building - Room 937");

        String expectedTitle = "Class Starting Soon: COMP 346";

        NotificationReceiver receiver = new NotificationReceiver();
        receiver.onReceive(context, intent);

        // --- 1. TEST THE HEADS-UP POP-UP ---
        // Wait for the heads-up banner to drop down
        UiObject2 headsUpTitle = device.wait(Until.findObject(By.text(expectedTitle)), 3000);
        assertNotNull("The heads-up notification pop-up should be visible on the screen", headsUpTitle);

        // Wait for the heads-up banner to slide back up and disappear
        boolean isGone = device.wait(Until.gone(By.text(expectedTitle)), 5000);
        assertTrue("The heads-up notification should disappear after a few seconds", isGone);


        // --- 2. TEST THE SYSTEM TRAY ---
        // open the Android Notification Shade
        device.openNotification();

        // Wait for the shade to fully pull down and find the text
        UiObject2 trayTitle = device.wait(Until.findObject(By.text(expectedTitle)), 3000);
        assertNotNull("The notification should be waiting in the pulled-down system tray", trayTitle);

        // Hold the shade open for 2 seconds so we can see it
        Thread.sleep(2000);

        // Clean up by closing the notification shade
        device.pressBack();
    }
}