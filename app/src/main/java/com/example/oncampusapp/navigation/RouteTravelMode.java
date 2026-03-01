package com.example.oncampusapp.navigation;

public enum RouteTravelMode {
    WALK("WALK"),
    DRIVE("DRIVE"),
    TRANSIT("TRANSIT");
    private final String value;
    RouteTravelMode(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    public static RouteTravelMode fromString(String value) {
        for (RouteTravelMode mode : RouteTravelMode.values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return WALK; // default fallback
    }
}
