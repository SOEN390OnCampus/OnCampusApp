package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PoiTest {

    @Test
    public void constructorAndGetters_storeValuesCorrectly() {
        Poi poi = new Poi(
                "Cafe Van Houtte",
                "Restaurants",
                45.4958,
                -73.5785,
                0.54,
                "Open"
        );

        assertEquals("Cafe Van Houtte", poi.getName());
        assertEquals("Restaurants", poi.getCategory());
        assertEquals(45.4958, poi.getLatitude(), 0.0001);
        assertEquals(-73.5785, poi.getLongitude(), 0.0001);
        assertEquals(0.54, poi.getDistanceKm(), 0.0001);
        assertEquals("Open", poi.getStatus());
    }
}