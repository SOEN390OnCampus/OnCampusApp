package com.example.oncampusapp;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Building {
    private String id;

    private String name;



    private List<LatLng> polygon;

    private boolean currentlyInside;

    public Building(String id, String name, List<LatLng> polygon) {
        this.id = id;
        this.setName(name);
        this.setPolygon(polygon);
        setCurrentlyInside(false);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LatLng getCenter() {
        if (getPolygon() == null || getPolygon().isEmpty()) {
            return null;
        }
        double latSum = 0;
        double lngSum = 0;
        for (LatLng point : getPolygon()) {
            latSum += point.latitude;
            lngSum += point.longitude;
        }
        return new LatLng(latSum / getPolygon().size(), lngSum / getPolygon().size());
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LatLng> getPolygon() {
        return polygon;
    }

    public void setPolygon(List<LatLng> polygon) {
        this.polygon = polygon;
    }

    public boolean isCurrentlyInside() {
        return currentlyInside;
    }

    public void setCurrentlyInside(boolean currentlyInside) {
        this.currentlyInside = currentlyInside;
    }
}
