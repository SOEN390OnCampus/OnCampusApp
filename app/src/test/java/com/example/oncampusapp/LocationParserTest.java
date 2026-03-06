package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LocationParserTest {

    @Test
    public void testParseSmartLocation_SGW_HallBuilding() {
        String result = LocationParser.parseSmartLocation("SOEN 390", "H-110", "Software Engineering Team Design Project");
        assertEquals("Henry F. Hall Building (H) - Room 110", result);
    }

    @Test
    public void testParseSmartLocation_Loyola_ScienceComplex() {
        String result = LocationParser.parseSmartLocation("BIOL 201", "SP-S110", "Intro to Biology");
        assertEquals("Richard J. Renaud Science Complex - Room S110", result);
    }

    @Test
    public void testParseSmartLocation_Online_ZoomLinkInDescription() {
        String result = LocationParser.parseSmartLocation("COMP 346", "TBD", "Join here: https://concordia-ca.zoom.us/j/12345");
        assertEquals("Online", result);
    }

    @Test
    public void testParseSmartLocation_Online_TeamsLocation() {
        String result = LocationParser.parseSmartLocation("ENCS 282", "teams.microsoft.com", "");
        assertEquals("Online", result);
    }

    @Test
    public void testParseSmartLocation_NoMatch_ReturnsRawLocation() {
        String result = LocationParser.parseSmartLocation("Unknown Event", "Off Campus Coffee Shop", "Meeting");
        assertEquals("Off Campus Coffee Shop", result);
    }

    @Test
    public void testParseSmartLocation_EmptyData_ReturnsTBD() {
        String result = LocationParser.parseSmartLocation("", "", "");
        assertEquals("TBD", result);
    }
}