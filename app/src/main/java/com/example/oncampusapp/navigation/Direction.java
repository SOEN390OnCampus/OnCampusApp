package com.example.oncampusapp.navigation;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Represents a navigation step used by the UI when displaying route directions.
 */
public class Direction {

    private String instructions;
    private String distance;
    private String duration;
    private RouteTravelMode travelMode;

    private List<LatLng> points;

    private LatLng transitDepartureStop;

    private LatLng transitArrivalStop;

    public Direction(String instructions, String distance, String duration,
                     RouteTravelMode travelMode, List<LatLng> points) {
        this.instructions = instructions;
        this.distance = distance;
        this.duration = duration;
        this.travelMode = travelMode;
        this.points = points;
    }

    public void setTransitStops(LatLng departure, LatLng arrival) {
        this.transitDepartureStop = departure;
        this.transitArrivalStop = arrival;
    }


    public LatLng getTransitDepartureStop() {
        return transitDepartureStop;
    }

    public LatLng getTransitArrivalStop() {
        return transitArrivalStop;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    // Getters
    public String getInstructions() {
        return instructions;
    }
    public String getDistance() {
        return distance;
    }
    public String getDuration() {
        return duration;
    }
    public RouteTravelMode getTravelMode() {
        return travelMode;
    }
}
