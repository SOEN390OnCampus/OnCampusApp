package com.example.oncampusapp;

import android.content.res.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class IndoorMapActivityTest {

    @Mock
    IndoorMapActivity mockActivity;

    @Mock
    Resources mockResources;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLoadNodesForFloor_handlesStandardAndMBBasements() throws Exception {
        String fakeJson = "{ \"nodes\": [" +
                "{ \"floor\": \"8\", \"buildingId\": \"H\", \"label\": \"H-807\", \"x\": 100, \"y\": 200 }," +
                "{ \"floor\": \"2\", \"buildingId\": \"MB-S2\", \"label\": \"MB-S2.440\", \"x\": 300, \"y\": 400 }" +
                "]}";

        InputStream fakeInputStream = new ByteArrayInputStream(fakeJson.getBytes(StandardCharsets.UTF_8));
        when(mockActivity.getResources()).thenReturn(mockResources);
        when(mockResources.openRawResource(anyInt())).thenReturn(fakeInputStream);

        // Prepare Reflection to call the private loadNodesForFloor method
        Method loadNodesMethod = IndoorMapActivity.class.getDeclaredMethod("loadNodesForFloor", int.class, String.class);
        loadNodesMethod.setAccessible(true);

        // Test Match Hall Floor 8
        List<IndoorNode> resultH8 = (List<IndoorNode>) loadNodesMethod.invoke(mockActivity, 1, "8");
        assertEquals(1, resultH8.size());
        assertEquals("H-807", resultH8.get(0).getLabel());

        // Reset stream for next call
        fakeInputStream.reset();

        // Test MB-S2 Quirk
        List<IndoorNode> resultS2 = (List<IndoorNode>) loadNodesMethod.invoke(mockActivity, 1, "S2");

        assertEquals(1, resultS2.size());

        boolean foundMBS2 = false;
        for (IndoorNode node : resultS2) {
            if (node.getLabel().equals("MB-S2.440")) foundMBS2 = true;
        }

        assertTrue("Failed to find the MB-S2 room via buildingId quirk", foundMBS2);
    }

    @Test
    public void testLoadNodesForFloor_skipsInvalidAndEmptyNodes() throws Exception {
        String dirtyJson = "{ \"nodes\": [" +
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"label\": \"H-101\", \"x\": 10, \"y\": 20 }," + // Valid
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"label\": \"\", \"x\": 10, \"y\": 20 }," + // Empty label (should skip)
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"x\": 10, \"y\": 20 }," + // Missing label key entirely (should skip)
                "{ \"floor\": \"2\", \"buildingId\": \"H\", \"label\": \"H-201\", \"x\": 10, \"y\": 20 }" + // Wrong floor (should skip)
                "]}";

        InputStream fakeInputStream = new ByteArrayInputStream(dirtyJson.getBytes(StandardCharsets.UTF_8));
        when(mockActivity.getResources()).thenReturn(mockResources);
        when(mockResources.openRawResource(anyInt())).thenReturn(fakeInputStream);

        Method loadNodesMethod = IndoorMapActivity.class.getDeclaredMethod("loadNodesForFloor", int.class, String.class);
        loadNodesMethod.setAccessible(true);

        List<IndoorNode> result = (List<IndoorNode>) loadNodesMethod.invoke(mockActivity, 1, "1");

        assertEquals(1, result.size());
        assertEquals("H-101", result.get(0).getLabel());
    }
}