package com.example.oncampusapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Geofence;

import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    // ...
    @Override
    public void onReceive(Context context, Intent intent) {

        GeofencingEvent event = GeofencingEvent.fromIntent(intent);

        if (event == null || event.hasError()) {
            Log.e("GeofenceBR", "Error receiving geofence event");
            return;
        }

        int transition = event.getGeofenceTransition();
        
        Location location = event.getTriggeringLocation();

        /// for loop in case multiple geofences are overlapping

        for (Geofence geofence : event.getTriggeringGeofences()) {


            String id = geofence.getRequestId();

            List<LatLng> polygon = MapsActivity.buildingPolygons.get(id);

            if (polygon == null) {
                Log.d("GeofenceBR", "Polygon not found for " + id);
                continue;
            }

            LatLng userLocation = new LatLng(
                    event.getTriggeringLocation().getLatitude(),
                    event.getTriggeringLocation().getLongitude()
            );

            boolean isUserActuallyInside = PolyUtil.containsLocation(userLocation, polygon, true);

            if (isUserActuallyInside) {
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.d("GeofenceBR", "Entered building ID: " + id);
                    sendNotification(context, "Notification Works For " + id);
                } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    Log.d("GeofenceBR", "Exited building");
                }
            }
        }
    }

    private void sendNotification(Context context, String message) {
        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(context, "GEOFENCE_CHANNEL")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("OnCampus App")
                        .setContentText(message)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);

        androidx.core.app.NotificationManagerCompat notificationManager =
                androidx.core.app.NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }

    }
}