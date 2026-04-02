package com.example.oncampusapp;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class BuildingDetailsTest {

    // ── Default constructor & Schedule defaults ────────────────────────────────

    @Test
    public void defaultConstructor_createsSchedule() {
        BuildingDetails details = new BuildingDetails();
        assertNotNull(details.getSchedule());
    }

    @Test
    public void schedule_defaultGrouping_weekdaysAndWeekendsSeparated() {
        BuildingDetails.Schedule schedule = new BuildingDetails.Schedule();
        List<String> groups = schedule.groupSchedule();

        // Default: Mon–Fri "7 a.m.–11 p.m.", Sat–Sun "7 a.m.–7 p.m."
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertTrue(groups.get(0).contains("Monday"));
        assertTrue(groups.get(0).contains("Friday"));
        assertTrue(groups.get(1).contains("Saturday"));
        assertTrue(groups.get(1).contains("Sunday"));
    }

    @Test
    public void schedule_toString_containsNewlines() {
        BuildingDetails.Schedule schedule = new BuildingDetails.Schedule();
        String str = schedule.toString();
        assertTrue(str.contains("\n"));
        assertFalse(str.equals("Always Open"));
    }

    // ── alwaysOpen path ───────────────────────────────────────────────────────

    @Test
    public void schedule_alwaysOpen_groupScheduleReturnsEmptyList() {
        BuildingDetails.Schedule schedule = new BuildingDetails.Schedule();
        // Access via reflection since alwaysOpen has no public setter
        try {
            java.lang.reflect.Field f = BuildingDetails.Schedule.class.getDeclaredField("alwaysOpen");
            f.setAccessible(true);
            f.set(schedule, true);
        } catch (Exception e) {
            fail("Could not set alwaysOpen: " + e.getMessage());
        }
        List<String> groups = schedule.groupSchedule();
        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    public void schedule_alwaysOpen_toStringReturnsEmptyString() {
        // groupSchedule() returns an empty list when alwaysOpen=true,
        // so toString() iterates nothing and returns "".
        BuildingDetails.Schedule schedule = new BuildingDetails.Schedule();
        try {
            java.lang.reflect.Field f = BuildingDetails.Schedule.class.getDeclaredField("alwaysOpen");
            f.setAccessible(true);
            f.set(schedule, true);
        } catch (Exception e) {
            fail("Could not set alwaysOpen: " + e.getMessage());
        }
        assertEquals("", schedule.toString());
    }

    // ── Single-day range (same start/end day) ─────────────────────────────────

    @Test
    public void schedule_allSameHours_producesOneGroup() {
        BuildingDetails.Schedule schedule = new BuildingDetails.Schedule();
        try {
            setField(schedule, "monday",    "8 a.m.–6 p.m.");
            setField(schedule, "tuesday",   "8 a.m.–6 p.m.");
            setField(schedule, "wednesday", "8 a.m.–6 p.m.");
            setField(schedule, "thursday",  "8 a.m.–6 p.m.");
            setField(schedule, "friday",    "8 a.m.–6 p.m.");
            setField(schedule, "saturday",  "8 a.m.–6 p.m.");
            setField(schedule, "sunday",    "8 a.m.–6 p.m.");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        List<String> groups = schedule.groupSchedule();
        assertEquals(1, groups.size());
        assertTrue(groups.get(0).contains("Monday"));
        assertTrue(groups.get(0).contains("Sunday"));
    }

    @Test
    public void schedule_everyDayDifferent_producesSevenGroups() {
        BuildingDetails.Schedule schedule = new BuildingDetails.Schedule();
        String[] times = {"8am", "9am", "10am", "11am", "12pm", "1pm", "2pm"};
        String[] fields = {"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};
        try {
            for (int i = 0; i < fields.length; i++) setField(schedule, fields[i], times[i]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        List<String> groups = schedule.groupSchedule();
        assertEquals(7, groups.size());
    }

    // ── BuildingDetails getters ────────────────────────────────────────────────

    @Test
    public void buildingDetails_defaultGetters_returnNullOrFalse() {
        BuildingDetails details = new BuildingDetails();
        assertNull(details.getCode());
        assertNull(details.getName());
        assertNull(details.getAddress());
        assertNull(details.getImage());
        assertNull(details.getLink());
        assertFalse(details.isAccessible());
        assertFalse(details.hasDirectTunnelToMetro());
        assertEquals(0.0, details.getLat(), 0.0001);
        assertEquals(0.0, details.getLng(), 0.0001);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void setField(Object obj, String name, Object value) throws Exception {
        java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }
}
