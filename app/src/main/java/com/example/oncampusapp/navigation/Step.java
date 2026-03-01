package com.example.oncampusapp.navigation;


import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Step is a single part of a route. (e.g. Take bus to go to ...)
 */
public class Step {
    private RouteTravelMode travelMode;
    private List<LatLng> points;
    private TransitDetails transitDetails;
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
}

