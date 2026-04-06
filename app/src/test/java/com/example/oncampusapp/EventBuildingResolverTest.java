package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class EventBuildingResolverTest {

    private Map<String, BuildingDetails> buildingDetailsMap;

    private final String location;
    private final String expectedBuilding;

    public EventBuildingResolverTest(String location, String expectedBuilding) {
        this.location = location;
        this.expectedBuilding = expectedBuilding;
    }

    @Parameterized.Parameters(name = "location={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "Henry F. Hall Building", "Henry F. Hall Building" },
            { "H-110",                 "Henry F. Hall Building" },
            { "MB 2.210",              "John Molson School of Business" },
            { "EV 1.162",              "Engineering, Computer Science and Visual Arts Integrated Complex" },
            { "FG-B060",               "Le Faubourg" },
            { "Some Random Place",     "Some Random Place" }
        });
    }

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
    public void extractBuildingNameFromEvent_returnsExpectedBuilding() throws Exception {
        JSONObject event = new JSONObject();
        event.put("location", location);

        String result = EventBuildingResolver.extractBuildingNameFromEvent(event, buildingDetailsMap);

        assertEquals(expectedBuilding, result);
    }
}
