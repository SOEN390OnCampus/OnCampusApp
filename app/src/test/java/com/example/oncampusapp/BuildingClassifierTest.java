package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class BuildingClassifierTest {

    @Test
    public void testIsConcordiaBuilding_university() {
        assertTrue(BuildingClassifier.isConcordiaBuilding("university", null, null));
    }

    @Test
    public void testIsConcordiaBuilding_nameContainsConcordia() {
        assertTrue(BuildingClassifier.isConcordiaBuilding(null, "Concordia Hall", null));
    }

    @Test
    public void testIsConcordiaBuilding_operatorIsConcordia() {
        assertTrue(BuildingClassifier.isConcordiaBuilding(null, null, "Concordia University"));
    }

    @Test
    public void testIsConcordiaBuilding_stingerDome() {
        assertTrue(BuildingClassifier.isConcordiaBuilding(null, "Stinger Dome (SD)", null));
    }

    @Test
    public void testIsConcordiaBuilding_notConcordiaBuilding() {
        assertFalse(BuildingClassifier.isConcordiaBuilding("apartments", "Some Building", "Some Operator"));
    }

    @Test
    public void testIsConcordiaBuilding_nullProperties() {
        assertFalse(BuildingClassifier.isConcordiaBuilding(null, null, null));
    }
}