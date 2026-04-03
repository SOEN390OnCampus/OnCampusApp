package com.example.oncampusapp;

public class Poi {
    private final String name;
    private final String category;
    private final double latitude;
    private final double longitude;
    private final double distanceKm;

    public Poi(String name, String category, double latitude, double longitude, double distanceKm) {
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
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
}