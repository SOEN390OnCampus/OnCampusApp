package com.example.oncampusapp;

import com.example.oncampusapp.location.FusedLocationProvider;
import com.example.oncampusapp.location.ILocationProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
// This class is used to track the location of the user, specifically when they are inside a building
public class LocationTrackingService extends Service {

    private ILocationProvider fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TrackingService", "Service started, requesting location updates");
        startForegroundNotification();

        // Retrieve the globally injected provider
        OnCampusApplication app = (OnCampusApplication) getApplication();
        fusedLocationClient = app.getLocationProvider();

        // Safety check in case the service starts before the activity initializes it
        if (fusedLocationClient == null) {
            fusedLocationClient = new FusedLocationProvider(this);
            app.setLocationProvider(fusedLocationClient);
        }

        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }

    // Starts sending notifications
    private void startForegroundNotification() {
        String channelId = "TRACKING_CHANNEL";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("OnCampus Tracking")
                .setContentText("Checking if you're inside buildings")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
    }

    // Starts constant checks on the users location while they are inside a campus building
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000
        ).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (location == null) return;

                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                checkUserInsideBuildings(userLatLng);
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void sendNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "GEOFENCE_CHANNEL")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("OnCampus App")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // Checks whether user is still inside the building
    private void checkUserInsideBuildings(LatLng userLocation) {
        for (Building building : MapsActivity.buildingsMap.values()) {
            if (building.polygon == null) continue;

            boolean userWasInside = building.currentlyInside;
            boolean userIsCurrentlyInside = PolyUtil.containsLocation(userLocation, building.polygon, true);

            if (!userWasInside && userIsCurrentlyInside) {
                Log.d("Building", "Entered " + building.name);
                sendNotification("You have entered " + building.name);
                building.currentlyInside = true;
            }

            else if (userWasInside && !userIsCurrentlyInside) {
                Log.d("Building", "Exited " + building.name);
                sendNotification("You have exited " + building.name);
                building.currentlyInside = false;
            }
        }
    }
}

