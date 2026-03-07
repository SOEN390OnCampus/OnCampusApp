package com.example.oncampusapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.assertEquals;

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
}