package com.example.oncampusapp;

import android.graphics.Color;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

public class GeofenceManager {

    private final Context context;
    private final GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    public GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        geofencingClient = LocationServices.getGeofencingClient(this.context);
    }

    /** This method allows the addition of geofence locations, so coordinates and radius around said
    coordinates */
    public void addGeofence(String id, double latitude, double longitude, float radius) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId(id)
                /** sets the area of the geofence */
                .setCircularRegion(latitude,longitude, radius)
                /** sets transition type when entering and exiting geofence */
                .setTransitionTypes(enterAndExit())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return; // permission not granted
        }

        geofencingClient.addGeofences(request, getGeofencePendingIntent());
    }

    private int enterAndExit() {
        return Geofence.GEOFENCE_TRANSITION_ENTER |
                Geofence.GEOFENCE_TRANSITION_EXIT;
    }
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        return geofencePendingIntent;
    }

    /** Experimental feature, to visualise the geofence radius **/
    protected CircleOptions drawGeofenceCircle(LatLng center, float radius) {
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(radius) // meters
                .strokeWidth(4f)
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF); // transparent blue

        return circleOptions;
    }


}
