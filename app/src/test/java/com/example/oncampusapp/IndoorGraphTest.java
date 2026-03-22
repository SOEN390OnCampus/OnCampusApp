package com.example.oncampusapp;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

public class IndoorGraphTest {

    private IndoorGraph graph;

    @Before
    public void setUp() throws IOException, JSONException {
        graph = new IndoorGraph();

        // A mini-map to test Dijkstra and the room penalty.
        String dummyJson = "{" +
                "  \"nodes\": [" +
                "    {\"id\": \"H1\", \"type\": \"hallway\"}," +
                "    {\"id\": \"H2\", \"type\": \"hallway\"}," +
                "    {\"id\": \"R3\", \"type\": \"room\"}," +
                "    {\"id\": \"R4\", \"type\": \"room\"}," +
                "    {\"id\": \"Elevator1\", \"type\": \"elevator\"}," +
                "    {\"id\": \"Elevator2\", \"type\": \"elevator\"}" +
                "  ]," +
                "  \"edges\": [" +
                "    {\"source\": \"H1\", \"target\": \"H2\", \"weight\": 1.0, \"type\": \"walk\"}," + // Direct walk
                "    {\"source\": \"H2\", \"target\": \"R3\", \"weight\": 5.0, \"type\": \"walk\"}," +
                "    {\"source\": \"H1\", \"target\": \"R4\", \"weight\": 2.0, \"type\": \"walk\"}," +
                "    {\"source\": \"R4\", \"target\": \"H2\", \"weight\": 2.0, \"type\": \"walk\"}," +
                "    {\"source\": \"Elevator1\", \"target\": \"Elevator2\", \"weight\": 0.0, \"type\": \"elevator\"}" + // Vertical connection
                "  ]" +
                "}";

        ByteArrayInputStream is = new ByteArrayInputStream(dummyJson.getBytes(StandardCharsets.UTF_8));
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
}