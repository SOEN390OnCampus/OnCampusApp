package com.example.oncampusapp;import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class GetCurrentBuildingTest {

    @Before
    public void setup() {
        MapsActivity.buildingsMap.clear();
    }

    @Test
    public void returnsBuildingWhenUserInside() {
        Building building = new Building("1", "Hall", null);
        building.currentlyInside = true;

        MapsActivity.buildingsMap.put("1", building);

        Building result = MapsActivity.getCurrentBuilding();

        assertNotNull(result);
        assertEquals("Library", result.getName());
    }

    @Test
    public void returnsFirstBuildingIfMultipleInside() {
        Building building1 = new Building("1", "Hall", null);
        building1.currentlyInside = true;

        Building building2 = new Building("2", "JMSB", null);
        building2.currentlyInside = true;

        MapsActivity.buildingsMap.put("1", building1);
        MapsActivity.buildingsMap.put("2", building2);

        Building result = MapsActivity.getCurrentBuilding();

        assertNotNull(result);
        assertTrue(result == building1 || result == building2);
    }

    @Test
    public void returnsNullWhenUserNotInsideAnyBuilding() {
        Building building = new Building("1", "Hall", null);
        building.currentlyInside = false;

        MapsActivity.buildingsMap.put("1", building);

        Building result = MapsActivity.getCurrentBuilding();

        assertNull(result);
    }
}
