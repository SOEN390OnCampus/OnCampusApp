package com.example.oncampusapp;

import android.content.Intent;
import android.widget.ArrayAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class IndoorNavigationControllerTest {

    private MapsActivity activity;
    private ArrayAdapter<String> mockAdapter;

    private Map<String, IndoorNode> indoorRoomMap;
    private IndoorNavigationController controller;

    @Before
    public void setUp() {
        // Build a raw activity context using Robolectric to handle Intents and Toasts
        activity = Robolectric.buildActivity(MapsActivity.class).get();
        mockAdapter = mock(ArrayAdapter.class);

        indoorRoomMap = new LinkedHashMap<>();
        controller = new IndoorNavigationController(activity, indoorRoomMap);
    }

    // ── Original Getter / Setter / State Tests ────────────────────────────────

    @Test
    public void getIndoorRoomMap_returnsSameMapPassedIn() {
        assertSame(indoorRoomMap, controller.getIndoorRoomMap());
    }

    @Test
    public void getIndoorRoomMap_initiallyEmpty() {
        assertTrue(controller.getIndoorRoomMap().isEmpty());
    }

    @Test
    public void getIndoorRoomMap_reflectsAddedNode() {
        IndoorNode node = new IndoorNode.Builder().id("h-867").label("H-867").buildingId("H").build();
        indoorRoomMap.put("H-867", node);

        assertEquals(1, controller.getIndoorRoomMap().size());
        assertSame(node, controller.getIndoorRoomMap().get("H-867"));
    }

    @Test
    public void setSearchSuggestionsAdapter_nullAdapter_doesNotThrow() {
        controller.setSearchSuggestionsAdapter(null);
        assertNotNull(controller.getIndoorRoomMap());
    }

    @Test
    public void loadIndoorRoomsIntoAdapter_doesNotThrowOnCallingThread() {
        controller.loadIndoorRoomsIntoAdapter();
        assertNotNull(controller.getIndoorRoomMap());
    }

    // ── IndoorNode.Builder edge cases ─────────────────────────────────────────

    @Test
    public void indoorNodeBuilder_allFields_roundTrip() {
        IndoorNode node = new IndoorNode.Builder()
                .id("lb-207").label("LB-207")
                .buildingId("LB").floor("2")
                .type("room").x(10f).y(20f).accessible(true)
                .build();
        assertEquals("lb-207", node.getId());
        assertEquals("LB-207", node.getLabel());
        assertEquals("LB", node.getRootBuildingId());
    }

    // ── Static Pathfinding Math ───────────────────────────────────────────────

    @Test
    public void computePath_reducedMobilityEnabled_usesAccessiblePath() {
        IndoorGraph graph = mock(IndoorGraph.class);
        List<String> expectedPath = Arrays.asList("A", "B", "C");

        when(graph.shortestAccessiblePath("from", "to")).thenReturn(expectedPath);

        List<String> result = IndoorNavigationController.computePath(graph, "from", "to", true);

        assertEquals(expectedPath, result);
        verify(graph).shortestAccessiblePath("from", "to");
        verify(graph, never()).shortestPath("from", "to");
    }

    @Test
    public void computePath_reducedMobilityDisabled_usesNormalPath() {
        IndoorGraph graph = mock(IndoorGraph.class);
        List<String> expectedPath = Arrays.asList("X", "Y");

        when(graph.shortestPath("from", "to")).thenReturn(expectedPath);

        List<String> result = IndoorNavigationController.computePath(graph, "from", "to", false);

        assertEquals(expectedPath, result);
        verify(graph).shortestPath("from", "to");
        verify(graph, never()).shortestAccessiblePath("from", "to");
    }

    // ── Routing Logic & Cross-Building Tests ──────────────────────────────────

    @Test
    public void testLaunchIndoorRoute_sameBuilding_missingResource_showsToast() {
        // FIX: Use a building ID that definitely does not exist in res/raw
        // to guarantee getIdentifier() returns 0 and triggers the Toast instantly.
        IndoorNode fromRoom = new IndoorNode.Builder().id("fake-1").buildingId("FAKE").build();
        IndoorNode toRoom = new IndoorNode.Builder().id("fake-2").buildingId("FAKE").build();

        // Action
        controller.launchIndoorRoute(fromRoom, toRoom);

        // Force the main thread to execute its pending UI tasks
        org.robolectric.shadows.ShadowLooper.idleMainLooper();

        // Assertion
        String latestToast = ShadowToast.getTextOfLatestToast();
        assertEquals("Error loading building data.", latestToast);
    }

    @Test
    public void testLaunchIndoorRoute_crossBuilding_noDoorways_showsToast() {
        IndoorNode fromRoom = new IndoorNode.Builder().id("h-1").buildingId("H").build();
        IndoorNode toRoom = new IndoorNode.Builder().id("mb-1").buildingId("MB").build();

        // Action: allIndoorNodesById is empty, so it can't find building doorways
        controller.launchIndoorRoute(fromRoom, toRoom);

        // Assertion
        String latestToast = ShadowToast.getTextOfLatestToast();
        assertEquals("Cross-building indoor navigation is not supported for these buildings.", latestToast);
    }

    @Test
    public void testFindPreferredDoorway_prefersFloor1OverFloor2() throws Exception {
        // Setup: Inject mock doorway nodes into the private map
        Map<String, IndoorNode> allNodes = new HashMap<>();
        IndoorNode doorFloor2 = new IndoorNode.Builder().id("H_building_entry_exit_2").buildingId("H").floor("2").build();
        IndoorNode doorFloor1 = new IndoorNode.Builder().id("H_building_entry_exit_1").buildingId("H").floor("1").build();

        allNodes.put(doorFloor2.getId(), doorFloor2);
        allNodes.put(doorFloor1.getId(), doorFloor1);

        Field nodesField = IndoorNavigationController.class.getDeclaredField("allIndoorNodesById");
        nodesField.setAccessible(true);
        nodesField.set(controller, allNodes);

        // Action: Call private doorway logic via reflection
        Method findDoorway = IndoorNavigationController.class.getDeclaredMethod("findPreferredDoorway", String.class);
        findDoorway.setAccessible(true);

        IndoorNode result = (IndoorNode) findDoorway.invoke(controller, "H");

        // Assertion
        assertEquals("Should prefer floor 1", doorFloor1, result);
    }

    @Test
    public void testLaunchIndoorRoute_crossBuilding_success_firesIntent() throws Exception {
        // Setup: Spy the controller so we can intercept loadIndoorPath
        IndoorNavigationController spyController = Mockito.spy(controller);

        // 1. Inject doorways into the private map
        Map<String, IndoorNode> allNodes = new HashMap<>();
        IndoorNode doorH = new IndoorNode.Builder().id("H_building_entry_exit_1").buildingId("H").floor("1").label("H Exit").build();
        IndoorNode doorMB = new IndoorNode.Builder().id("MB_building_entry_exit_1").buildingId("MB").floor("1").label("MB Exit").build();
        allNodes.put(doorH.getId(), doorH);
        allNodes.put(doorMB.getId(), doorMB);

        Field nodesField = IndoorNavigationController.class.getDeclaredField("allIndoorNodesById");
        nodesField.setAccessible(true);
        nodesField.set(spyController, allNodes);

        // 2. Intercept the background thread callback to simulate immediate success
        doAnswer(invocation -> {
            java.util.function.Consumer<List<String>> callback = invocation.getArgument(3);
            callback.accept(Arrays.asList("H_room1", "H_hallway", "H_building_entry_exit_1"));
            return null;
        }).when(spyController).loadIndoorPath(anyString(), anyString(), anyString(), any(), anyString());

        // 3. Create route parameters
        IndoorNode fromRoom = new IndoorNode.Builder().id("H_room1").buildingId("H").floor("8").build();
        IndoorNode toRoom = new IndoorNode.Builder().id("MB_room1").buildingId("MB").floor("2").build();

        // Action
        spyController.launchIndoorRoute(fromRoom, toRoom);

        // Assertion: Verify it calculated the correct routing stage and fired the Intent
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent intent = shadowActivity.getNextStartedActivity();

        assertNotNull("Intent should be fired to launch IndoorMapActivity", intent);
        assertEquals(IndoorMapActivity.class.getName(), intent.getComponent().getClassName());

        // Verify Cross-Building Extras
        assertEquals("FIRST_INDOOR", intent.getStringExtra("CROSS_BUILDING_STAGE"));
        assertEquals("H Exit", intent.getStringExtra("DISPLAY_DEST_LABEL"));
        assertEquals("H_building_entry_exit_1", intent.getStringExtra("TO_NODE_ID"));
    }
}