package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class BuildingClassifierTest {

    private BuildingClassifier buildingClassifier;

    @Before
    public void setUp() {
        buildingClassifier = new BuildingClassifier();
    }

    @Test
    public void testIsConcordiaBuilding_university() {
        assertTrue(buildingClassifier.isConcordiaBuilding("university", null, null));
    }

    @Test
    public void testIsConcordiaBuilding_nameContainsConcordia() {
        assertTrue(buildingClassifier.isConcordiaBuilding(null, "Concordia Hall", null));
    }

    @Test
    public void testIsConcordiaBuilding_operatorIsConcordia() {
        assertTrue(buildingClassifier.isConcordiaBuilding(null, null, "Concordia University"));
    }

    @Test
    public void testIsConcordiaBuilding_stingerDome() {
        assertTrue(buildingClassifier.isConcordiaBuilding(null, "Stinger Dome (SD)", null));
    }

    @Test
    public void testIsConcordiaBuilding_notConcordiaBuilding() {
        assertFalse(buildingClassifier.isConcordiaBuilding("apartments", "Some Building", "Some Operator"));
    }

    @Test
    public void testIsConcordiaBuilding_nullProperties() {
        assertFalse(buildingClassifier.isConcordiaBuilding(null, null, null));
    }

    @Test
    public void polygon_isStoredCorrectly() {

        List<LatLng> polygon = Arrays.asList(
                new LatLng(1,1),
                new LatLng(2,2),
                new LatLng(3,3)
        );

        Building building = new Building("B", "Test", polygon);

        assertEquals(3, building.polygon.size());
    }

    @Test
    public void buildingConstructor_setsFieldsCorrectly() {
        List<LatLng> polygon = Arrays.asList(
                new LatLng(45.0, -73.0),
                new LatLng(45.1, -73.1)
        );

        Building building = new Building("H123", "Hall Building", polygon);

        assertEquals("H123", building.getId());
        assertEquals("Hall Building", building.name);
        assertEquals(polygon, building.polygon);
    }
}