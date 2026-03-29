package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class RouteManagerTest {

    // ── parseDurationToMinutes ────────────────────────────────────────────────

    @Test
    public void parseDuration_null_returnsZero() {
        assertEquals(0, RouteManager.parseDurationToMinutes(null));
    }

    @Test
    public void parseDuration_empty_returnsZero() {
        assertEquals(0, RouteManager.parseDurationToMinutes(""));
        assertEquals(0, RouteManager.parseDurationToMinutes("   "));
    }

    @Test
    public void parseDuration_minutesOnly() {
        assertEquals(15, RouteManager.parseDurationToMinutes("15 mins"));
        assertEquals(1,  RouteManager.parseDurationToMinutes("1 min"));
        assertEquals(45, RouteManager.parseDurationToMinutes("45 minutes"));
    }

    @Test
    public void parseDuration_hoursOnly() {
        assertEquals(60,  RouteManager.parseDurationToMinutes("1 hour"));
        assertEquals(120, RouteManager.parseDurationToMinutes("2 hours"));
    }

    @Test
    public void parseDuration_hoursAndMinutes() {
        assertEquals(90,  RouteManager.parseDurationToMinutes("1 hour 30 mins"));
        assertEquals(125, RouteManager.parseDurationToMinutes("2 hours 5 mins"));
    }

    @Test
    public void parseDuration_uppercaseInput() {
        assertEquals(20, RouteManager.parseDurationToMinutes("20 MINS"));
        assertEquals(75, RouteManager.parseDurationToMinutes("1 HOUR 15 MINS"));
    }

    @Test
    public void parseDuration_noRecognizedTokens_returnsZero() {
        assertEquals(0, RouteManager.parseDurationToMinutes("unknown format"));
    }

    // ── checkForInsideRooms ───────────────────────────────────────────────────

    @Test
    public void checkForInsideRooms_regularBuildingName_returnsTrue() {
        assertTrue(RouteManager.checkForInsideRooms("Hall Building"));
        assertTrue(RouteManager.checkForInsideRooms("JMSB"));
        assertTrue(RouteManager.checkForInsideRooms("Loyola Campus"));
    }

    @Test
    public void checkForInsideRooms_hallRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("H-867"));
        assertFalse(RouteManager.checkForInsideRooms("H-110"));
    }

    @Test
    public void checkForInsideRooms_veRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("VE-101"));
    }

    @Test
    public void checkForInsideRooms_ccRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("CC-310"));
    }

    @Test
    public void checkForInsideRooms_lbRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("LB-207"));
    }

    @Test
    public void checkForInsideRooms_mbRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("MB-S2.440"));
    }

    @Test
    public void checkForInsideRooms_vlRoom_returnsFalse() {
        assertFalse(RouteManager.checkForInsideRooms("VL-105"));
    }
}
