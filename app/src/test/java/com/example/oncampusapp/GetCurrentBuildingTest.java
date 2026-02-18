package com.example.oncampusapp;import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class GetCurrentBuildingTest {

    BuildingManager buildingManager;
    Building building1;
    Building building2;


    @Before
    public void setUp() {
        buildingManager = new BuildingManager();
        building1 = new Building("1", "Hall", null);
        building2 = new Building("2", "JMSB", null);
        buildingManager.addBuilding(building1);
        buildingManager.addBuilding(building2);
    }

    @Test
    public void returnsBuildingWhenUserInside() {
        building1.currentlyInside = true;
        Building result = buildingManager.getCurrentBuilding();

        assertNotNull(result);
        assertEquals("Hall", result.getName());
    }

    @Test
    public void returnsFirstBuildingIfMultipleInside() {

        building1.currentlyInside = true;
        building2.currentlyInside = true;

        Building result = buildingManager.getCurrentBuilding();

        assertNotNull(result);
        assertTrue(result == building1 || result == building2);
    }

    @Test
    public void returnsNullWhenUserNotInsideAnyBuilding() {


        building1.currentlyInside = false;
        building2.currentlyInside = false;

        Building result = buildingManager.getCurrentBuilding();

        assertNull(result);
    }
}
