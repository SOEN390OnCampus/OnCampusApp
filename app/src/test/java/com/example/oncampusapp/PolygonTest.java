package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mockito.junit.MockitoJUnitRunner;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PolygonTest {

    LatLng insideBuilding;
    LatLng outsideBuilding;

    List<LatLng> building_polygon;

    Building HBuilding;

    // Creates a mock polygon to test the rest of the methods with
    @Before
    public void setUp() {
        insideBuilding = new LatLng(45.4970, -73.5790);
        outsideBuilding = new LatLng(45.4980, -73.5805);

        building_polygon = Arrays.asList(
                new LatLng(45.4968, -73.5788),
                new LatLng(45.4972, -73.5784),
                new LatLng(45.4977, -73.5790),
                new LatLng(45.4971, -73.5795)
        );

        HBuilding = new Building("H", "Hall Building", building_polygon);
    }


    @Test
    public void userOutsidePolygon_returnsFalseEnteredPolygon() {

        boolean result = PolyUtil.containsLocation(outsideBuilding, building_polygon, true);

        assertFalse(result);
    }

    @Test
    public void buildingState_enteringBuilding() {

        boolean entered = PolyUtil.containsLocation(insideBuilding, building_polygon, true);

        assertTrue(entered);
    }

    @Test
    public void buildingState_exitBuilding() {

        boolean entered = PolyUtil.containsLocation(outsideBuilding, building_polygon, true);

        assertFalse(entered);
    }

    @Test
    public void buildingState_switchStates() {
        // user enters
        boolean entered = PolyUtil.containsLocation(insideBuilding, building_polygon, true);

        if (!HBuilding.currentlyInside && entered) {
            HBuilding.currentlyInside = true;
        }

        assertTrue(HBuilding.currentlyInside);

        // user exits
        boolean exited = PolyUtil.containsLocation(outsideBuilding, building_polygon, true);

        if (HBuilding.currentlyInside && !exited) {
            HBuilding.currentlyInside = false;
        }

        assertFalse(HBuilding.currentlyInside);
    }

    @Test
    public void buildingState_checkBuildingId() {
        assertEquals("H", HBuilding.getId());
    }



}
