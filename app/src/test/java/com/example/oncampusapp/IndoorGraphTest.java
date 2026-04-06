package com.example.oncampusapp;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;


import static org.junit.Assert.*;

public class IndoorGraphTest {

    private IndoorGraph graph;

    @Before
    public void setUp() throws IOException, JSONException {
        graph = new IndoorGraph();

        // Load the mini-map JSON directly from the test resources folder
        InputStream is = getClass().getClassLoader().getResourceAsStream("dummy_graph.json");

        // Good practice: ensure the file was actually found before proceeding
        assertNotNull("Test resource file 'dummy_graph.json' not found in src/test/resources/", is);

        graph.load(is);
    }

    @Test
    public void testShortestPath_AvoidsRooms() {
        // Path H1 -> H2 is weight 1.
        // Path H1 -> R4 -> H2 is weight 4.
        // Even though H1->H2 is faster anyway, the penalty ensures it NEVER uses R4.
        List<String> path = graph.shortestPath("H1", "H2");

        assertNotNull(path);
        assertEquals(2, path.size());
        assertEquals("H1", path.get(0));
        assertEquals("H2", path.get(1));
    }

    @Test
    public void testShortestPath_ReachesDestinationRoom() {
        // Destination IS a room, so it shouldn't be penalized out of existence.
        List<String> path = graph.shortestPath("H1", "R3");

        assertNotNull(path);
        assertEquals(3, path.size());
        assertEquals("H1", path.get(0));
        assertEquals("H2", path.get(1));
        assertEquals("R3", path.get(2));
    }

    @Test
    public void testPathTransitPenalty() {
        // Simulate a vertical elevator ride between floors
        List<String> pathWithElevator = List.of("Elevator1", "Elevator2");
        int penalty = graph.pathTransitPenaltySeconds(pathWithElevator);

        assertEquals(45, penalty);
    }

    @Test
    public void testPathDistance() {
        List<String> path = List.of("H1", "H2", "R3");
        double distance = graph.pathDistance(path);

        assertEquals(6.0, distance, 0.01); // 1.0 + 5.0
    }

    // ── penaltyPerType — all branches ─────────────────────────────────────────

    @Test
    public void penaltyPerType_stair_returns30() {
        assertEquals(30, graph.penaltyPerType("stair"));
    }

    @Test
    public void penaltyPerType_escalator_returns20() {
        assertEquals(20, graph.penaltyPerType("escalator"));
    }

    @Test
    public void penaltyPerType_elevator_returns45() {
        assertEquals(45, graph.penaltyPerType("elevator"));
    }

    @Test
    public void penaltyPerType_walk_returnsZero() {
        assertEquals(0, graph.penaltyPerType("walk"));
    }

    @Test
    public void penaltyPerType_unknown_returnsZero() {
        assertEquals(0, graph.penaltyPerType("ramp"));
        assertEquals(0, graph.penaltyPerType(""));
    }

    @Test
    public void shortestPath_bothUnknown_returnsEmpty() {
        assertTrue(graph.shortestPath("FOO", "BAR").isEmpty());
    }

    // ── shortestPath — source equals target ───────────────────────────────────

    @Test
    public void shortestPath_sourceEqualsTarget_returnsEmpty() {
        // The algorithm never adds source to prev, so buildPath returns empty
        assertTrue(graph.shortestPath("H1", "H1").isEmpty());
    }

    // ── getNode ───────────────────────────────────────────────────────────────

    @Test
    public void getNode_existingId_returnsNode() {
        IndoorNode node = graph.getNode("H1");
        assertNotNull(node);
        assertEquals("hallway", node.getType());
    }

    @Test
    public void getNode_missingId_returnsNull() {
        assertNull(graph.getNode("DOES_NOT_EXIST"));
    }

    // ── getAllNodes ────────────────────────────────────────────────────────────

    @Test
    public void getAllNodes_returnsAllSixNodes() {
        assertEquals(6, graph.getAllNodes().size());
    }

    @Test
    public void getAllNodes_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> graph.getAllNodes().put("NEW", new IndoorNode()));
    }

    // ── pathDistance — edge cases ─────────────────────────────────────────────

    @Test
    public void pathDistance_emptyPath_returnsZero() {
        assertEquals(0.0, graph.pathDistance(Collections.emptyList()), 0.001);
    }

    @Test
    public void pathDistance_singleNode_returnsZero() {
        assertEquals(0.0, graph.pathDistance(List.of("H1")), 0.001);
    }

    // ── pathTransitPenaltySeconds — edge types ────────────────────────────────

    @Test
    public void pathTransitPenalty_emptyPath_returnsZero() {
        assertEquals(0, graph.pathTransitPenaltySeconds(Collections.emptyList()));
    }

    @Test
    public void pathTransitPenalty_walkEdge_returnsZero() {
        // H1→H2 is a "walk" edge → 0 extra seconds
        assertEquals(0, graph.pathTransitPenaltySeconds(List.of("H1", "H2")));
    }

    @Test
    public void pathTransitPenalty_elevatorEdge_returns45() {
        // Elevator1→Elevator2 is type "elevator" → 45 s
        assertEquals(45, graph.pathTransitPenaltySeconds(List.of("Elevator1", "Elevator2")));
    }

    // ── getLabeledRooms ───────────────────────────────────────────────────────

    @Test
    public void getLabeledRooms_noLabelsInDummyGraph_returnsEmpty() {
        // dummy_graph.json has no "label" fields → all nodes have empty label
        List<IndoorNode> rooms = graph.getLabeledRooms();
        assertNotNull(rooms);
        assertTrue(rooms.isEmpty());
    }

    @Test
    public void getLabeledRooms_withLabels_returnsLabeledNodes() throws Exception {
        String json = "{"
                + "\"nodes\":["
                + "{\"id\":\"n1\",\"label\":\"H-801\",\"type\":\"room\"},"
                + "{\"id\":\"n2\",\"label\":\"\",\"type\":\"hallway\"},"
                + "{\"id\":\"n3\",\"label\":\"H-802\",\"type\":\"room\"}"
                + "],"
                + "\"edges\":[]"
                + "}";
        IndoorGraph g = new IndoorGraph();
        g.load(toStream(json));

        List<IndoorNode> rooms = g.getLabeledRooms();
        assertEquals(2, rooms.size());
    }

    // ── stair / escalator edges via inline graph ──────────────────────────────

    @Test
    public void pathTransitPenalty_stairEdge_returns30() throws Exception {
        String json = "{"
                + "\"nodes\":["
                + "{\"id\":\"A\",\"type\":\"hallway\"},"
                + "{\"id\":\"B\",\"type\":\"hallway\"}"
                + "],"
                + "\"edges\":[{\"source\":\"A\",\"target\":\"B\",\"weight\":3.0,\"type\":\"stair\"}]"
                + "}";
        IndoorGraph g = new IndoorGraph();
        g.load(toStream(json));
        assertEquals(30, g.pathTransitPenaltySeconds(List.of("A", "B")));
    }

    @Test
    public void pathTransitPenalty_escalatorEdge_returns20() throws Exception {
        String json = "{"
                + "\"nodes\":["
                + "{\"id\":\"A\",\"type\":\"hallway\"},"
                + "{\"id\":\"B\",\"type\":\"hallway\"}"
                + "],"
                + "\"edges\":[{\"source\":\"A\",\"target\":\"B\",\"weight\":2.0,\"type\":\"escalator\"}]"
                + "}";
        IndoorGraph g = new IndoorGraph();
        g.load(toStream(json));
        assertEquals(20, g.pathTransitPenaltySeconds(List.of("A", "B")));
    }

    @Test
    public void shortestPath_simpleLinear_returnsCorrectPath() throws Exception {
        // A --1.0--> B --1.0--> C
        String json = "{"
                + "\"nodes\":["
                + "{\"id\":\"A\",\"type\":\"hallway\"},"
                + "{\"id\":\"B\",\"type\":\"hallway\"},"
                + "{\"id\":\"C\",\"type\":\"hallway\"}"
                + "],"
                + "\"edges\":["
                + "{\"source\":\"A\",\"target\":\"B\",\"weight\":1.0,\"type\":\"walk\"},"
                + "{\"source\":\"B\",\"target\":\"C\",\"weight\":1.0,\"type\":\"walk\"}"
                + "]"
                + "}";
        IndoorGraph g = new IndoorGraph();
        g.load(toStream(json));
        List<String> path = g.shortestPath("A", "C");
        assertEquals(List.of("A", "B", "C"), path);
    }

    @Test
    public void pathDistance_inlineGraph_computesCorrectly() throws Exception {
        String json = "{"
                + "\"nodes\":["
                + "{\"id\":\"X\",\"type\":\"hallway\"},"
                + "{\"id\":\"Y\",\"type\":\"hallway\"}"
                + "],"
                + "\"edges\":[{\"source\":\"X\",\"target\":\"Y\",\"weight\":7.5,\"type\":\"walk\"}]"
                + "}";
        IndoorGraph g = new IndoorGraph();
        g.load(toStream(json));
        assertEquals(7.5, g.pathDistance(List.of("X", "Y")), 0.001);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private InputStream toStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}