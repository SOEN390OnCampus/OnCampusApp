package com.example.oncampusapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.oncampusapp.navigation.RouteTravelMode;
import com.google.android.gms.maps.GoogleMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

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

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(new RoutePickerController(
                mockActivity, mockRouteManager, mockIndoorNav, mockBannerManager));
    }

    // ── setMap ────────────────────────────────────────────────────────────────

    @Test
    public void setMap_nonNull_doesNotThrow() {
        controller.setMap(mockMap);
    }

    @Test
    public void setMap_null_doesNotThrow() {
        controller.setMap(null);
    }

    // ── setSearchAdapter ──────────────────────────────────────────────────────

    @Test
    public void setSearchAdapter_setsAdapterOnStartField() {
        controller.setSearchAdapter(mockAdapter);
        verify(mockStartText).setAdapter(mockAdapter);
    }

    @Test
    public void setSearchAdapter_setsAdapterOnEndField() {
        controller.setSearchAdapter(mockAdapter);
        verify(mockEndText).setAdapter(mockAdapter);
    }

    @Test
    public void setSearchAdapter_null_doesNotThrow() {
        controller.setSearchAdapter(null);
    }

    // ── tryFillFocusedField – routePicker state ───────────────────────────────

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
    public void tryFillFocusedField_routePickerInvisible_returnsFalse() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.INVISIBLE);
        assertFalse(controller.tryFillFocusedField("H-820"));
    }

    // ── tryFillFocusedField – start field focused ─────────────────────────────

    @Test
    public void tryFillFocusedField_startFocused_returnsTrue() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(true);
        assertTrue(controller.tryFillFocusedField("H-820"));
    }

    @Test
    public void tryFillFocusedField_startFocused_setsText() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(true);
        controller.tryFillFocusedField("Hall Building");
        verify(mockStartText).setText("Hall Building");
    }

    @Test
    public void tryFillFocusedField_startFocused_dismissesDropDown() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(true);
        controller.tryFillFocusedField("EV");
        verify(mockStartText).dismissDropDown();
    }

    @Test
    public void tryFillFocusedField_startFocused_doesNotTouchEndField() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(true);
        controller.tryFillFocusedField("EV");
        verify(mockEndText, never()).setText(anyString());
    }

    // ── tryFillFocusedField – end field focused ───────────────────────────────

    @Test
    public void tryFillFocusedField_endFocused_returnsTrue() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(false);
        when(mockEndText.hasFocus()).thenReturn(true);
        assertTrue(controller.tryFillFocusedField("SP"));
    }

    @Test
    public void tryFillFocusedField_endFocused_setsText() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(false);
        when(mockEndText.hasFocus()).thenReturn(true);
        controller.tryFillFocusedField("SP Building");
        verify(mockEndText).setText("SP Building");
    }

    @Test
    public void tryFillFocusedField_endFocused_dismissesDropDown() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(false);
        when(mockEndText.hasFocus()).thenReturn(true);
        controller.tryFillFocusedField("SP Building");
        verify(mockEndText).dismissDropDown();
    }

    @Test
    public void tryFillFocusedField_endFocused_doesNotTouchStartField() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(false);
        when(mockEndText.hasFocus()).thenReturn(true);
        controller.tryFillFocusedField("SP Building");
        verify(mockStartText, never()).setText(anyString());
    }

    // ── tryFillFocusedField – neither field focused ───────────────────────────

    @Test
    public void tryFillFocusedField_neitherFocused_returnsFalse() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(false);
        when(mockEndText.hasFocus()).thenReturn(false);
        assertFalse(controller.tryFillFocusedField("H-820"));
    }

    @Test
    public void tryFillFocusedField_neitherFocused_doesNotFillAnyField() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(false);
        when(mockEndText.hasFocus()).thenReturn(false);
        controller.tryFillFocusedField("H-820");
        verify(mockStartText, never()).setText(anyString());
        verify(mockEndText,   never()).setText(anyString());
    }

    // ── tryFillFocusedField – edge cases ─────────────────────────────────────

    @Test
    public void tryFillFocusedField_emptyName_fillsStartIfFocused() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(true);
        assertTrue(controller.tryFillFocusedField(""));
        verify(mockStartText).setText("");
    }

    @Test
    public void tryFillFocusedField_nullName_fillsStartIfFocused() {
        when(mockRoutePicker.getVisibility()).thenReturn(View.VISIBLE);
        when(mockStartText.hasFocus()).thenReturn(true);
        assertTrue(controller.tryFillFocusedField(null));
        verify(mockStartText).setText((String) null);
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
        LinearLayout inputs    = mock(LinearLayout.class);
        LinearLayout tabs      = mock(LinearLayout.class);
        Button go              = mock(Button.class);
        LinearLayout navActive = mock(LinearLayout.class);
        ConstraintLayout dir   = mock(ConstraintLayout.class);
        ImageButton prev       = mock(ImageButton.class);
        ImageButton next       = mock(ImageButton.class);
        TextView textDir       = mock(TextView.class);
        FrameLayout close      = mock(FrameLayout.class);

        stubToggleViews(inputs, tabs, go, navActive, dir, prev, next, textDir, close);
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);

        controller.toggleNavigationUI(true);

        verify(inputs).setVisibility(View.GONE);
        verify(tabs).setVisibility(View.GONE);
        verify(go).setVisibility(View.GONE);
        verify(navActive).setVisibility(View.VISIBLE);
        verify(close).setVisibility(View.GONE);
    }

    @Test
    public void toggleNavigationUI_isNavigating_showsDirectionViewsForWalk() {
        LinearLayout inputs    = mock(LinearLayout.class);
        LinearLayout tabs      = mock(LinearLayout.class);
        Button go              = mock(Button.class);
        LinearLayout navActive = mock(LinearLayout.class);
        ConstraintLayout dir   = mock(ConstraintLayout.class);
        ImageButton prev       = mock(ImageButton.class);
        ImageButton next       = mock(ImageButton.class);
        TextView textDir       = mock(TextView.class);
        FrameLayout close      = mock(FrameLayout.class);

        stubToggleViews(inputs, tabs, go, navActive, dir, prev, next, textDir, close);
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.WALK);

        controller.toggleNavigationUI(true);

        verify(dir).setVisibility(View.VISIBLE);
        verify(prev).setVisibility(View.VISIBLE);
        verify(next).setVisibility(View.VISIBLE);
        verify(textDir).setVisibility(View.VISIBLE);
    }

    @Test
    public void toggleNavigationUI_notNavigating_showsInputsAndHidesNavActive() {
        LinearLayout inputs    = mock(LinearLayout.class);
        LinearLayout tabs      = mock(LinearLayout.class);
        Button go              = mock(Button.class);
        LinearLayout navActive = mock(LinearLayout.class);
        ConstraintLayout dir   = mock(ConstraintLayout.class);
        ImageButton prev       = mock(ImageButton.class);
        ImageButton next       = mock(ImageButton.class);
        TextView textDir       = mock(TextView.class);
        FrameLayout close      = mock(FrameLayout.class);

        stubToggleViews(inputs, tabs, go, navActive, dir, prev, next, textDir, close);

        controller.toggleNavigationUI(false);

        verify(inputs).setVisibility(View.VISIBLE);
        verify(tabs).setVisibility(View.VISIBLE);
        verify(go).setVisibility(View.VISIBLE);
        verify(navActive).setVisibility(View.GONE);
        verify(close).setVisibility(View.VISIBLE);
        verify(dir).setVisibility(View.GONE);
    }

    @Test
    public void toggleNavigationUI_shuttleMode_doesNotShowDirectionViews() {
        LinearLayout inputs    = mock(LinearLayout.class);
        LinearLayout tabs      = mock(LinearLayout.class);
        Button go              = mock(Button.class);
        LinearLayout navActive = mock(LinearLayout.class);
        ConstraintLayout dir   = mock(ConstraintLayout.class);
        ImageButton prev       = mock(ImageButton.class);
        ImageButton next       = mock(ImageButton.class);
        TextView textDir       = mock(TextView.class);
        FrameLayout close      = mock(FrameLayout.class);

        stubToggleViews(inputs, tabs, go, navActive, dir, prev, next, textDir, close);
        when(mockRouteManager.getSelectedMode()).thenReturn(RouteTravelMode.SHUTTLE);

        controller.toggleNavigationUI(true);

        // Direction views should NOT become visible in shuttle mode
        verify(dir,     never()).setVisibility(View.VISIBLE);
        verify(prev,    never()).setVisibility(View.VISIBLE);
        verify(next,    never()).setVisibility(View.VISIBLE);
        verify(textDir, never()).setVisibility(View.VISIBLE);
    }
}
