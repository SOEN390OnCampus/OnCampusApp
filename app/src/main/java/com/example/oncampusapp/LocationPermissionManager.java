package com.example.oncampusapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationPermissionManager {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private final MapsActivity activity;
    private GoogleMap mMap;
    private final ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher;

    public LocationPermissionManager(MapsActivity activity,
                                     ActivityResultLauncher<String[]> permissionsLauncher) {
        this.activity = activity;
        this.requestMultiplePermissionsLauncher = permissionsLauncher;
    }

    public void setMap(GoogleMap map) {
        this.mMap = map;
    }

    @SuppressWarnings("MissingPermission")
    public void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    public void launchPermissionRequest() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)  != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsToRequest.isEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    public void moveMapToLocation(LatLng location, float zoom) {
        activity.mapIdlingResource.increment();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(location).zoom(zoom).tilt(0f).build()),
                new GoogleMap.CancelableCallback() {
                    @Override public void onFinish() {
                        mMap.setOnMapLoadedCallback(() -> {
                            if (!activity.mapIdlingResource.isIdleNow())
                                activity.mapIdlingResource.decrement();
                        });
                    }
                    @Override public void onCancel() {
                        if (!activity.mapIdlingResource.isIdleNow())
                            activity.mapIdlingResource.decrement();
                    }
                });
    }

    public static void logPermissionResult(String permission, boolean granted) {
        Log.d("PermissionManager", permission + " granted: " + granted);
    }
}
