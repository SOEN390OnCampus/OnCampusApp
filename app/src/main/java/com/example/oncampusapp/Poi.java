package com.example.oncampusapp;

public class Poi {
    private final String name;
    private final String category;
    private final double latitude;
    private final double longitude;
    private final double distanceKm;

    private final String status;
    public Poi(String name, String category, double latitude, double longitude, double distanceKm, String status) {
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public String getStatus() {
        return status;
    }
}