package com.example.oncampusapp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {34})
public class ScheduleViewerTest {

    private ScheduleViewer activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(ScheduleViewer.class).get();
    }

    @Test
    public void testGetEventColors_ColorId1() {
        String[] colors = activity.getEventColors("1");
        assertArrayEquals(new String[]{"#7986CB", "#E8EAF6"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId2() {
        String[] colors = activity.getEventColors("2");
        assertArrayEquals(new String[]{"#33B679", "#E8F5E9"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId3() {
        String[] colors = activity.getEventColors("3");
        assertArrayEquals(new String[]{"#8E24AA", "#F3E5F5"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId4() {
        String[] colors = activity.getEventColors("4");
        assertArrayEquals(new String[]{"#E67C73", "#FBE9E7"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId5() {
        String[] colors = activity.getEventColors("5");
        assertArrayEquals(new String[]{"#F6BF26", "#FFFDE7"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId6() {
        String[] colors = activity.getEventColors("6");
        assertArrayEquals(new String[]{"#F4511E", "#FBE9E7"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId7() {
        String[] colors = activity.getEventColors("7");
        assertArrayEquals(new String[]{"#039BE5", "#E1F5FE"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId8() {
        String[] colors = activity.getEventColors("8");
        assertArrayEquals(new String[]{"#616161", "#F5F5F5"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId9() {
        String[] colors = activity.getEventColors("9");
        assertArrayEquals(new String[]{"#3F51B5", "#E8EAF6"}, colors);
    }

    @Test
    public void testGetEventColors_ColorId10() {
        String[] colors = activity.getEventColors("10");
        assertArrayEquals(new String[]{"#0B8043", "#E8F5E9"}, colors);
    }

    @Test
    public void testGetEventColors_Default() {
        String[] colors = activity.getEventColors("0");
        assertArrayEquals(new String[]{"#4285F4", "#DCE6F8"}, colors);
    }

    @Test
    public void testGetEventColors_InvalidColorId() {
        String[] colors = activity.getEventColors("invalid");
        assertArrayEquals(new String[]{"#4285F4", "#DCE6F8"}, colors);
    }

    @Test
    public void testGetEventColors_Modulo11() {
        // 12 % 11 = 1
        String[] colors = activity.getEventColors("12");
        assertArrayEquals(new String[]{"#7986CB", "#E8EAF6"}, colors);
    }

    @Test
    public void testGetHourFromIso_Valid() {
        assertEquals(10, activity.getHourFromIso("2023-10-27T10:30:00Z"));
        assertEquals(0, activity.getHourFromIso("2023-10-27T00:45:00Z"));
        assertEquals(23, activity.getHourFromIso("2023-10-27T23:59:59Z"));
    }

    @Test
    public void testGetHourFromIso_Invalid() {
        assertEquals(0, activity.getHourFromIso("2023-10-27"));
        assertEquals(0, activity.getHourFromIso(""));
        assertEquals(0, activity.getHourFromIso(null));
    }

    @Test
    public void testGetMinFromIso_Valid() {
        assertEquals(30, activity.getMinFromIso("2023-10-27T10:30:00Z"));
        assertEquals(0, activity.getMinFromIso("2023-10-27T10:00:00Z"));
        assertEquals(59, activity.getMinFromIso("2023-10-27T10:59:00Z"));
    }

    @Test
    public void testGetMinFromIso_Invalid() {
        assertEquals(0, activity.getMinFromIso("2023-10-27"));
        assertEquals(0, activity.getMinFromIso(""));
        assertEquals(0, activity.getMinFromIso(null));
    }

    @Test
    public void testDpToPx() {
        // In Robolectric's default configuration, 1dp = 1px (density is 1.0)
        // This might vary if the configuration is changed, but for a standard test it's reliable.
        assertEquals(60, activity.dpToPx(60));
        assertEquals(0, activity.dpToPx(0));
    }

    @Test
    public void testParseIsoToCalendar_Valid() {
        Calendar cal = activity.parseIsoToCalendar("2023-10-27T10:30:00Z");
        assertNotNull(cal);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH));
        assertEquals(27, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testParseIsoToCalendar_Invalid() {
        assertNull(activity.parseIsoToCalendar("2023-10-27"));
        assertNull(activity.parseIsoToCalendar("invalid-date"));
        assertNull(activity.parseIsoToCalendar(""));
        assertNull(activity.parseIsoToCalendar(null));
    }

    @Test
    public void testGetDayOfWeek_Valid() {
        // 2023-10-23 is a Monday
        assertEquals("monday", activity.getDayOfWeek("2023-10-23T10:00:00Z"));
        assertEquals("tuesday", activity.getDayOfWeek("2023-10-24T10:00:00Z"));
        assertEquals("wednesday", activity.getDayOfWeek("2023-10-25T10:00:00Z"));
        assertEquals("thursday", activity.getDayOfWeek("2023-10-26T10:00:00Z"));
        assertEquals("friday", activity.getDayOfWeek("2023-10-27T10:00:00Z"));
        assertEquals("saturday", activity.getDayOfWeek("2023-10-28T10:00:00Z"));
        assertEquals("sunday", activity.getDayOfWeek("2023-10-29T10:00:00Z"));
    }

    @Test
    public void testGetDayOfWeek_Invalid() {
        assertEquals("", activity.getDayOfWeek("2023-10-23"));
        assertEquals("", activity.getDayOfWeek("invalid"));
        assertEquals("", activity.getDayOfWeek(""));
        assertEquals("", activity.getDayOfWeek(null));
    }
}