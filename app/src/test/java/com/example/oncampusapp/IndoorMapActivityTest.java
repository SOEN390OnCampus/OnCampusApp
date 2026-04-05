package com.example.oncampusapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class IndoorMapActivityTest {

    private Context context;
    private IndoorMapActivity initializedActivity;
    private Class<?> stepTypeEnumClass;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        // Build a standard initialized activity for tests that need the UI bound
        Intent validIntent = new Intent(context, IndoorMapActivity.class);
        validIntent.putExtra("BUILDING_ID", "H");
        validIntent.putExtra("FLOOR_ID", "8");
        initializedActivity = Robolectric.buildActivity(IndoorMapActivity.class, validIntent).create().get();

        stepTypeEnumClass = Class.forName("com.example.oncampusapp.IndoorMapActivity$StepType");
    }

    // ── Helper Methods for Reflection ─────────────────────────────────────────

    private Object getEnum(String name) {
        return Enum.valueOf((Class<Enum>) stepTypeEnumClass, name);
    }

    private Object createNavigationStep(Object type, String title, String subtitle, String floorId) throws Exception {
        Class<?> navStepClass = Class.forName("com.example.oncampusapp.IndoorMapActivity$NavigationStep");
        Constructor<?> constructor = navStepClass.getDeclaredConstructor(
                stepTypeEnumClass, String.class, String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(type, title, subtitle, floorId);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Lifecycle & Intent Validation Tests ───────────────────────────────────

    @Test
    public void testActivityFinishes_whenMissingBuildingOrFloorId() {
        Intent emptyIntent = new Intent(context, IndoorMapActivity.class);
        ActivityController<IndoorMapActivity> controller = Robolectric.buildActivity(IndoorMapActivity.class, emptyIntent);
        IndoorMapActivity activity = controller.create().get();
        assertTrue("Activity should finish immediately if required intent extras are missing", activity.isFinishing());
    }

    @Test
    public void testActivityInitializes_withValidIntentExtras() {
        assertFalse("Activity should not finish if valid extras are provided", initializedActivity.isFinishing());
        TextView tvTitle = initializedActivity.findViewById(R.id.tv_floor_title);
        assertEquals("H FLOOR 8", tvTitle.getText().toString());
    }

    // ── Bottom Navigation Tests ───────────────────────────────────────────────

    @Test
    public void testBottomNav_clickHome_startsMapsActivityAndClearsTop() {
        BottomNavigationView bottomNav = initializedActivity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        ShadowActivity shadowActivity = Shadows.shadowOf(initializedActivity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertEquals(MapsActivity.class.getName(), actualIntent.getComponent().getClassName());

        // Ensure the CLEAR_TOP and SINGLE_TOP flags were added to prevent backstack buildup
        int expectedFlags = Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
        assertEquals(expectedFlags, actualIntent.getFlags() & expectedFlags);
    }

    @Test
    public void testBottomNav_clickAccount_startsAuthActivity() {
        BottomNavigationView bottomNav = initializedActivity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_account);

        ShadowActivity shadowActivity = Shadows.shadowOf(initializedActivity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertEquals(GoogleCalendarAuthActivity.class.getName(), actualIntent.getComponent().getClassName());
    }

    @Test
    public void testBottomNav_clickSettings_startsSettingsActivity() {
        BottomNavigationView bottomNav = initializedActivity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_settings);

        ShadowActivity shadowActivity = Shadows.shadowOf(initializedActivity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertEquals(SettingsActivity.class.getName(), actualIntent.getComponent().getClassName());
    }

    // ── Turn Calculation Math Tests (Reflection) ──────────────────────────────

    @Test
    public void testComputeTurnType_detectsLeftTurn() throws Exception {
        Method computeTurnType = IndoorMapActivity.class.getDeclaredMethod(
                "computeTurnType", IndoorNode.class, IndoorNode.class, IndoorNode.class);
        computeTurnType.setAccessible(true);

        IndoorNode a = new IndoorNode.Builder().x(0).y(0).build();
        IndoorNode b = new IndoorNode.Builder().x(0).y(100).build();
        IndoorNode c = new IndoorNode.Builder().x(100).y(100).build();

        Object result = computeTurnType.invoke(initializedActivity, a, b, c);
        assertEquals("TURN_LEFT", result.toString());
    }

    @Test
    public void testComputeTurnType_detectsRightTurn() throws Exception {
        Method computeTurnType = IndoorMapActivity.class.getDeclaredMethod(
                "computeTurnType", IndoorNode.class, IndoorNode.class, IndoorNode.class);
        computeTurnType.setAccessible(true);

        IndoorNode a = new IndoorNode.Builder().x(100).y(0).build();
        IndoorNode b = new IndoorNode.Builder().x(100).y(100).build();
        IndoorNode c = new IndoorNode.Builder().x(0).y(100).build();

        Object result = computeTurnType.invoke(initializedActivity, a, b, c);
        assertEquals("TURN_RIGHT", result.toString());
    }

    @Test
    public void testComputeTurnType_detectsGoStraightWhenSegmentIsTooShort() throws Exception {
        Method computeTurnType = IndoorMapActivity.class.getDeclaredMethod(
                "computeTurnType", IndoorNode.class, IndoorNode.class, IndoorNode.class);
        computeTurnType.setAccessible(true);

        // Setup: Move DOWN 100px, but the next turn is only 10px long (Below the 80px threshold)
        IndoorNode a = new IndoorNode.Builder().x(0).y(0).build();
        IndoorNode b = new IndoorNode.Builder().x(0).y(100).build();
        IndoorNode c = new IndoorNode.Builder().x(10).y(100).build();

        Object result = computeTurnType.invoke(initializedActivity, a, b, c);
        assertEquals("Should ignore short jaggies and say go straight", "GO_STRAIGHT", result.toString());
    }

    // ── Node Logic & Property Checks (Reflection) ─────────────────────────────

    @Test
    public void testSafeString_handlesNullAndValidStrings() throws Exception {
        Method safeString = IndoorMapActivity.class.getDeclaredMethod("safeString", String.class);
        safeString.setAccessible(true);

        assertEquals("", safeString.invoke(initializedActivity, (Object) null));
        assertEquals("Valid", safeString.invoke(initializedActivity, "Valid"));
    }

    @Test
    public void testIsTurnCandidate_detectsHallwaysAndDoorways() throws Exception {
        Method isTurnCandidate = IndoorMapActivity.class.getDeclaredMethod("isTurnCandidate", IndoorNode.class);
        isTurnCandidate.setAccessible(true);

        assertTrue((Boolean) isTurnCandidate.invoke(initializedActivity, new IndoorNode.Builder().type("hallway").build()));
        assertTrue((Boolean) isTurnCandidate.invoke(initializedActivity, new IndoorNode.Builder().type("doorway").build()));
        assertFalse((Boolean) isTurnCandidate.invoke(initializedActivity, new IndoorNode.Builder().type("room").build()));
        assertFalse((Boolean) isTurnCandidate.invoke(initializedActivity, new IndoorNode.Builder().build()));
    }

    @Test
    public void testGetTransitionType_mapsTypesToEnums() throws Exception {
        Method getTransitionType = IndoorMapActivity.class.getDeclaredMethod("getTransitionType", IndoorNode.class);
        getTransitionType.setAccessible(true);

        assertEquals("TAKE_ESCALATOR", getTransitionType.invoke(initializedActivity, new IndoorNode.Builder().type("escalator").build()).toString());
        assertEquals("TAKE_STAIRS", getTransitionType.invoke(initializedActivity, new IndoorNode.Builder().type("stair").build()).toString());
        assertEquals("TAKE_ELEVATOR", getTransitionType.invoke(initializedActivity, new IndoorNode.Builder().type("elevator").build()).toString());
        assertNull(getTransitionType.invoke(initializedActivity, new IndoorNode.Builder().type("room").build()));
        assertNull(getTransitionType.invoke(initializedActivity, new IndoorNode.Builder().build())); // Null type check
    }

    @Test
    public void testIsTransitOnly_detectsTransitFloors() throws Exception {
        Method isTransitOnly = IndoorMapActivity.class.getDeclaredMethod("isTransitOnly", List.class);
        isTransitOnly.setAccessible(true);

        assertTrue((Boolean) isTransitOnly.invoke(initializedActivity, new ArrayList<>()));
        assertTrue((Boolean) isTransitOnly.invoke(initializedActivity, Arrays.asList(
                new IndoorNode.Builder().type("elevator").build(),
                new IndoorNode.Builder().type("stair").build()
        )));
        assertFalse((Boolean) isTransitOnly.invoke(initializedActivity, Arrays.asList(
                new IndoorNode.Builder().type("elevator").build(),
                new IndoorNode.Builder().type("room").build()
        )));
    }

    @Test
    public void testIsInvalidFloor_handlesNullAndEmpty() throws Exception {
        Method isInvalidFloor = IndoorMapActivity.class.getDeclaredMethod("isInvalidFloor", List.class);
        isInvalidFloor.setAccessible(true);

        assertTrue((Boolean) isInvalidFloor.invoke(initializedActivity, (Object) null));
        assertTrue((Boolean) isInvalidFloor.invoke(initializedActivity, new ArrayList<IndoorNode>()));
        assertFalse((Boolean) isInvalidFloor.invoke(initializedActivity, Arrays.asList(new IndoorNode.Builder().build())));
    }

    // ── String Formatting & UI Math (Reflection) ──────────────────────────────

    @Test
    public void testTransitionTitle_formatsCorrectly() throws Exception {
        Method transitionTitle = IndoorMapActivity.class.getDeclaredMethod("transitionTitle", stepTypeEnumClass, String.class);
        transitionTitle.setAccessible(true);

        assertEquals("Take Staircase to Floor 8", transitionTitle.invoke(initializedActivity, getEnum("TAKE_STAIRS"), "8"));
        assertEquals("Take Escalator to Floor 2", transitionTitle.invoke(initializedActivity, getEnum("TAKE_ESCALATOR"), "2"));
        assertEquals("Take Elevator to Floor 5", transitionTitle.invoke(initializedActivity, getEnum("TAKE_ELEVATOR"), "5"));
    }

    @Test
    public void testUpdateRouteTime_setsText() throws Exception {
        Method updateRouteTime = IndoorMapActivity.class.getDeclaredMethod("updateRouteTime", int.class);
        updateRouteTime.setAccessible(true);

        updateRouteTime.invoke(initializedActivity, 12);

        TextView tvTime = initializedActivity.findViewById(R.id.tv_route_time);
        assertEquals("12 min", tvTime.getText().toString());
    }

    // ── JSON Parsing Tests ────────────────────────────────────────────────────

    @Test
    public void testLoadNodesForFloor_handlesStandardAndMBBasements() throws Exception {
        Resources mockResources = Mockito.mock(Resources.class);

        String fakeJson = "{ \"nodes\": [" +
                "{ \"floor\": \"8\", \"buildingId\": \"H\", \"label\": \"H-807\", \"x\": 100, \"y\": 200 }," +
                "{ \"floor\": \"2\", \"buildingId\": \"MB-S2\", \"label\": \"MB-S2.440\", \"x\": 300, \"y\": 400 }" +
                "]}";

        InputStream fakeInputStream = new ByteArrayInputStream(fakeJson.getBytes(StandardCharsets.UTF_8));

        IndoorMapActivity spyActivity = Mockito.spy(initializedActivity);
        when(spyActivity.getResources()).thenReturn(mockResources);
        when(mockResources.openRawResource(anyInt())).thenReturn(fakeInputStream);

        Method loadNodesMethod = IndoorMapActivity.class.getDeclaredMethod("loadNodesForFloor", int.class, String.class);
        loadNodesMethod.setAccessible(true);

        List<IndoorNode> resultH8 = (List<IndoorNode>) loadNodesMethod.invoke(spyActivity, 1, "8");
        assertEquals(1, resultH8.size());
        assertEquals("H-807", resultH8.get(0).getLabel());

        fakeInputStream.reset();

        List<IndoorNode> resultS2 = (List<IndoorNode>) loadNodesMethod.invoke(spyActivity, 1, "S2");
        assertEquals(1, resultS2.size());
        assertEquals("MB-S2.440", resultS2.get(0).getLabel());
    }

    @Test
    public void testLoadNodesForFloor_skipsInvalidAndEmptyNodes() throws Exception {
        Resources mockResources = Mockito.mock(Resources.class);

        String dirtyJson = "{ \"nodes\": [" +
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"label\": \"H-101\", \"x\": 10, \"y\": 20 }," +
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"label\": \"\", \"x\": 10, \"y\": 20 }," +
                "{ \"floor\": \"1\", \"buildingId\": \"H\", \"x\": 10, \"y\": 20 }," +
                "{ \"floor\": \"2\", \"buildingId\": \"H\", \"label\": \"H-201\", \"x\": 10, \"y\": 20 }" +
                "]}";

        InputStream fakeInputStream = new ByteArrayInputStream(dirtyJson.getBytes(StandardCharsets.UTF_8));

        IndoorMapActivity spyActivity = Mockito.spy(initializedActivity);
        when(spyActivity.getResources()).thenReturn(mockResources);
        when(mockResources.openRawResource(anyInt())).thenReturn(fakeInputStream);

        Method loadNodesMethod = IndoorMapActivity.class.getDeclaredMethod("loadNodesForFloor", int.class, String.class);
        loadNodesMethod.setAccessible(true);

        List<IndoorNode> result = (List<IndoorNode>) loadNodesMethod.invoke(spyActivity, 1, "1");

        assertEquals(1, result.size());
        assertEquals("H-101", result.get(0).getLabel());
    }

    // ── Navigation Step Filter Logic ──────────────────────────────────────────

    @Test
    public void testFilterSteps_removesRedundantGoStraightCommands() throws Exception {
        Method filterStepsMethod = IndoorMapActivity.class.getDeclaredMethod("filterSteps", List.class);
        filterStepsMethod.setAccessible(true);

        List<Object> rawSteps = new ArrayList<>();
        // This Go Straight is BEFORE the transition -> filtered out
        rawSteps.add(createNavigationStep(getEnum("GO_STRAIGHT"), "Go Straight", "F8", "8"));

        // The Transition -> kept
        rawSteps.add(createNavigationStep(getEnum("TAKE_STAIRS"), "Take Stairs", "F8", "8"));

        // This Go Straight is AFTER the transition -> filtered out
        rawSteps.add(createNavigationStep(getEnum("GO_STRAIGHT"), "Go Straight", "F7", "7"));

        // The Arrival -> kept
        rawSteps.add(createNavigationStep(getEnum("ARRIVE"), "Arrived", "F7", "7"));

        List<?> filtered = (List<?>) filterStepsMethod.invoke(initializedActivity, rawSteps);

        // Expect exactly 2 steps to remain (The stairs and the arrival)
        assertEquals("Should remove the Go Straight before AND after the stairs", 2, filtered.size());

        // Verify the exact steps that survived
        assertEquals("TAKE_STAIRS", getStepType(filtered.get(0)));
        assertEquals("ARRIVE", getStepType(filtered.get(1)));
    }

    private String getStepType(Object navStep) throws Exception {
        Field typeField = navStep.getClass().getDeclaredField("type");
        typeField.setAccessible(true);
        return typeField.get(navStep).toString();
    }

    // ── UI Visibility Logic ───────────────────────────────────────────────────

    @Test
    public void testShowRoutePanels_hidesSearchBarAndShowsCards() throws Exception {
        Method showRoutePanels = IndoorMapActivity.class.getDeclaredMethod("showRoutePanels");
        showRoutePanels.setAccessible(true);

        showRoutePanels.invoke(initializedActivity);

        assertEquals(View.GONE, initializedActivity.findViewById(R.id.et_indoor_search).getVisibility());
        assertEquals(View.VISIBLE, initializedActivity.findViewById(R.id.row_info_cards).getVisibility());
        assertEquals(View.VISIBLE, initializedActivity.findViewById(R.id.card_current_step).getVisibility());
        assertEquals(View.VISIBLE, initializedActivity.findViewById(R.id.card_next_step).getVisibility());
    }

    // ── Step Display Logic ────────────────────────────────────────────────────

    @Test
    public void testUpdateNextCard_handlesNullGracefully() throws Exception {
        Method updateNextCard = IndoorMapActivity.class.getDeclaredMethod("updateNextCard", Class.forName("com.example.oncampusapp.IndoorMapActivity$NavigationStep"));
        updateNextCard.setAccessible(true);

        // Ensure UI panels are visible first so we can grab the TextView
        Method showRoutePanels = IndoorMapActivity.class.getDeclaredMethod("showRoutePanels");
        showRoutePanels.setAccessible(true);
        showRoutePanels.invoke(initializedActivity);

        updateNextCard.invoke(initializedActivity, (Object) null);

        TextView tvNext = initializedActivity.findViewById(R.id.tv_next_step_title);
        assertEquals("—", tvNext.getText().toString());
    }
}