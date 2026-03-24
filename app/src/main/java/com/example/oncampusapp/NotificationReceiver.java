package com.example.oncampusapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "CLASS_REMINDER_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieve the class details passed from the AlarmManager
        String eventTitle = intent.getStringExtra("event_title");
        String eventLocation = intent.getStringExtra("event_location");

        if (eventTitle == null) eventTitle = "Upcoming Class";
        if (eventLocation == null) eventLocation = "Check schedule for details";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0+ requires a Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Class Reminders",
                    NotificationManager.IMPORTANCE_HIGH // High importance makes it pop up on screen
            );
            channel.setDescription("Notifications for upcoming classes");
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = (int) System.currentTimeMillis();

        // For generating directions
        Intent directionsIntent = new Intent(context, MapsActivity.class);
        directionsIntent.putExtra("OPEN_DIRECTIONS", true);
        directionsIntent.putExtra("notification_id", notificationId);
        directionsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                directionsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the actual notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon if you have one
                .setContentTitle("Class Starting Soon: " + eventTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText("Location: " + eventLocation)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Directions", pendingIntent)
                .setAutoCancel(true);

        // Show the notification (Use a unique ID based on time so they don't overwrite each other)
        notificationManager.notify(notificationId, builder.build());    }
}