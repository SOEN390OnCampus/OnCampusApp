package com.example.oncampusapp;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;
import java.util.ArrayList;

public class CampusData {
    public static final String CAMPUS_SGW = "SGW";
    public static final String CAMPUS_LOYOLA = "LOYOLA";

    // SGW Campus Center
    public static final LatLng SGW_CENTER = new LatLng(45.496107243097704, -73.57725834380621);
    // Loyola Campus Center
    public static final LatLng LOYOLA_CENTER = new LatLng(45.457944, -73.640678);

    public static class Building {
        private final String name;
        private final String campusCode;
        private final LatLng position;
        private final List<LatLng> polygonCoordinates;
        private final String placeId;
        private final String buildingCode;

        public Building(String name, String campusCode, LatLng position, List<LatLng> polygonCoordinates, String buildingCode) {
            this.name = name;
            this.campusCode = campusCode;
            this.position = position;
            this.polygonCoordinates = polygonCoordinates;
            this.buildingCode = buildingCode;
            this.placeId = "PLACE_ID_" + buildingCode; // Temporary placeholder
        }

        // Getters
        public String getName() { return name; }
        public String getCampusCode() { return campusCode; }
        public LatLng getPosition() { return position; }
        public List<LatLng> getPolygonCoordinates() { return polygonCoordinates; }
        public String getPlaceId() { return placeId; }
        public String getBuildingCode() { return buildingCode; }
    }

    public static List<Building> getSGWBuildings() {
        List<Building> buildings = new ArrayList<>();

        // Hall Building (H) - Main SGW building
        List<LatLng> hallPolygon = new ArrayList<>();
        hallPolygon.add(new LatLng(45.497000, -73.579000));
        hallPolygon.add(new LatLng(45.497200, -73.578500));
        hallPolygon.add(new LatLng(45.496800, -73.578200));
        hallPolygon.add(new LatLng(45.496600, -73.578700));
        buildings.add(new Building("Henry F. Hall Building", CAMPUS_SGW,
            new LatLng(45.496900, -73.578600), hallPolygon, "H"));

        // Engineering Building (EV)
        List<LatLng> evPolygon = new ArrayList<>();
        evPolygon.add(new LatLng(45.495500, -73.577800));
        evPolygon.add(new LatLng(45.495800, -73.577300));
        evPolygon.add(new LatLng(45.495400, -73.577000));
        evPolygon.add(new LatLng(45.495100, -73.577500));
        buildings.add(new Building("Engineering Building", CAMPUS_SGW,
            new LatLng(45.495450, -73.577400), evPolygon, "EV"));

        // John Molson Building (MB)
        List<LatLng> mbPolygon = new ArrayList<>();
        mbPolygon.add(new LatLng(45.495000, -73.579200));
        mbPolygon.add(new LatLng(45.495300, -73.578800));
        mbPolygon.add(new LatLng(45.494900, -73.578500));
        mbPolygon.add(new LatLng(45.494600, -73.578900));
        buildings.add(new Building("John Molson Building", CAMPUS_SGW,
            new LatLng(45.494950, -73.578850), mbPolygon, "MB"));

        return buildings;
    }

    public static List<Building> getLoyolaBuildings() {
        List<Building> buildings = new ArrayList<>();

        // Central Building (CB)
        List<LatLng> cbPolygon = new ArrayList<>();
        cbPolygon.add(new LatLng(45.458200, -73.641000));
        cbPolygon.add(new LatLng(45.458400, -73.640600));
        cbPolygon.add(new LatLng(45.458000, -73.640300));
        cbPolygon.add(new LatLng(45.457800, -73.640700));
        buildings.add(new Building("Central Building", CAMPUS_LOYOLA,
            new LatLng(45.458100, -73.640650), cbPolygon, "CB"));

        // Administration Building (AD)
        List<LatLng> adPolygon = new ArrayList<>();
        adPolygon.add(new LatLng(45.457500, -73.640200));
        adPolygon.add(new LatLng(45.457700, -73.639800));
        adPolygon.add(new LatLng(45.457300, -73.639500));
        adPolygon.add(new LatLng(45.457100, -73.639900));
        buildings.add(new Building("Administration Building", CAMPUS_LOYOLA,
            new LatLng(45.457400, -73.639850), adPolygon, "AD"));

        return buildings;
    }
}
