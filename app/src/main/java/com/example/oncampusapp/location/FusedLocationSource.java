package com.example.oncampusapp.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.oncampusapp.MapsActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.LocationSource;

public class FusedLocationSource implements LocationSource {
    private final ILocationProvider client;
    private final Context context; // Added this
    private OnLocationChangedListener mapLocationListener;
    private final LocationCallback locationCallback;

    public FusedLocationSource(Context context, ILocationProvider client) {
        this.context = context; // Store the context
        this.client = client;

        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null && mapLocationListener != null) {
                    mapLocationListener.onLocationChanged(locationResult.getLastLocation());
                }
            }
        };
    }

    @Override
    public void activate(@NonNull OnLocationChangedListener onLocationChangedListener) {
        this.mapLocationListener = onLocationChangedListener;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        // Use the passed 'context' here instead of MapsActivity.this
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            client.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void deactivate() {
        this.mapLocationListener = null;
        client.removeLocationUpdates(locationCallback);
    }
}