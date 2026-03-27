package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EventBuildingResolverTest {

    private Map<String, BuildingDetails> buildingDetailsMap;

    @Before
    public void setUp() {
        buildingDetailsMap = new HashMap<>();

        BuildingDetails hall = new BuildingDetails();
        hall.setName("Henry F. Hall Building");

        BuildingDetails jmsb = new BuildingDetails();
        jmsb.setName("John Molson School of Business");

        buildingDetailsMap.put("hall", hall);
        buildingDetailsMap.put("jmsb", jmsb);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsNull_whenEventIsNull() {
        String result = EventBuildingResolver.extractBuildingNameFromEvent(null, buildingDetailsMap);
        assertNull(result);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsNull_whenMapIsNull() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", "Henry F. Hall Building");

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, null);
        assertNull(result);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsNull_whenLocationIsEmpty() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", "");

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, buildingDetailsMap);
        assertNull(result);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsExactMatch_whenFullBuildingNameExists() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", "Henry F. Hall Building");

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, buildingDetailsMap);

        assertEquals("Henry F. Hall Building", result);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsHall_whenShortCodeStartsWithH() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", "H-110");

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, buildingDetailsMap);

        assertEquals("Henry F. Hall Building", result);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsJmsb_whenShortCodeStartsWithMb() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", "MB 2.210");

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, buildingDetailsMap);

        assertEquals("John Molson School of Business", result);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsEngineering_whenLocationStartsWithEv() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", "EV 1.162");

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, buildingDetailsMap);

        assertEquals("Engineering, Computer Science and Visual Arts Integrated Complex", result);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsFaubourg_whenLocationStartsWithFg() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", "FG-B060");

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, buildingDetailsMap);

        assertEquals("Le Faubourg", result);
    }

    @Test
    public void extractBuildingNameFromEvent_returnsRawLocation_whenNoMatchFound() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", "Some Random Place");

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, buildingDetailsMap);

        assertEquals("Some Random Place", result);
    }
}
