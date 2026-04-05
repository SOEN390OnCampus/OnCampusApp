package com.example.oncampusapp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {34})
public class ScheduleViewerTest {

    private ScheduleViewer activity;

    @Before
    public void setUp() {
        // Uninitialized activity for pure logic and reflection tests
        activity = Robolectric.buildActivity(ScheduleViewer.class).get();
    }

    // ── Full Activity UI & Lifecycle Tests ────────────────────────────────────

    /**
     * Safely boots a fully lifecycle-aware Activity for UI testing.
     * The background Executor thread will naturally swallow its own network exceptions
     * in Robolectric, allowing the UI to safely bind and be tested.
     */
    @SuppressWarnings("deprecation") // Suppress the strikethrough warning for buildActivity
    private ScheduleViewer createFullActivity() {
        Intent intent = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), ScheduleViewer.class);
        intent.putExtra("calendar_name", "Test Calendar");
        intent.putExtra("calendar_color", "#8B1E2D");
        intent.putExtra("calendar_id", "test_cal_123");
        intent.putExtra("calendar_token", "fake_token");

        // Renamed from 'activity' to 'fullActivity' to fix the shadowing warning
        ScheduleViewer fullActivity = Robolectric.buildActivity(ScheduleViewer.class, intent).create().resume().get();

        // FIX: Flush any pending runOnUiThread tasks from the background Executor
        // so the UI is fully populated with real dates before the tests start.
        org.robolectric.shadows.ShadowLooper.idleMainLooper();

        return fullActivity;
    }

    @Test
    public void testActivityInitialization_setsTitle() {
        ScheduleViewer fullActivity = createFullActivity();
        TextView titleView = fullActivity.findViewById(R.id.calendar_header_title);
        assertEquals("Test Calendar", titleView.getText().toString());
    }

    @Test
    public void testMainCalendarButton_updatesSharedPreferences() {
        ScheduleViewer fullActivity = createFullActivity();
        Button btnMain = fullActivity.findViewById(R.id.btn_select_main_calendar);

        // Initially it should not be selected
        assertNotEquals("Selected ✓", btnMain.getText().toString());

        // Action: Click the button to select it as the main calendar
        btnMain.performClick();

        // Assertion 1: UI updates
        assertEquals("Selected ✓", btnMain.getText().toString());

        // Assertion 2: SharedPreferences is updated securely
        SharedPreferences prefs = fullActivity.getSharedPreferences("OnCampusPrefs", Context.MODE_PRIVATE);
        assertEquals("test_cal_123", prefs.getString("selected_calendar", ""));
    }

    @Test
    public void testBackButton_finishesActivity() {
        ScheduleViewer fullActivity = createFullActivity();
        fullActivity.findViewById(R.id.btn_back).performClick();
        assertTrue("Activity should finish when back is clicked", fullActivity.isFinishing());
    }

    @Test
    public void testNavButtons_changeWeekTitle() {
        ScheduleViewer fullActivity = createFullActivity();
        TextView weekTitle = fullActivity.findViewById(R.id.week_title);

        // FIX: The background network call fails with our "fake_token", so the initial
        // UI update never happens and the XML placeholder ("Sep 8 - 14, 2025") remains.
        // By clicking left then right, we force the Activity to run updateWeek()
        // on the main thread, syncing the UI with the real current date.
        fullActivity.findViewById(R.id.nav_left).performClick();
        fullActivity.findViewById(R.id.nav_right).performClick();

        // Now the title will accurately reflect the real date (e.g., Apr 4 - 10, 2026)
        String initialTitle = weekTitle.getText().toString();

        // Action: Click right to go to next week
        fullActivity.findViewById(R.id.nav_right).performClick();
        String nextWeekTitle = weekTitle.getText().toString();

        assertNotEquals("Title should change when moving forward a week", initialTitle, nextWeekTitle);

        // Action: Click left to go back
        fullActivity.findViewById(R.id.nav_left).performClick();
        String prevWeekTitle = weekTitle.getText().toString();

        assertEquals("Moving right then left should return to original week", initialTitle, prevWeekTitle);
    }

    // ── Private UI Math & Inflation Tests (Reflection) ────────────────────────

    @Test
    public void testCreateEventBox_inflatesAndPopulatesCorrectly() throws Exception {
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);

        Method createEventBox = ScheduleViewer.class.getDeclaredMethod(
                "createEventBox", String.class, String.class, String.class, String.class, String.class, ViewGroup.class);
        createEventBox.setAccessible(true);
        FrameLayout dummyParent = new FrameLayout(activity);

        // Action: Invoke the private method. "Building - Room 101" should split to "Room 101"
        View eventView = (View) createEventBox.invoke(
                activity, "Lecture", "Building - Room 101", "2023-10-23T10:00:00Z", "2023-10-23T11:00:00Z", "1", dummyParent);

        assertNotNull(eventView);
        TextView titleView = eventView.findViewById(R.id.event_title);
        TextView locView = eventView.findViewById(R.id.event_location);

        // Assertions
        assertEquals("Lecture", titleView.getText().toString());
        assertEquals("Room 101", locView.getText().toString());
    }

    @Test
    public void testSnapToMonday_forcesMondayStart() throws Exception {
        Method snapToMonday = ScheduleViewer.class.getDeclaredMethod("snapToMonday", Calendar.class);
        snapToMonday.setAccessible(true);

        // Setup: A random Thursday at 3:00 PM
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.OCTOBER, 26);
        cal.set(Calendar.HOUR_OF_DAY, 15);

        // Action
        snapToMonday.invoke(activity, cal);

        // Assertion: Must snap cleanly to Monday at Midnight
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testSetupGrid_populatesTimeAndDayColumns() throws Exception {
        // Setup: Manually bind the UI components to test the Grid builder without starting the activity
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
        activity.setContentView(R.layout.activity_schedule);

        Field timeColField = ScheduleViewer.class.getDeclaredField("timeColumn");
        timeColField.setAccessible(true);
        timeColField.set(activity, activity.findViewById(R.id.time_column));

        Field headerRowField = ScheduleViewer.class.getDeclaredField("headerRow");
        headerRowField.setAccessible(true);
        headerRowField.set(activity, activity.findViewById(R.id.header_row));

        Field daysContainerField = ScheduleViewer.class.getDeclaredField("daysContainer");
        daysContainerField.setAccessible(true);
        daysContainerField.set(activity, activity.findViewById(R.id.days_container));

        Method setupGrid = ScheduleViewer.class.getDeclaredMethod("setupGrid");
        setupGrid.setAccessible(true);

        // Action
        setupGrid.invoke(activity);

        // Assertion 1: Time column should be populated from 7am to 10pm (16 items)
        LinearLayout timeCol = activity.findViewById(R.id.time_column);
        assertEquals(16, timeCol.getChildCount());

        // Assertion 2: Day columns should be generated (Mon - Sun)
        LinearLayout headerRow = activity.findViewById(R.id.header_row);
        assertTrue("Header row should have at least 7 days", headerRow.getChildCount() >= 7);
    }

    // ── Original Pure Logic Tests ─────────────────────────────────────────────

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