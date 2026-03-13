package com.example.oncampusapp;

import java.util.List;

/**
 * This class is used for the indoor map, specifically the selection UI that leads up to the Indoor Map
 */
public class BuildingFloorData {
    private final String id;
    private final String fullName;
    private final List<Floor> floors;

    public BuildingFloorData(String id, String fullName, List<Floor> floors) {
        this.id = id;
        this.fullName = fullName;
        this.floors = floors;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public List<Floor> getFloors() { return floors; }

    public record Floor(String id, String name) {
    }
}
