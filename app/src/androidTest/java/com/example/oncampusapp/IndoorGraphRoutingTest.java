package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import static org.junit.Assert.*;

public class IndoorGraphRoutingTest {

    private IndoorGraph graph;

    private static final String TEST_GRAPH_JSON = "{\n" +
            "  \"nodes\": [\n" +
            "    {\"id\":\"A\",      \"label\":\"Room A\", \"type\":\"room\",      \"floor\":\"1\", \"buildingId\":\"H\", \"x\":0, \"y\":0, \"accessible\":true},\n" +
            "    {\"id\":\"STAIRS\", \"label\":\"\",        \"type\":\"staircase\", \"floor\":\"1\", \"buildingId\":\"H\", \"x\":1, \"y\":0, \"accessible\":false},\n" +
            "    {\"id\":\"ELEV\",   \"label\":\"\",        \"type\":\"elevator\",  \"floor\":\"1\", \"buildingId\":\"H\", \"x\":0, \"y\":1, \"accessible\":true},\n" +
            "    {\"id\":\"B\",      \"label\":\"Room B\", \"type\":\"room\",      \"floor\":\"2\", \"buildingId\":\"H\", \"x\":1, \"y\":1, \"accessible\":true}\n" +
            "  ],\n" +
            "  \"edges\": [\n" +
            "    {\"source\":\"A\",      \"target\":\"STAIRS\", \"weight\":1, \"type\":\"stair\",    \"accessible\":false},\n" +
            "    {\"source\":\"STAIRS\", \"target\":\"B\",      \"weight\":1, \"type\":\"stair\",    \"accessible\":false},\n" +
            "    {\"source\":\"A\",      \"target\":\"ELEV\",   \"weight\":2, \"type\":\"elevator\", \"accessible\":true},\n" +
            "    {\"source\":\"ELEV\",   \"target\":\"B\",      \"weight\":2, \"type\":\"elevator\", \"accessible\":true}\n" +
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
        assertFalse(path.isEmpty());
        assertTrue(path.contains("STAIRS"));
        assertFalse(path.contains("ELEV"));
    }

    @Test
    public void shortestAccessiblePath_avoidsStairs() {
        List<String> path = graph.shortestAccessiblePath("A", "B");
        assertFalse(path.isEmpty());
        assertFalse(path.contains("STAIRS"));
        assertTrue(path.contains("ELEV"));
    }

    @Test
    public void shortestPath_containsStartAndEnd() {
        List<String> path = graph.shortestPath("A", "B");
        assertEquals("A", path.get(0));
        assertEquals("B", path.get(path.size() - 1));
    }

    @Test
    public void shortestAccessiblePath_containsStartAndEnd() {
        List<String> path = graph.shortestAccessiblePath("A", "B");
        assertEquals("A", path.get(0));
        assertEquals("B", path.get(path.size() - 1));
    }

    @Test
    public void shortestPath_unknownNode_returnsEmpty() {
        List<String> path = graph.shortestPath("A", "NONEXISTENT");
        assertTrue(path.isEmpty());
    }

    @Test
    public void shortestAccessiblePath_unknownNode_returnsEmpty() {
        List<String> path = graph.shortestAccessiblePath("A", "NONEXISTENT");
        assertTrue(path.isEmpty());
    }
}
