package com.example.oncampusapp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Pure-Java tests for the BuildingFloorData POJO and its nested Floor record.
 */
public class BuildingFloorDataTest {

    // ── Constructor & getters ─────────────────────────────────────────────────

    @Test
    public void constructor_andGetters_allFieldsReturned() {
        List<BuildingFloorData.Floor> floors = Arrays.asList(
                new BuildingFloorData.Floor("8", "Floor 8"),
                new BuildingFloorData.Floor("9", "Floor 9")
        );
        BuildingFloorData data = new BuildingFloorData("H", "Henry F. Hall", floors);

        assertEquals("H",               data.getId());
        assertEquals("Henry F. Hall",   data.getFullName());
        assertSame(floors,              data.getFloors());
    }

    @Test
    public void constructor_withNullValues_returnsNulls() {
        BuildingFloorData data = new BuildingFloorData(null, null, null);
        assertNull(data.getId());
        assertNull(data.getFullName());
        assertNull(data.getFloors());
    }

    @Test
    public void constructor_withEmptyFloorList_returnsEmptyList() {
        BuildingFloorData data = new BuildingFloorData("MB", "Molson Building", Collections.emptyList());
        assertNotNull(data.getFloors());
        assertTrue(data.getFloors().isEmpty());
    }

    @Test
    public void constructor_preservesIdExactly() {
        BuildingFloorData data = new BuildingFloorData("EV", "Engineering Building", Collections.emptyList());
        assertEquals("EV", data.getId());
    }

    @Test
    public void constructor_preservesFullNameExactly() {
        BuildingFloorData data = new BuildingFloorData("EV", "EV Building – Engineering", Collections.emptyList());
        assertEquals("EV Building – Engineering", data.getFullName());
    }

    // ── Floor record ─────────────────────────────────────────────────────────

    @Test
    public void floor_record_idAndName() {
        BuildingFloorData.Floor floor = new BuildingFloorData.Floor("1", "Ground Floor");
        assertEquals("1",            floor.id());
        assertEquals("Ground Floor", floor.name());
    }

    @Test
    public void floor_record_nullValues() {
        BuildingFloorData.Floor floor = new BuildingFloorData.Floor(null, null);
        assertNull(floor.id());
        assertNull(floor.name());
    }

    @Test
    public void floor_record_multipleFloors_areIndependent() {
        BuildingFloorData.Floor f1 = new BuildingFloorData.Floor("1", "First");
        BuildingFloorData.Floor f2 = new BuildingFloorData.Floor("2", "Second");
        assertNotEquals(f1.id(), f2.id());
        assertNotEquals(f1.name(), f2.name());
    }

    @Test
    public void getFloors_returnsSameListReference() {
        List<BuildingFloorData.Floor> floors = Arrays.asList(
                new BuildingFloorData.Floor("G", "Ground"),
                new BuildingFloorData.Floor("1", "First")
        );
        BuildingFloorData data = new BuildingFloorData("VL", "Vanier Library", floors);
        assertSame(floors, data.getFloors());
        assertEquals(2, data.getFloors().size());
    }
}
