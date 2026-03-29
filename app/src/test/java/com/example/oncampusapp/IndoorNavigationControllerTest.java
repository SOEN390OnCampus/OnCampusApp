package com.example.oncampusapp;

import android.widget.ArrayAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class IndoorNavigationControllerTest {

    // RETURNS_DEEP_STUBS so getResources().getIdentifier() returns 0 instead of NPE
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MapsActivity mockActivity;
    @Mock ArrayAdapter<String> mockAdapter;

    private Map<String, IndoorNode> indoorRoomMap;
    private IndoorNavigationController controller;

    @Before
    public void setUp() {
        indoorRoomMap = new LinkedHashMap<>();
        controller = new IndoorNavigationController(mockActivity, indoorRoomMap);
    }

    // ── Constructor & getters ─────────────────────────────────────────────────

    @Test
    public void getIndoorRoomMap_returnsSameMapPassedIn() {
        assertSame(indoorRoomMap, controller.getIndoorRoomMap());
    }

    @Test
    public void getIndoorRoomMap_initiallyEmpty() {
        assertTrue(controller.getIndoorRoomMap().isEmpty());
    }

    @Test
    public void setSearchSuggestionsAdapter_doesNotThrow() {
        controller.setSearchSuggestionsAdapter(mockAdapter);
        // No exception = pass; adapter is stored for later use
    }

    // ── Map mutation via the returned reference ───────────────────────────────

    @Test
    public void getIndoorRoomMap_reflectsAddedNode() {
        IndoorNode node = new IndoorNode.Builder()
                .id("h-867").label("H-867")
                .buildingId("H").floor("8")
                .build();
        indoorRoomMap.put("H-867", node);

        assertEquals(1, controller.getIndoorRoomMap().size());
        assertSame(node, controller.getIndoorRoomMap().get("H-867"));
    }

    @Test
    public void getIndoorRoomMap_multipleBuildings() {
        String[] labels = {"H-867", "MB-S2.440", "LB-207", "CC-310", "VE-101"};
        for (String label : labels) {
            indoorRoomMap.put(label, new IndoorNode.Builder().label(label).build());
        }
        assertEquals(5, controller.getIndoorRoomMap().size());
    }

}
