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
    public void testNotificationAppearsInSystemTray() throws InterruptedException {
        // Manually fire the BroadcastReceiver (simulating the AlarmManager going off)
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("event_title", "COMP 346");
        intent.putExtra("event_location", "Henry F. Hall Building (H) - Room 937");

        NotificationReceiver receiver = new NotificationReceiver();
        receiver.onReceive(context, intent);

        // Open the Android Notification Shade
        device.openNotification();

        // Wait up to 5 seconds for the notification title to appear on the screen
        String expectedTitle = "Class Starting Soon: COMP 346";
        UiObject2 notificationTitle = device.wait(Until.findObject(By.text(expectedTitle)), 5000);

        // Assert that the notification was found
        assertNotNull("The notification should be visible in the system tray", notificationTitle);

        Thread.sleep(3000);

        // Clean up by closing the notification shade
        device.pressBack();
    }
}