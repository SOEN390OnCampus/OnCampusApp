package com.example.oncampusapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import android.graphics.Color;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import android.widget.Toast;

import com.example.oncampusapp.navigation.Route;
import com.example.oncampusapp.navigation.RouteTravelMode;
import com.google.android.gms.maps.GoogleMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class RoutePickerControllerTest {

    @Mock MapsActivity mockActivity;
    @Mock RouteManager mockRouteManager;
    @Mock IndoorNavigationController mockIndoorNav;
    @Mock EventBannerManager mockBannerManager;
    @Mock GoogleMap mockMap;
    @Mock ArrayAdapter<String> mockAdapter;

    // Views injected via reflection to bypass setup()
    @Mock LinearLayout mockRoutePicker;
    @Mock AutoCompleteTextView mockStartText;
    @Mock AutoCompleteTextView mockEndText;

    private RoutePickerController controller;

    @Before
    public void setUp() throws Exception {
        controller = new RoutePickerController(
                mockActivity, mockRouteManager, mockIndoorNav, mockBannerManager);
        injectField("routePicker",          mockRoutePicker);
        injectField("startDestinationText", mockStartText);
        injectField("endDestinationText",   mockEndText);
    }

    private void injectField(String name, Object value) throws Exception {
        Field f = RoutePickerController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    private Object invokePrivateMethod(String methodName, Class<?>[] argTypes, Object... args) throws Exception {
        Method m;
        try {
            m = RoutePickerController.class.getDeclaredMethod(methodName, argTypes);
        } catch (NoSuchMethodException exactMiss) {
            m = findCompatibleMethod(methodName, args);
            if (m == null) {
                throw exactMiss;
            }
            args = expandWithDefaultArgs(m.getParameterTypes(), args);
        }
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    private void invokePrivateMethod(String methodName) throws Exception {
        Method m = RoutePickerController.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(controller);
    }

    private Method findCompatibleMethod(String methodName, Object... args) {
        for (Method candidate : RoutePickerController.class.getDeclaredMethods()) {
            if (!candidate.getName().equals(methodName)) {
                continue;
            }

            Class<?>[] paramTypes = candidate.getParameterTypes();
            if (paramTypes.length < args.length) {
                continue;
            }

            boolean compatible = true;
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    continue;
                }
                Class<?> expected = boxType(paramTypes[i]);
                if (!expected.isAssignableFrom(arg.getClass())) {
                    compatible = false;
                    break;
                }
            }

            if (compatible) {
                return candidate;
            }
        }
        return null;
    }

    private Object[] expandWithDefaultArgs(Class<?>[] paramTypes, Object[] args) {
        if (paramTypes.length == args.length) {
            return args;
        }

        Object[] expanded = Arrays.copyOf(args, paramTypes.length);
        for (int i = args.length; i < paramTypes.length; i++) {
            expanded[i] = defaultValueForType(paramTypes[i]);
        }
        return expanded;
    }

    private Class<?> boxType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == char.class) return Character.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        return Void.class;
    }

    private Object defaultValueForType(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }

    /**
     * Helper to safely mock getText().toString() since Android's
     * SpannableStringBuilder / Editable return null in local JVM tests.
     */
    private void stubText(AutoCompleteTextView view, String text) {
        Editable mockEditable = mock(Editable.class);
        when(mockEditable.toString()).thenReturn(text);
        when(view.getText()).thenReturn(mockEditable);
    }

    // ── Constructor & Setup ───────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(new RoutePickerController(
                mockActivity, mockRouteManager, mockIndoorNav, mockBannerManager));
    }

    @Test
    public void setMap_nonNull_doesNotThrow() {
        controller.setMap(mockMap);
        assertNotNull(controller);
    }

    @Test
    public void setMap_null_doesNotThrow() {
        controller.setMap(null);
        assertNotNull(controller);
    }

    @Test
    public void setSearchAdapter_setsAdapters() {
        controller.setSearchAdapter(mockAdapter);
        verify(mockStartText).setAdapter(mockAdapter);
        verify(mockEndText).setAdapter(mockAdapter);
    }

    // ── openWithStartAndDestination ───────────────────────────────────────────

    @Test
    public void openWithStartAndDestination_hidesBannerAndSearch_setsTextAndPreviews() {
        View mockBannerView = mock(View.class);
        View mockSearchBarView = mock(View.class);
        when(mockActivity.findViewById(R.id.included_banner)).thenReturn(mockBannerView);
        when(mockActivity.findViewById(R.id.search_bar_container)).thenReturn(mockSearchBarView);

        controller.openWithStartAndDestination("H-820", "EV");

        verify(mockBannerView).setVisibility(View.GONE);
        verify(mockStartText).setText("H-820");
        verify(mockEndText).setText("EV");
        verify(mockSearchBarView).setVisibility(View.GONE);
        verify(mockRoutePicker).setVisibility(View.VISIBLE);
        verify(mockBannerManager).setRoutePickerOpen(true);
        verify(mockRouteManager).initiateRoutePreview("H-820", "EV");
    }

    @Test
    public void openWithStartAndDestination_nullViews_doesNotCrash() {
        when(mockActivity.findViewById(R.id.included_banner)).thenReturn(null);
        when(mockActivity.findViewById(R.id.search_bar_container)).thenReturn(null);

        controller.openWithStartAndDestination("Hall", "Library");

        verify(mockStartText).setText("Hall");
        verify(mockEndText).setText("Library");
        verify(mockRoutePicker).setVisibility(View.VISIBLE);
        verify(mockBannerManager).setRoutePickerOpen(true);
        verify(mockRouteManager).initiateRoutePreview("Hall", "Library");
    }

    // ── tryFillFocusedField ───────────────────────────────────────────────────

    @Test
    public void tryFillFocusedField_nullRoutePicker_returnsFalse() throws Exception {
        injectField("routePicker", null);
        assertFalse(controller.tryFillFocusedField("H-820"));
    }

    @Test
    public void tryFillFocusedField_routePickerGone_returnsFalse() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.GONE);
        assertFalse(controller.tryFillFocusedField("H-820"));
    }

    @Test
    public void tryFillFocusedField_startFocused_returnsTrueAndSetsText() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(true);

        assertTrue(controller.tryFillFocusedField("Hall Building"));
        verify(mockStartText).setText("Hall Building");
        verify(mockStartText).dismissDropDown();
        verify(mockEndText, never()).setText(anyString());
    }

    @Test
    public void tryFillFocusedField_endFocused_returnsTrueAndSetsText() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(false);
        when(mockEndText.hasFocus()).thenReturn(true);

        assertTrue(controller.tryFillFocusedField("SP Building"));
        verify(mockEndText).setText("SP Building");
        verify(mockEndText).dismissDropDown();
        verify(mockStartText, never()).setText(anyString());
    }

    @Test
    public void tryFillFocusedField_neitherFocused_returnsFalse() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(false);
        when(mockEndText.hasFocus()).thenReturn(false);

        assertFalse(controller.tryFillFocusedField("H-820"));
        verify(mockStartText, never()).setText(anyString());
        verify(mockEndText,   never()).setText(anyString());
    }

    // ── toggleNavigationUI ────────────────────────────────────────────────────

    private void stubToggleViews(LinearLayout inputs, LinearLayout tabs, Button go,
                                 LinearLayout navActive, ConstraintLayout dir,
                                 ImageButton prev, ImageButton next,
                                 TextView textDir, FrameLayout close) {
        doReturn(inputs).when(mockActivity).findViewById(R.id.layout_inputs);
        doReturn(tabs).when(mockActivity).findViewById(R.id.layout_tabs);
        doReturn(go).when(mockActivity).findViewById(R.id.btn_go);
        doReturn(navActive).when(mockActivity).findViewById(R.id.layout_navigation_active);
        doReturn(dir).when(mockActivity).findViewById(R.id.dirLayout);
        doReturn(prev).when(mockActivity).findViewById(R.id.prevDirBtn);
        doReturn(next).when(mockActivity).findViewById(R.id.nextDirBtn);
        doReturn(textDir).when(mockActivity).findViewById(R.id.textDir);
        doReturn(close).when(mockActivity).findViewById(R.id.close_search);
    }

    @Test
    public void toggleNavigationUI_isNavigating_hidesInputsAndShowsNavActive() {
        LinearLayout inputs = mock(LinearLayout.class);
        LinearLayout tabs = mock(LinearLayout.class);
        Button go = mock(Button.class);
        LinearLayout navActive = mock(LinearLayout.class);
        ConstraintLayout dir = mock(ConstraintLayout.class);
        ImageButton prev = mock(ImageButton.class);
        ImageButton next = mock(ImageButton.class);
        TextView textDir = mock(TextView.class);
        FrameLayout close = mock(FrameLayout.class);

        stubToggleViews(inputs, tabs, go, navActive, dir, prev, next, textDir, close);
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);

        controller.toggleNavigationUI(true);

        verify(inputs).setVisibility(View.GONE);
        verify(tabs).setVisibility(View.GONE);
        verify(go).setVisibility(View.GONE);
        verify(navActive).setVisibility(View.VISIBLE);
        verify(close).setVisibility(View.GONE);
        verify(dir).setVisibility(View.VISIBLE);
        verify(prev).setVisibility(View.VISIBLE);
        verify(next).setVisibility(View.VISIBLE);
        verify(textDir).setVisibility(View.VISIBLE);
    }

    @Test
    public void toggleNavigationUI_notNavigating_showsInputsAndHidesNavActive() {
        LinearLayout inputs = mock(LinearLayout.class);
        LinearLayout tabs = mock(LinearLayout.class);
        Button go = mock(Button.class);
        LinearLayout navActive = mock(LinearLayout.class);
        ConstraintLayout dir = mock(ConstraintLayout.class);
        ImageButton prev = mock(ImageButton.class);
        ImageButton next = mock(ImageButton.class);
        TextView textDir = mock(TextView.class);
        FrameLayout close = mock(FrameLayout.class);

        stubToggleViews(inputs, tabs, go, navActive, dir, prev, next, textDir, close);

        controller.toggleNavigationUI(false);

        verify(inputs).setVisibility(View.VISIBLE);
        verify(tabs).setVisibility(View.VISIBLE);
        verify(go).setVisibility(View.VISIBLE);
        verify(navActive).setVisibility(View.GONE);
        verify(close).setVisibility(View.VISIBLE);
        verify(dir).setVisibility(View.GONE);
        verify(prev).setVisibility(View.GONE);
        verify(next).setVisibility(View.GONE);
        verify(textDir).setVisibility(View.GONE);
    }

    @Test
    public void toggleNavigationUI_shuttleMode_doesNotShowDirectionViews() {
        LinearLayout inputs = mock(LinearLayout.class);
        LinearLayout tabs = mock(LinearLayout.class);
        Button go = mock(Button.class);
        LinearLayout navActive = mock(LinearLayout.class);
        ConstraintLayout dir = mock(ConstraintLayout.class);
        ImageButton prev = mock(ImageButton.class);
        ImageButton next = mock(ImageButton.class);
        TextView textDir = mock(TextView.class);
        FrameLayout close = mock(FrameLayout.class);

        stubToggleViews(inputs, tabs, go, navActive, dir, prev, next, textDir, close);
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.SHUTTLE);

        controller.toggleNavigationUI(true);

        verify(dir, never()).setVisibility(View.VISIBLE);
        verify(prev, never()).setVisibility(View.VISIBLE);
        verify(next, never()).setVisibility(View.VISIBLE);
        verify(textDir, never()).setVisibility(View.VISIBLE);
    }

    // ── Private Helpers (Reflection Tests) ────────────────────────────────────

    @Test
    public void handleEndTrip_poiNavigationActive_reloadsPoiExit() throws Exception {
        when(mockActivity.isPoiNavigationActive()).thenReturn(true);

        invokePrivateMethod("handleEndTrip");

        verify(mockActivity).reloadForPoiExit();
        verify(mockRouteManager, never()).stopNavigation();
    }

    @Test
    public void handleEndTrip_poiNavigationNotActive_stopsNavigationAndTryLaunch() throws Exception {
        when(mockActivity.isPoiNavigationActive()).thenReturn(false);
        controller.setMap(null); // By-pass map animation block to prevent static factory exceptions

        // toggleNavigationUI(false) is called inside handleEndTrip. Needs mock views.
        stubToggleViews(
                mock(LinearLayout.class), mock(LinearLayout.class), mock(Button.class),
                mock(LinearLayout.class), mock(ConstraintLayout.class), mock(ImageButton.class),
                mock(ImageButton.class), mock(TextView.class), mock(FrameLayout.class)
        );

        invokePrivateMethod("handleEndTrip");

        verify(mockRouteManager).stopNavigation();
        verify(mockActivity).tryLaunchPendingFinalIndoorRoute();
    }

    @Test
    public void swapAddresses_swapsTextAndClearsFocus() throws Exception {
        stubText(mockStartText, "StartLocation");
        stubText(mockEndText, "EndLocation");

        invokePrivateMethod("swapAddresses");

        verify(mockStartText).setText("EndLocation");
        verify(mockEndText).setText("StartLocation");
        verify(mockStartText).clearFocus();
        verify(mockEndText).clearFocus();
    }

    @Test
    public void previewRoute_callsInitiateRoutePreview() throws Exception {
        stubText(mockStartText, "Point A");
        stubText(mockEndText, "Point B");

        invokePrivateMethod("previewRoute");

        verify(mockRouteManager).initiateRoutePreview("Point A", "Point B");
    }

    @Test
    public void setCurrentBuilding_setsStartDestinationText() throws Exception {
        BuildingManager mockBM = mock(BuildingManager.class);
        Building mockBuilding = mock(Building.class);
        mockActivity.buildingManager = mockBM;
        when(mockBM.getCurrentBuilding()).thenReturn(mockBuilding);
        when(mockBuilding.getName()).thenReturn("Hall Building");

        invokePrivateMethod("setCurrentBuilding");

        verify(mockStartText).setText("Hall Building");
    }

    @Test
    public void setCurrentBuilding_nullBuilding_doesNothing() throws Exception {
        BuildingManager mockBM = mock(BuildingManager.class);
        mockActivity.buildingManager = mockBM;
        when(mockBM.getCurrentBuilding()).thenReturn(null);

        invokePrivateMethod("setCurrentBuilding");

        verify(mockStartText, never()).setText(anyString());
    }


    @Test
    public void tryLaunchIndoorRoute_bothRoomsFound_launchesIndoorRoute() throws Exception {
        Map<String, IndoorNode> localRoomMap = new HashMap<>();
        IndoorNode node1 = mock(IndoorNode.class);
        IndoorNode node2 = mock(IndoorNode.class);
        localRoomMap.put("H-820", node1);
        localRoomMap.put("EV", node2);

        // Notice the lenient() added here!
        lenient().when(mockIndoorNav.getIndoorRoomMap()).thenReturn(localRoomMap);

        boolean result = (boolean) invokePrivateMethod("tryLaunchIndoorRoute",
                new Class<?>[]{String.class, String.class}, "H-820", "EV");

        assertTrue(result);
        verify(mockIndoorNav).launchIndoorRoute(node1, node2);
    }

    @Test
    public void tryLaunchIndoorRoute_neitherRoomFound_returnsFalse() throws Exception {
        // Renamed from mockMap to localRoomMap
        Map<String, IndoorNode> localRoomMap = new HashMap<>();

        // Updated to use the new variable name
        when(mockIndoorNav.getIndoorRoomMap()).thenReturn(localRoomMap);

        boolean result = (boolean) invokePrivateMethod("tryLaunchIndoorRoute",
                new Class<?>[]{String.class, String.class}, "H-820", "EV");

        assertFalse(result);
        verify(mockIndoorNav, never()).launchIndoorRoute(any(), any());
    }

    @Test
    public void handleGoClick_indoorRoute_returnsEarly() throws Exception {
        stubText(mockStartText, "H-820");
        stubText(mockEndText, "EV");

        Map<String, IndoorNode> mockRoomMap = new HashMap<>();
        IndoorNode node1 = mock(IndoorNode.class);
        IndoorNode node2 = mock(IndoorNode.class);
        mockRoomMap.put("H-820", node1);
        mockRoomMap.put("EV", node2);
        when(mockIndoorNav.getIndoorRoomMap()).thenReturn(mockRoomMap);

        invokePrivateMethod("handleGoClick");

        // Should return early and launch indoor route without hitting the rest of the flow
        verify(mockIndoorNav).launchIndoorRoute(node1, node2);
        verify(mockRouteManager, never()).initiateRoutePreview(anyString(), anyString());
    }

    @Test
    public void selectNonShuttleMode_setsModeAndHidesShuttleButton() throws Exception {
        stubText(mockStartText, "");
        stubText(mockEndText, "");
        Button mockShuttleBtn = mock(Button.class);

        invokePrivateMethod("selectNonShuttleMode",
                new Class<?>[]{RouteTravelMode.class, Button.class}, RouteTravelMode.WALK, mockShuttleBtn);

        verify(mockRouteManager).setSelectedMode(RouteTravelMode.WALK);
        verify(mockShuttleBtn).setVisibility(View.GONE);
        verify(mockRouteManager).adjustGoButtonWidth(any(), any(Boolean.class));
        verify(mockRouteManager).clearShuttleRoute();
    }

    @Test
    public void selectShuttleMode_setsModeAndShowsShuttleButton() throws Exception {
        stubText(mockStartText, "");
        stubText(mockEndText, "");
        Button mockShuttleBtn = mock(Button.class);

        invokePrivateMethod("selectShuttleMode", new Class<?>[]{Button.class}, mockShuttleBtn);

        verify(mockRouteManager).setSelectedMode(RouteTravelMode.SHUTTLE);
        verify(mockShuttleBtn).setVisibility(View.VISIBLE);
        verify(mockRouteManager).adjustGoButtonWidth(any(), any(Boolean.class));
        verify(mockRouteManager).clearNormalRoute();
    }

    @Test
    public void highlightTab_setsColorsProperly() throws Exception {
        View selected = mock(View.class);
        View other1 = mock(View.class);
        View other2 = mock(View.class);
        List<View> tabs = Arrays.asList(selected, other1, other2);

        invokePrivateMethod("highlightTab", new Class<?>[]{View.class, List.class}, selected, tabs);

        verify(selected).setBackgroundColor(Color.parseColor("#D3D3D3"));
        verify(selected).setAlpha(1.0f);
        verify(other1).setBackgroundResource(0);
        verify(other1).setAlpha(0.5f);
        verify(other2).setBackgroundResource(0);
        verify(other2).setAlpha(0.5f);
    }

    // ── handleGoClick — empty field guards ────────────────────────────────────

    @Test
    public void handleGoClick_emptyStart_doesNotProceed() throws Exception {
        stubText(mockStartText, "");
        stubText(mockEndText, "Library");

        try (MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            invokePrivateMethod("handleGoClick");
        }

        verify(mockIndoorNav, never()).getIndoorRoomMap();
        verify(mockRouteManager, never()).initiateRoutePreview(anyString(), anyString());
    }

    @Test
    public void handleGoClick_emptyDest_doesNotProceed() throws Exception {
        stubText(mockStartText, "Hall");
        stubText(mockEndText, "");

        try (MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            invokePrivateMethod("handleGoClick");
        }

        verify(mockIndoorNav, never()).getIndoorRoomMap();
        verify(mockRouteManager, never()).initiateRoutePreview(anyString(), anyString());
    }

    // ── tryLaunchIndoorRoute — one room found ─────────────────────────────────

    @Test
    public void tryLaunchIndoorRoute_onlyStartRoomFound_returnsTrueWithoutLaunch() throws Exception {
        Map<String, IndoorNode> roomMap = new HashMap<>();
        roomMap.put("H-820", mock(IndoorNode.class));
        when(mockIndoorNav.getIndoorRoomMap()).thenReturn(roomMap);

        boolean result;
        try (MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            result = (boolean) invokePrivateMethod("tryLaunchIndoorRoute",
                    new Class<?>[]{String.class, String.class}, "H-820", "EV");
        }

        assertTrue(result);
        verify(mockIndoorNav, never()).launchIndoorRoute(any(), any());
    }

    @Test
    public void tryLaunchIndoorRoute_onlyDestRoomFound_returnsTrueWithoutLaunch() throws Exception {
        Map<String, IndoorNode> roomMap = new HashMap<>();
        roomMap.put("EV", mock(IndoorNode.class));
        when(mockIndoorNav.getIndoorRoomMap()).thenReturn(roomMap);

        boolean result;
        try (MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            result = (boolean) invokePrivateMethod("tryLaunchIndoorRoute",
                    new Class<?>[]{String.class, String.class}, "H-820", "EV");
        }

        assertTrue(result);
        verify(mockIndoorNav, never()).launchIndoorRoute(any(), any());
    }

    // ── handleGoClick — no preview active ────────────────────────────────────

    @Test
    public void handleGoClick_noPreviewActive_callsInitiateRoutePreview() throws Exception {
        stubText(mockStartText, "Hall");
        stubText(mockEndText, "Library");

        when(mockIndoorNav.getIndoorRoomMap()).thenReturn(new HashMap<>());
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);
        when(mockRouteManager.isPreviewActive()).thenReturn(false);

        try (MockedStatic<BuildingLookup> mocked = mockStatic(BuildingLookup.class);
             MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            mocked.when(() -> BuildingLookup.getLatLngFromBuildingName(anyString(), any()))
                  .thenReturn(null);
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            invokePrivateMethod("handleGoClick");
        }

        verify(mockRouteManager).initiateRoutePreview("Hall", "Library");
    }

    // ── handleGoClick → startNavigation ──────────────────────────────────────

    @Test
    public void handleGoClick_previewActive_callsStartNavigation() throws Exception {
        stubText(mockStartText, "Hall");
        stubText(mockEndText, "Library");
        TextView mockInstruction = mock(TextView.class);
        injectField("txtNavInstruction", mockInstruction);

        when(mockIndoorNav.getIndoorRoomMap()).thenReturn(new HashMap<>());
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);
        when(mockRouteManager.isPreviewActive()).thenReturn(true);
        when(mockRouteManager.getFirstRoutePoint()).thenReturn(null);

        stubToggleViews(
                mock(LinearLayout.class), mock(LinearLayout.class), mock(Button.class),
                mock(LinearLayout.class), mock(ConstraintLayout.class), mock(ImageButton.class),
                mock(ImageButton.class), mock(TextView.class), mock(FrameLayout.class)
        );

        try (MockedStatic<BuildingLookup> mocked = mockStatic(BuildingLookup.class);
             MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            mocked.when(() -> BuildingLookup.getLatLngFromBuildingName(anyString(), any()))
                  .thenReturn(null);
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            invokePrivateMethod("handleGoClick");
        }

        verify(mockRouteManager).removeStartDot();
        verify(mockRouteManager).startNavigationUpdates();
    }

    // ── startNavigation directly ──────────────────────────────────────────────

    @Test
    public void startNavigation_nullDuration_setsFollowRouteText() throws Exception {
        TextView mockInstruction = mock(TextView.class);
        injectField("txtNavInstruction", mockInstruction);

        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);
        when(mockRouteManager.getFirstRoutePoint()).thenReturn(null);

        stubToggleViews(
                mock(LinearLayout.class), mock(LinearLayout.class), mock(Button.class),
                mock(LinearLayout.class), mock(ConstraintLayout.class), mock(ImageButton.class),
                mock(ImageButton.class), mock(TextView.class), mock(FrameLayout.class)
        );

        try (MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            invokePrivateMethod("startNavigation",
                    new Class<?>[]{String.class, String.class}, "Hall", "Library");
        }

        verify(mockRouteManager).removeStartDot();
        verify(mockRouteManager).startNavigationUpdates();
        verify(mockInstruction).setText(anyString());
    }

    @Test
    public void startNavigation_withDuration_setsDurationText() throws Exception {
        TextView mockInstruction = mock(TextView.class);
        TextView mockDuration    = mock(TextView.class);
        injectField("txtNavInstruction", mockInstruction);
        injectField("txtDuration",       mockDuration);

        when(mockDuration.getText()).thenReturn("15 mins");
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);
        when(mockRouteManager.getFirstRoutePoint()).thenReturn(null);

        stubToggleViews(
                mock(LinearLayout.class), mock(LinearLayout.class), mock(Button.class),
                mock(LinearLayout.class), mock(ConstraintLayout.class), mock(ImageButton.class),
                mock(ImageButton.class), mock(TextView.class), mock(FrameLayout.class)
        );

        try (MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            invokePrivateMethod("startNavigation",
                    new Class<?>[]{String.class, String.class}, "Hall", "Library");
        }

        verify(mockInstruction).setText(anyString());
    }

    // ── applyPoiRoute ─────────────────────────────────────────────────────────

    @Test
    public void applyPoiRoute_withNullName_skipsOpenWithStartAndDestination() throws Exception {
        stubToggleViews(
                mock(LinearLayout.class), mock(LinearLayout.class), mock(Button.class),
                mock(LinearLayout.class), mock(ConstraintLayout.class), mock(ImageButton.class),
                mock(ImageButton.class), mock(TextView.class), mock(FrameLayout.class)
        );
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);

        Route mockRoute = mock(Route.class);
        when(mockRoute.getPoints()).thenReturn(new ArrayList<>());
        when(mockRoute.getDuration()).thenReturn("10 mins");
        when(mockRoute.getSteps()).thenReturn(new ArrayList<>());

        Method method = RoutePickerController.class.getDeclaredMethod(
                "applyPoiRoute", Route.class, String.class);
        method.setAccessible(true);

        try (MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            method.invoke(controller, mockRoute, null);
        }

        verify(mockRouteManager).drawRouteOnMap(any(), anyString(), any());
        verify(mockRouteManager).startNavigationUpdates();
        verify(mockRouteManager, never()).initiateRoutePreview(anyString(), anyString());
    }

    @Test
    public void applyPoiRoute_withName_setsNavInstruction() throws Exception {
        TextView mockInstruction = mock(TextView.class);
        TextView mockDuration    = mock(TextView.class);
        injectField("txtNavInstruction", mockInstruction);
        injectField("txtDuration",       mockDuration);

        when(mockDuration.getText()).thenReturn("");
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);
        when(mockActivity.findViewById(R.id.included_banner)).thenReturn(null);
        when(mockActivity.findViewById(R.id.search_bar_container)).thenReturn(null);

        stubToggleViews(
                mock(LinearLayout.class), mock(LinearLayout.class), mock(Button.class),
                mock(LinearLayout.class), mock(ConstraintLayout.class), mock(ImageButton.class),
                mock(ImageButton.class), mock(TextView.class), mock(FrameLayout.class)
        );

        Route mockRoute = mock(Route.class);
        when(mockRoute.getPoints()).thenReturn(new ArrayList<>());
        when(mockRoute.getDuration()).thenReturn("5 mins");
        when(mockRoute.getSteps()).thenReturn(new ArrayList<>());

        Method method = RoutePickerController.class.getDeclaredMethod(
                "applyPoiRoute", Route.class, String.class);
        method.setAccessible(true);

        try (MockedStatic<Toast> toastMock = mockStatic(Toast.class)) {
            toastMock.when(() -> Toast.makeText(any(), any(CharSequence.class), anyInt()))
                     .thenReturn(mock(Toast.class));
            method.invoke(controller, mockRoute, "Cafe Van Houtte");
        }

        verify(mockRouteManager).drawRouteOnMap(any(), anyString(), any());
        verify(mockRouteManager).startNavigationUpdates();
        verify(mockInstruction).setText(anyString());
    }
}