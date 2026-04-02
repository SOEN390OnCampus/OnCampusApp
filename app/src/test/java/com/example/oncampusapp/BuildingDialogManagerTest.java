package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BuildingDialogManagerTest {

    @Mock MapsActivity mockActivity;

    private BuildingDialogManager manager;

    @Before
    public void setUp() {
        manager = new BuildingDialogManager(mockActivity);
    }

    // ── Constructor & initial state ───────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(manager);
    }

    @Test
    public void getCurrentBuildingDialog_initiallyNull() {
        assertNull(manager.getCurrentBuildingDialog());
    }

    @Test
    public void getGeoIdToBuildingDetailsMap_initiallyNull() {
        assertNull(manager.getGeoIdToBuildingDetailsMap());
    }
}
