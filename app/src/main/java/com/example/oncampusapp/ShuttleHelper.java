package com.example.oncampusapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Helper class for Concordia Shuttle Service functionality
 * Handles shuttle-specific constants, routing, timetable access, and map markers
 */
public class ShuttleHelper {
    
    // Concordia Shuttle Stop Locations
    public static final LatLng SHUTTLE_STOP_SGW = new LatLng(45.497163, -73.578535); // SGW Campus
    public static final LatLng SHUTTLE_STOP_LOY = new LatLng(45.458424, -73.638369); // Loyola Campus
    
    // Concordia Shuttle Timetable URL (Winter schedule)
    private static final String SHUTTLE_TIMETABLE_URL = "https://www.concordia.ca/maps/shuttle-bus.html";
    
    /**
     * Opens the Concordia Shuttle Bus timetable in the default browser
     */
    public static void openTimetable(Context context) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SHUTTLE_TIMETABLE_URL));
            context.startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open shuttle timetable", Toast.LENGTH_SHORT).show();
            Log.e("ShuttleHelper", "Error opening shuttle timetable", e);
        }
    }
    
    /**
     * Shows shuttle stop markers on the map
     * @param map The GoogleMap instance to add markers to
     * @param existingMarkers Array containing existing marker references [SGW, Loyola] to remove before adding new ones
     * @return Array containing new marker references [SGW, Loyola]
     */
    public static Marker[] showShuttleStops(GoogleMap map, Marker[] existingMarkers) {
        if (map == null) return new Marker[]{null, null};
        
        // Remove old shuttle markers if they exist
        if (existingMarkers != null) {
            if (existingMarkers[0] != null) existingMarkers[0].remove();
            if (existingMarkers[1] != null) existingMarkers[1].remove();
        }

        // Add shuttle stop markers with distinctive appearance
        Marker sgwMarker = map.addMarker(new MarkerOptions()
                .position(SHUTTLE_STOP_SGW)
                .title("SGW Shuttle Stop")
                .snippet("Tap for shuttle timetable")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        Marker loyMarker = map.addMarker(new MarkerOptions()
                .position(SHUTTLE_STOP_LOY)
                .title("Loyola Shuttle Stop")
                .snippet("Tap for shuttle timetable")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        
        return new Marker[]{sgwMarker, loyMarker};
    }
    
    /**
     * Hides shuttle stop markers from the map
     * @param markers Array containing marker references [SGW, Loyola] to remove
     */
    public static void hideShuttleStops(Marker[] markers) {
        if (markers == null) return;
        
        if (markers[0] != null) {
            markers[0].remove();
            markers[0] = null;
        }
        if (markers[1] != null) {
            markers[1].remove();
            markers[1] = null;
        }
    }
    
    /**
     * Checks if a marker is a shuttle stop marker
     * @param marker The marker to check
     * @return true if the marker is a shuttle stop
     */
    public static boolean isShuttleStopMarker(Marker marker) {
        if (marker == null || marker.getTitle() == null) return false;
        String title = marker.getTitle();
        return title.equals("SGW Shuttle Stop") || title.equals("Loyola Shuttle Stop");
    }

    /**
     * Determines if two locations are on the same Concordia campus.
     * Uses the midpoint between SGW and Loyola as the boundary.
     * @param point1 First location
     * @param point2 Second location
     * @param sgwCoords SGW campus center coordinates
     * @param loyCoords Loyola campus center coordinates
     * @return true if both points are closer to the same campus
     */
    public static boolean isSameCampus(LatLng point1, LatLng point2, LatLng sgwCoords, LatLng loyCoords) {
        if (point1 == null || point2 == null) return false;
        boolean point1AtSGW = distanceBetween(point1, sgwCoords) < distanceBetween(point1, loyCoords);
        boolean point2AtSGW = distanceBetween(point2, sgwCoords) < distanceBetween(point2, loyCoords);
        return point1AtSGW == point2AtSGW;
    }

    /** Simple Euclidean-style distance proxy (no Android SDK required) */
    private static double distanceBetween(LatLng a, LatLng b) {
        double dLat = a.latitude - b.latitude;
        double dLng = a.longitude - b.longitude;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }
}
