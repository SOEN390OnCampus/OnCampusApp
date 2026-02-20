package com.example.oncampusapp;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Building {
    private String id;

    public String name;
    List<LatLng> polygon;

    boolean currentlyInside;

    public Building(String id, String name, List<LatLng> polygon) {
        this.id = id;
        this.name = name;
        this.polygon = polygon;
        currentlyInside = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LatLng getCenter() {
        if (polygon == null || polygon.isEmpty()) {
            return null;
        }
        double latSum = 0;
        double lngSum = 0;
        for (LatLng point : polygon) {
            latSum += point.latitude;
            lngSum += point.longitude;
        }
        return new LatLng(latSum / polygon.size(), lngSum / polygon.size());
    }
}
