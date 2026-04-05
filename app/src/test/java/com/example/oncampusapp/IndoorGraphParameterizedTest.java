package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class IndoorGraphParameterizedTest {

    private IndoorGraph graph;
    private String source;
    private String target;

    // 1. Constructor takes the parameters
    public IndoorGraphParameterizedTest(String source, String target) {
        this.source = source;
        this.target = target;
    }

    // 2. Define the parameters
    @Parameterized.Parameters(name = "{index}: path({0}, {1})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "NONEXISTENT", "H2" },
                { "H1", "NONEXISTENT" },
                { "FOO", "BAR" }
        });
    }

    // 3. Setup the graph
    @Before
    public void setUp() throws Exception {
        graph = new IndoorGraph();
        InputStream is = getClass().getClassLoader().getResourceAsStream("dummy_graph.json");
        graph.load(is);
    }

    // 4. The single test method that runs for every parameter row
    @Test
    public void shortestPath_unknownNodes_returnsEmpty() {
        List<String> path = graph.shortestPath(source, target);
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }
}