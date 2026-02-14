package com.example.oncampusapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

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


        Intent serviceIntent = new Intent(context, LocationTrackingService.class);

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            ContextCompat.startForegroundService(context, serviceIntent);
        } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            context.stopService(serviceIntent);
        }

    }
}