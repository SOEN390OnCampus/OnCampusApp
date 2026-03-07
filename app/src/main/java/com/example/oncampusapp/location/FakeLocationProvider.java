package com.example.oncampusapp.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FakeLocationProvider implements ILocationProvider {
    private final FusedLocationProviderClient client;
    private final Location fakeLocation;
    private final Handler handler;
    private final Map<LocationCallback, Runnable> activeUpdates = new HashMap<>();

    @SuppressLint("MissingPermission")
    public FakeLocationProvider(Context context) {
        client = LocationServices.getFusedLocationProviderClient(context);
        fakeLocation = new Location("fake");
        handler = new Handler(Looper.getMainLooper());

        client.setMockMode(true).addOnFailureListener(e ->
            Log.e("FakeLocation", "Failed to enable mock mode", e)
        );
    }

    @SuppressLint("MissingPermission")
    @Override
    public void setFakeLocation(double lat, double lng) {
        fakeLocation.setLatitude(lat);
        fakeLocation.setLongitude(lng);
        fakeLocation.setAccuracy(5f);
        fakeLocation.setTime(System.currentTimeMillis());
        fakeLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        client.setMockLocation(fakeLocation);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public Task<Location> getLastLocation() {
        return Tasks.forResult(fakeLocation);
    }

    public void removeLocationUpdates(LocationCallback locationCallback) {
        // Stop the Google Client updates
        client.removeLocationUpdates(locationCallback);

        // Stop our custom Handler loop
        Runnable runnable = activeUpdates.remove(locationCallback);
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public void requestLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback, Looper looper) {
        // Prevent duplicate listeners for the same callback
        if (activeUpdates.containsKey(locationCallback)) {
            removeLocationUpdates(locationCallback);
        }

        long interval = locationRequest.getIntervalMillis();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                // Update timestamp each emission
                fakeLocation.setTime(System.currentTimeMillis());

                LocationResult result = LocationResult.create(Collections.singletonList(fakeLocation));

                // SEND FAKE LOCATION
                locationCallback.onLocationResult(result);

                // schedule next update
                handler.postDelayed(this, interval);
            }
        };

        // Store the reference and start
        activeUpdates.put(locationCallback, runnable);
        handler.post(runnable);
    }
}
