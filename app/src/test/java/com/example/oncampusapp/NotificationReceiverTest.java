package com.example.oncampusapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowNotificationManager;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class NotificationReceiverTest {

    @Test
    public void testOnReceive_ExtractsDataAndTriggersNotification() {
        // 1. Get Robolectric's simulated Android Context
        Context context = ApplicationProvider.getApplicationContext();

        // 2. Setup the Intent with our mock class data
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("event_title", "COMP 346");
        intent.putExtra("event_location", "H-110");

        // 3. Execute the receiver
        NotificationReceiver receiver = new NotificationReceiver();
        receiver.onReceive(context, intent);

        // 4. Ask the simulated OS if a notification was fired
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ShadowNotificationManager shadowManager = Shadows.shadowOf(notificationManager);

        // Verify exactly one notification is currently active in the system tray
        assertEquals("One notification should be fired", 1, shadowManager.size());
    }
}