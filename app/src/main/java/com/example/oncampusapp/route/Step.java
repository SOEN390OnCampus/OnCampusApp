package com.example.oncampusapp.route;


import com.google.android.gms.maps.model.LatLng;

import java.util.List;

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

