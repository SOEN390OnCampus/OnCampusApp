package com.example.oncampusapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationScheduler {

    public static void scheduleClassNotification(Context context, JSONObject nextEvent) {
        if (nextEvent == null) return;

        try {
            String title = nextEvent.optString("summary", "Class");
            String rawLocation = nextEvent.optString("location", "");
            String description = nextEvent.optString("description", "");

            // 1. SMART LOCATION PARSING (Delegated to extracted class)
            String displayLocation = LocationParser.parseSmartLocation(title, rawLocation, description);

            // 2. ALARM TIMING
            String dateTimeStr = nextEvent.getJSONObject("start").getString("dateTime");
            SimpleDateFormat exactTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            Date eventDate = exactTimeFormat.parse(dateTimeStr);
            if (eventDate == null) return;

            long alarmTimeMillis = eventDate.getTime() - (15 * 60 * 1000);

            if (alarmTimeMillis > System.currentTimeMillis()) {
                Intent intent = new Intent(context, NotificationReceiver.class);
                intent.putExtra("event_title", title);
                intent.putExtra("event_location", displayLocation);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
                    Log.d("AlarmSetup", "Notification scheduled for: " + displayLocation);
                }
            }

        } catch (Exception e) {
            Log.e("NotificationScheduler", "Error scheduling notification", e);
        }
    }
}