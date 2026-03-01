package com.example.oncampusapp.navigation;

/**
 * This covers all transit types that Google Route API would returns, we might not need all
 */
public enum TransitVehicleType {
    TRANSIT_VEHICLE_TYPE_UNSPECIFIED,
    BUS,
    CABLE_CAR,
    COMMUTER_TRAIN,
    FERRY,
    FUNICULAR,
    GONDOLA_LIFT,
    HEAVY_RAIL,
    HIGH_SPEED_TRAIN,
    INTERCITY_BUS,
    LONG_DISTANCE_TRAIN,
    METRO_RAIL,
    MONORAIL,
    OTHER,
    RAIL,
    SHARE_TAXI,
    SUBWAY,
    TRAM,
    TROLLEYBUS;
    public static TransitVehicleType fromString(String value) {
        try {
            return TransitVehicleType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return OTHER; // fallback for unknown values
        }
    }
}