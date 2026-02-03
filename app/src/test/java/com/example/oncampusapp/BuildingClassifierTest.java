package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}