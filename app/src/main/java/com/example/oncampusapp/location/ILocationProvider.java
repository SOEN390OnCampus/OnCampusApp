package com.example.oncampusapp.location;

import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.Task;

public interface ILocationProvider {
    void setFakeLocation(double lat, double lng);
    Task<Location> getLastLocation();
    void removeLocationUpdates(LocationCallback locationCallback);
    void requestLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback, Looper looper);
}
