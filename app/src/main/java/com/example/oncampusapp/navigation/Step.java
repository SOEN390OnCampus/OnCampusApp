package com.example.oncampusapp.navigation;


import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Represents a segment of a route. (e.g. Take bus to go to ...)
 */
public class Step {
    private RouteTravelMode travelMode;
    private List<LatLng> points;
    private TransitDetails transitDetails;
    private String instructions;
    private String distance;
    private String duration;

    public RouteTravelMode getTravelMode() {
        return travelMode;
    }
    public void setTravelMode(RouteTravelMode travelMode) {
        this.travelMode = travelMode;
    }
    public List<LatLng> getPoints() {
        return points;
    }
    public void setPoints(List<LatLng> points) {
        this.points = points;
    }
    public TransitDetails getTransitDetails() {
        return transitDetails;
    }
    public void setTransitDetails(TransitDetails transitDetails) {
        this.transitDetails = transitDetails;
    }


    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}

