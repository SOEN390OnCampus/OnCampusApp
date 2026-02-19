package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.android.gms.maps.model.LatLng;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuildingTest {

    // Normal Case
    @Test
    public void getCenter_returnsCorrectAverage() {
        List<LatLng> polygon = Arrays.asList(
                new LatLng(0, 0),
                new LatLng(2, 2)
        );
        Building building = new Building("1", "Test", polygon);
        LatLng center = building.getCenter();

        assertEquals(1.0, center.latitude, 0.0001);
        assertEquals(1.0, center.longitude, 0.0001);
    }

    // Null Case
    @Test
    public void getCenter_nullPolygon_returnsNull() {
        Building building = new Building("1", "Test", null);
        LatLng center = building.getCenter();
        assertNull(center);
    }

    // Empty Polygon Case
    @Test
    public void getCenter_emptyPolygon_returnsNull() {
        Building building = new Building("1", "Test", new ArrayList<>());
        LatLng center = building.getCenter();
        assertNull(center);
    }

}
