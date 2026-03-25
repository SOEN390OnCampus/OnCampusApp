package com.example.oncampusapp;

public class IndoorNode {

    private String  id;
    private String  label;
    private String  type;
    private String  buildingId;
    private String  floor;
    private float   x;
    private float   y;
    private boolean accessible;

    public IndoorNode() {}

    /** Legacy constructor — keeps existing search-pin flow working. */
    public IndoorNode(String label, float x, float y) {
        this.label = label;
        this.x     = x;
        this.y     = y;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder {
        private String  id;
        private String  label;
        private String  type;
        private String  buildingId;
        private String  floor;
        private float   x;
        private float   y;
        private boolean accessible = true;

        public Builder id(String id)                   { this.id = id;                   return this; }
        public Builder label(String label)             { this.label = label;             return this; }
        public Builder type(String type)               { this.type = type;               return this; }
        public Builder buildingId(String buildingId)   { this.buildingId = buildingId;   return this; }
        public Builder floor(String floor)             { this.floor = floor;             return this; }
        public Builder x(float x)                      { this.x = x;                     return this; }
        public Builder y(float y)                      { this.y = y;                     return this; }
        public Builder accessible(boolean accessible)  { this.accessible = accessible;   return this; }

        public IndoorNode build() {
            IndoorNode node = new IndoorNode();
            node.id         = id;
            node.label      = label;
            node.type       = type;
            node.buildingId = buildingId;
            node.floor      = floor;
            node.x          = x;
            node.y          = y;
            node.accessible = accessible;
            return node;
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String  getId()         { return id; }
    public String  getLabel()      { return label; }
    public String  getType()       { return type; }
    public String  getBuildingId() { return buildingId; }
    public String  getFloor()      { return floor; }
    public float   getX()          { return x; }
    public float   getY()          { return y; }
    public boolean isAccessible()  { return accessible; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(String id)                      { this.id = id; }
    public void setLabel(String label)                { this.label = label; }
    public void setType(String type)                  { this.type = type; }
    public void setBuildingId(String buildingId)      { this.buildingId = buildingId; }
    public void setFloor(String floor)                { this.floor = floor; }
    public void setX(float x)                         { this.x = x; }
    public void setY(float y)                         { this.y = y; }
    public void setAccessible(boolean accessible)     { this.accessible = accessible; }

    /**
     * Returns the floor menu ID used by IndoorMapActivity.
     * Handles the MB-S2 special case where buildingId ends in "-S2".
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
