package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class IndoorGraphRoutingTest {

    private IndoorGraph graph;

    /**
     * Minimal graph JSON:
     *
     *  [A] --hallway--> [STAIRS] --stair--> [B]
     *   \                                   /
     *    \------elevator--> [ELEV] --------/
     *
     * shortestPath()           → A → STAIRS → B  (shortest by weight)
     * shortestAccessiblePath() → A → ELEV   → B  (avoids stairs)
     */
    private static final String TEST_GRAPH_JSON = "{\n" +
            "  \"nodes\": [\n" +
            "    {\"id\":\"A\",     \"label\":\"Room A\",   \"type\":\"room\",     \"floor\":\"1\", \"buildingId\":\"H\", \"x\":0, \"y\":0, \"accessible\":true},\n" +
            "    {\"id\":\"STAIRS\",\"label\":\"\",          \"type\":\"staircase\",\"floor\":\"1\", \"buildingId\":\"H\", \"x\":1, \"y\":0, \"accessible\":false},\n" +
            "    {\"id\":\"ELEV\",  \"label\":\"\",          \"type\":\"elevator\", \"floor\":\"1\", \"buildingId\":\"H\", \"x\":0, \"y\":1, \"accessible\":true},\n" +
            "    {\"id\":\"B\",     \"label\":\"Room B\",   \"type\":\"room\",     \"floor\":\"2\", \"buildingId\":\"H\", \"x\":1, \"y\":1, \"accessible\":true}\n" +
            "  ],\n" +
            "  \"edges\": [\n" +
            "    {\"from\":\"A\",      \"to\":\"STAIRS\", \"weight\":1},\n" +
            "    {\"from\":\"STAIRS\", \"to\":\"B\",      \"weight\":1},\n" +
            "    {\"from\":\"A\",      \"to\":\"ELEV\",   \"weight\":2},\n" +
            "    {\"from\":\"ELEV\",   \"to\":\"B\",      \"weight\":2}\n" +
            "  ]\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        graph = new IndoorGraph();
        InputStream is = new ByteArrayInputStream(TEST_GRAPH_JSON.getBytes("UTF-8"));
        graph.load(is);
    }

    @Test
    public void shortestPath_usesStairs() {
        List<String> path = graph.shortestPath("A", "B");
        assertFalse("Path should not be empty", path.isEmpty());
        assertTrue("Normal path should go through stairs", path.contains("STAIRS"));
        assertFalse("Normal path should not use elevator", path.contains("ELEV"));
    }

    @Test
    public void shortestAccessiblePath_avoidsStairs() {
        List<String> path = graph.shortestAccessiblePath("A", "B");
        assertFalse("Accessible path should not be empty", path.isEmpty());
        assertFalse("Accessible path must not contain stairs", path.contains("STAIRS"));
        assertTrue("Accessible path should use elevator", path.contains("ELEV"));
    }

    @Test
    public void shortestPath_containsStartAndEnd() {
        List<String> path = graph.shortestPath("A", "B");
        assertEquals("First node should be A", "A", path.get(0));
        assertEquals("Last node should be B", "B", path.get(path.size() - 1));
    }

    @Test
    public void shortestAccessiblePath_containsStartAndEnd() {
        List<String> path = graph.shortestAccessiblePath("A", "B");
        assertEquals("First node should be A", "A", path.get(0));
        assertEquals("Last node should be B", "B", path.get(path.size() - 1));
    }

    @Test
    public void shortestPath_invalidNode_returnsEmpty() {
        List<String> path = graph.shortestPath("A", "NONEXISTENT");
        assertTrue("Should return empty list for unknown node", path.isEmpty());
    }

    @Test
    public void shortestAccessiblePath_invalidNode_returnsEmpty() {
        List<String> path = graph.shortestAccessiblePath("A", "NONEXISTENT");
        assertTrue("Should return empty list for unknown node", path.isEmpty());
    }
}