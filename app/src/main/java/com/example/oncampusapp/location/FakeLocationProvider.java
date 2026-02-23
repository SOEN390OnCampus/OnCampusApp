package com.example.oncampusapp.location;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class FakeLocationProvider implements ILocationProvider {
    private final FusedLocationProviderClient client;
    private final Location fakeLocation;

    public FakeLocationProvider(Context context) {
        client = LocationServices.getFusedLocationProviderClient(context);

        fakeLocation = new Location("fake");
    }

    @Override
    public void setFakeLocation(double lat, double lng) {
        fakeLocation.setLatitude(lat);
        fakeLocation.setLongitude(lng);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public Task<Location> getLastLocation() {
        return Tasks.forResult(fakeLocation);
    }

    public void removeLocationUpdates(LocationCallback locationCallback) {
        client.removeLocationUpdates(locationCallback);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void requestLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback, Looper looper) {
        client.requestLocationUpdates(locationRequest, locationCallback, looper);
    }
}