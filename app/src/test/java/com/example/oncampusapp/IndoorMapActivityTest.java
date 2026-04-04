package com.example.oncampusapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class IndoorMapActivityTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    // ── Lifecycle & Intent Validation Tests ───────────────────────────────────

    @Test
    public void testActivityFinishes_whenMissingBuildingOrFloorId() {
        // Setup: Launch the activity with a completely empty Intent
        Intent emptyIntent = new Intent(context, IndoorMapActivity.class);
        ActivityController<IndoorMapActivity> controller = Robolectric.buildActivity(IndoorMapActivity.class, emptyIntent);

        // Action: Create the activity
        IndoorMapActivity activity = controller.create().get();

        // Assertion: validateInputs() should have triggered finish()
        assertTrue("Activity should finish immediately if required intent extras are missing", activity.isFinishing());
    }

    @Test
    public void testActivityInitializes_withValidIntentExtras() {
        // Setup: Provide the required intent extras
        Intent validIntent = new Intent(context, IndoorMapActivity.class);
        validIntent.putExtra("BUILDING_ID", "H");
        validIntent.putExtra("FLOOR_ID", "8");

        // Action: Create the activity
        IndoorMapActivity activity = Robolectric.buildActivity(IndoorMapActivity.class, validIntent)
                .create()
                .get();

        // Assertion 1: Activity should stay alive
        assertTrue("Activity should not finish if valid extras are provided", !activity.isFinishing());

        // Assertion 2: Verify the title text was set correctly using the Intent data
        TextView tvTitle = activity.findViewById(R.id.tv_floor_title);
        assertEquals("H FLOOR 8", tvTitle.getText().toString());
    }

    // ── Turn Calculation Math Tests (Reflection) ──────────────────────────────

    @Test
    public void testComputeTurnType_detectsLeftTurn() throws Exception {
        IndoorMapActivity activity = Robolectric.buildActivity(IndoorMapActivity.class).get();
        Method computeTurnType = IndoorMapActivity.class.getDeclaredMethod(
                "computeTurnType", IndoorNode.class, IndoorNode.class, IndoorNode.class);
        computeTurnType.setAccessible(true);

        // Setup: In screen coords (y-down), moving DOWN then RIGHT is a LEFT turn relative to the walker.
        IndoorNode a = new IndoorNode.Builder().x(0).y(0).build();
        IndoorNode b = new IndoorNode.Builder().x(0).y(100).build();
        IndoorNode c = new IndoorNode.Builder().x(100).y(100).build();

        Object result = computeTurnType.invoke(activity, a, b, c);
        assertEquals("Should detect a left turn", "TURN_LEFT", result.toString());
    }

    @Test
    public void testComputeTurnType_detectsRightTurn() throws Exception {
        IndoorMapActivity activity = Robolectric.buildActivity(IndoorMapActivity.class).get();
        Method computeTurnType = IndoorMapActivity.class.getDeclaredMethod(
                "computeTurnType", IndoorNode.class, IndoorNode.class, IndoorNode.class);
        computeTurnType.setAccessible(true);

        // Setup: Moving DOWN then LEFT is a RIGHT turn.
        IndoorNode a = new IndoorNode.Builder().x(100).y(0).build();
        IndoorNode b = new IndoorNode.Builder().x(100).y(100).build();
        IndoorNode c = new IndoorNode.Builder().x(0).y(100).build();

        Object result = computeTurnType.invoke(activity, a, b, c);
        assertEquals("Should detect a right turn", "TURN_RIGHT", result.toString());
    }

    @Test
    public void testComputeTurnType_detectsGoStraightWhenSegmentIsTooShort() throws Exception {
        IndoorMapActivity activity = Robolectric.buildActivity(IndoorMapActivity.class).get();
        Method computeTurnType = IndoorMapActivity.class.getDeclaredMethod(
                "computeTurnType", IndoorNode.class, IndoorNode.class, IndoorNode.class);
        computeTurnType.setAccessible(true);

        // Setup: Move DOWN 100px, but the next turn is only 10px long (Below the 80px threshold)
        IndoorNode a = new IndoorNode.Builder().x(0).y(0).build();
        IndoorNode b = new IndoorNode.Builder().x(0).y(100).build();
        IndoorNode c = new IndoorNode.Builder().x(10).y(100).build();

        Object result = computeTurnType.invoke(activity, a, b, c);
        assertEquals("Should ignore short jaggies and say go straight", "GO_STRAIGHT", result.toString());
    }

    // ── Bottom Navigation Tests ───────────────────────────────────────────────

    @Test
    public void testBottomNav_clickHome_startsMapsActivityAndClearsTop() {
        Intent validIntent = new Intent(context, IndoorMapActivity.class);
        validIntent.putExtra("BUILDING_ID", "H");
        validIntent.putExtra("FLOOR_ID", "8");
        IndoorMapActivity activity = Robolectric.buildActivity(IndoorMapActivity.class, validIntent).create().get();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertEquals(MapsActivity.class.getName(), actualIntent.getComponent().getClassName());

        // Ensure the CLEAR_TOP and SINGLE_TOP flags were added to prevent backstack buildup
        int expectedFlags = Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
        assertEquals(expectedFlags, actualIntent.getFlags() & expectedFlags);
    }

    // ── Your Existing JSON Parsing Tests ──────────────────────────────────────

    @Test
    public void testLoadNodesForFloor_handlesStandardAndMBBasements() throws Exception {
        IndoorMapActivity activity = Robolectric.buildActivity(IndoorMapActivity.class).get();
        Resources mockResources = Mockito.mock(Resources.class);

        String fakeJson = "{ \"nodes\": [" +
                "{ \"floor\": \"8\", \"buildingId\": \"H\", \"label\": \"H-807\", \"x\": 100, \"y\": 200 }," +
                "{ \"floor\": \"2\", \"buildingId\": \"MB-S2\", \"label\": \"MB-S2.440\", \"x\": 300, \"y\": 400 }" +
                "]}";

        InputStream fakeInputStream = new ByteArrayInputStream(fakeJson.getBytes(StandardCharsets.UTF_8));

        // Inject the mocked resources into our real Activity via reflection or a spy.
        // Since getResources() is final/hard to mock on a real activity, we mock the Context wrapper:
        IndoorMapActivity spyActivity = Mockito.spy(activity);
        when(spyActivity.getResources()).thenReturn(mockResources);
        when(mockResources.openRawResource(anyInt())).thenReturn(fakeInputStream);

        Method loadNodesMethod = IndoorMapActivity.class.getDeclaredMethod("loadNodesForFloor", int.class, String.class);
        loadNodesMethod.setAccessible(true);

        // Test Match Hall Floor 8
        List<IndoorNode> resultH8 = (List<IndoorNode>) loadNodesMethod.invoke(spyActivity, 1, "8");
        assertEquals(1, resultH8.size());
        assertEquals("H-807", resultH8.get(0).getLabel());

        fakeInputStream.reset();

        // Test MB-S2 Quirk
        List<IndoorNode> resultS2 = (List<IndoorNode>) loadNodesMethod.invoke(spyActivity, 1, "S2");
        assertEquals(1, resultS2.size());

        boolean foundMBS2 = false;
        for (IndoorNode node : resultS2) {
            if (node.getLabel().equals("MB-S2.440")) foundMBS2 = true;
        }
        assertTrue("Failed to find the MB-S2 room via buildingId quirk", foundMBS2);
    }

    @Test
    public void testLoadNodesForFloor_skipsInvalidAndEmptyNodes() throws Exception {
        IndoorMapActivity activity = Robolectric.buildActivity(IndoorMapActivity.class).get();
        Resources mockResources = Mockito.mock(Resources.class);

        String dirtyJson = "{ \"nodes\": [" +
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"label\": \"H-101\", \"x\": 10, \"y\": 20 }," +
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"label\": \"\", \"x\": 10, \"y\": 20 }," +
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"x\": 10, \"y\": 20 }," +
                "{ \"floor\": \"2\", \"buildingId\": \"H\", \"label\": \"H-201\", \"x\": 10, \"y\": 20 }" +
                "]}";

        InputStream fakeInputStream = new ByteArrayInputStream(dirtyJson.getBytes(StandardCharsets.UTF_8));

        IndoorMapActivity spyActivity = Mockito.spy(activity);
        when(spyActivity.getResources()).thenReturn(mockResources);
        when(mockResources.openRawResource(anyInt())).thenReturn(fakeInputStream);

        Method loadNodesMethod = IndoorMapActivity.class.getDeclaredMethod("loadNodesForFloor", int.class, String.class);
        loadNodesMethod.setAccessible(true);

        List<IndoorNode> result = (List<IndoorNode>) loadNodesMethod.invoke(spyActivity, 1, "1");

        assertEquals(1, result.size());
        assertEquals("H-101", result.get(0).getLabel());
    }
}