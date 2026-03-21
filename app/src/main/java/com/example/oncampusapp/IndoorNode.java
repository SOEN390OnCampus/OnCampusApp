package com.example.oncampusapp;

public class IndoorNode {
    private String id;
    private String label;
    private String type;
    private String buildingId;
    private String floor;
    private float x;
    private float y;
    private boolean accessible;

    public IndoorNode() {}

    /** Legacy constructor - keeps existing search-pin flow working. */
    public IndoorNode(String label, float x, float y) {
        this.label = label;
        this.x = x;
        this.y = y;
    }

    /** Full constructor used by IndoorGraph when loading from JSON. */
    public IndoorNode(String id, String label, String type,
                      String buildingId, String floor,
                      float x, float y, boolean accessible) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.buildingId = buildingId;
        this.floor = floor;
        this.x = x;
        this.y = y;
        this.accessible = accessible;
    }

    public String getId()         { return id; }
    public String getLabel()      { return label; }
    public String getType()       { return type; }
    public String getBuildingId() { return buildingId; }
    public String getFloor()      { return floor; }
    public float  getX()          { return x; }
    public float  getY()          { return y; }
    public boolean isAccessible() { return accessible; }


    public void setId(String id)               { this.id = id; }
    public void setLabel(String label)         { this.label = label; }
    public void setType(String type)           { this.type = type; }
    public void setBuildingId(String buildingId){ this.buildingId = buildingId; }
    public void setFloor(String floor)         { this.floor = floor; }
    public void setX(float x)                  { this.x = x; }
    public void setY(float y)                  { this.y = y; }
    public void setAccessible(boolean accessible){ this.accessible = accessible; }


    /**
     * Returns the floor menu ID used by IndoorMapActivity.
     * Handles the MB-S2 special case where buildingId ends in "-S2"
     * but the floor_menu.json entry is "S2".
     */
    public String getFloorMenuId() {
        if (buildingId != null && buildingId.endsWith("-S2")) return "S2";
        return floor != null ? floor : "";
    }

    /**
     * Returns the root building ID (strips sub-building suffix, e.g. "MB-S2" → "MB").
     */
    public String getRootBuildingId() {
        if (buildingId == null) return "";
        int dash = buildingId.indexOf('-');
        return dash >= 0 ? buildingId.substring(0, dash) : buildingId;
    }
}
