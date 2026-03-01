package com.example.oncampusapp.navigation;

import com.google.android.gms.maps.model.LatLng;

/**
 * Transit details contains all the details needed to display a transit route on the map.
 * Transit details only exist if the step is a transit step.
 */
public class TransitDetails {
    private String departureStopName;
    private LatLng departureStopLocation;
    private String arrivalStop;
    private LatLng arrivalStopLocation;
    private String departureTime;
    private String arrivalTime;
    private TransitVehicleType vehicleType;
    public String getDepartureStopName() {
        return departureStopName;
    }
    public void setDepartureStopName(String departureStopName) {
        this.departureStopName = departureStopName;
    }
    public LatLng getDepartureStopLocation() {
        return departureStopLocation;
    }
    public void setDepartureStopLocation(LatLng departureStopLocation) {
        this.departureStopLocation = departureStopLocation;
    }
    public String getArrivalStop() {
        return arrivalStop;
    }
    public void setArrivalStop(String arrivalStop) {
        this.arrivalStop = arrivalStop;
    }
    public LatLng getArrivalStopLocation() {
        return arrivalStopLocation;
    }
    public void setArrivalStopLocation(LatLng arrivalStopLocation) {
        this.arrivalStopLocation = arrivalStopLocation;
    }
    public String getDepartureTime() {
        return departureTime;
    }
    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }
    public String getArrivalTime() {
        return arrivalTime;
    }
    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
    public TransitVehicleType getVehicleType() {
        return vehicleType;
    }
    public void setVehicleType(TransitVehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }
}
