package com.example.oncampusapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class LocationParserTest {

    @Test
    public void testParseSmartLocation_SGW_HallBuilding() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(context, "SOEN 390", "H-110", "Software Engineering Team Design Project");
        assertEquals("Henry F. Hall Building - Room 110", result);
        // Note: Adjusted the expected result slightly based on the exact name in your JSON ("Henry F. Hall Building" instead of "Henry F. Hall Building (H)")
    }

    @Test
    public void testParseSmartLocation_Loyola_ScienceComplex() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(context, "BIOL 201", "SP-S110", "Intro to Biology");
        assertEquals("Richard J. Renaud Science Complex - Room S110", result);
    }

    @Test
    public void testParseSmartLocation_Online_ZoomLinkInDescription() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(context, "COMP 346", "TBD", "Join here: https://concordia-ca.zoom.us/j/12345");
        assertEquals("Online", result);
    }

    @Test
    public void testParseSmartLocation_Online_TeamsLocation() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(context, "ENCS 282", "teams.microsoft.com", "");
        assertEquals("Online", result);
    }

    @Test
    public void testParseSmartLocation_NoMatch_ReturnsRawLocation() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(context, "Unknown Event", "Off Campus Coffee Shop", "Meeting");
        assertEquals("Off Campus Coffee Shop", result);
    }

    @Test
    public void testParseSmartLocation_EmptyData_ReturnsTBD() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(context, "", "", "");
        assertEquals("TBD", result);
    }

    // ── Online detection – each individual trigger ────────────────────────────

    @Test
    public void parseSmartLocation_zoomKeyword_returnsOnline() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(context, "Lecture", "zoom", "");
        assertEquals("Online", result);
    }

    @Test
    public void parseSmartLocation_meetGoogleInDescription_returnsOnline() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(
                context, "Tutorial", "", "Join at meet.google.com/abc-xyz");
        assertEquals("Online", result);
    }

    @Test
    public void parseSmartLocation_onlineKeywordInTitle_returnsOnline() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(
                context, "Online Session", "", "");
        assertEquals("Online", result);
    }

    @Test
    public void parseSmartLocation_onlineKeywordInLocation_returnsOnline() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(
                context, "Lab", "online", "");
        assertEquals("Online", result);
    }

    @Test
    public void parseSmartLocation_zoomUsInTitle_returnsOnline() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(
                context, "zoom.us meeting", "", "");
        assertEquals("Online", result);
    }

    // ── Null inputs handled gracefully ────────────────────────────────────────

    @Test
    public void parseSmartLocation_nullTitle_doesNotThrow() {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull(LocationParser.parseSmartLocation(context, null, "H-110", ""));
    }

    @Test
    public void parseSmartLocation_nullRawLocation_doesNotThrow() {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull(LocationParser.parseSmartLocation(context, "SOEN 390", null, ""));
    }

    @Test
    public void parseSmartLocation_nullDescription_doesNotThrow() {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull(LocationParser.parseSmartLocation(context, "SOEN 390", "H-110", null));
    }

    @Test
    public void parseSmartLocation_allNulls_doesNotThrow() {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull(LocationParser.parseSmartLocation(context, null, null, null));
    }

    // ── No-match fallbacks ────────────────────────────────────────────────────

    @Test
    public void parseSmartLocation_noMatchNoRawLocation_returnsTBD() {
        Context context = ApplicationProvider.getApplicationContext();
        String result = LocationParser.parseSmartLocation(
                context, "Mystery Event", "", "");
        assertEquals("TBD", result);
    }

    @Test
    public void parseSmartLocation_noMatchWithRawLocation_returnsRawLocation() {
        Context context = ApplicationProvider.getApplicationContext();
        String rawLoc = "1455 de Maisonneuve Blvd W";
        String result = LocationParser.parseSmartLocation(
                context, "Off-campus event", rawLoc, "");
        assertEquals(rawLoc, result);
    }

    // ── isLoaded cache – second call must not reload ──────────────────────────

    @Test
    public void parseSmartLocation_calledTwice_doesNotThrow() {
        Context context = ApplicationProvider.getApplicationContext();
        String first = LocationParser.parseSmartLocation(context, "COMP 346", "H-110", "");
        String second = LocationParser.parseSmartLocation(context, "SOEN 390", "H-110", "");
        assertNotNull(first);
        assertNotNull(second);
    }
}