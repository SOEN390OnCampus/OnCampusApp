package com.example.oncampusapp;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BuildingDialogManagerTest {

    private MapsActivity activity;
    private BuildingDialogManager manager;

    @Before
    public void setUp() {
        // Build the activity without triggering its complex lifecycle
        activity = Robolectric.buildActivity(MapsActivity.class).get();

        // Apply a theme so the Dialog can be instantiated without crashing
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);

        manager = new BuildingDialogManager(activity);
    }

    // ── Constructor & initial state ───────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(manager);
    }

    @Test
    public void getCurrentBuildingDialog_initiallyNull() {
        assertNull(manager.getCurrentBuildingDialog());
    }

    @Test
    public void getGeoIdToBuildingDetailsMap_initiallyNull() {
        assertNull(manager.getGeoIdToBuildingDetailsMap());
    }

    // ── JSON Loading ──────────────────────────────────────────────────────────

    @Test
    public void testLoadBuildingDetails_populatesMapFromRawResource() {
        // Action: Load the real JSON file from res/raw/concordia_building_details.json
        manager.loadBuildingDetails();

        // Assertion: The map should be initialized and contain data
        Map<String, BuildingDetails> map = manager.getGeoIdToBuildingDetailsMap();
        assertNotNull("Map should be initialized after loading", map);
        assertFalse("Map should not be empty (assuming the JSON file has content)", map.isEmpty());
    }

    // ── Dialog Creation & Population ──────────────────────────────────────────

    @Test
    public void testShowBuildingInfoDialog_createsAndPopulatesDialog() {
        // Setup: Mock the BuildingDetails so we don't need to know its exact constructor
        BuildingDetails mockDetails = Mockito.mock(BuildingDetails.class);
        when(mockDetails.getCode()).thenReturn("MB");
        when(mockDetails.getName()).thenReturn("John Molson School of Business");
        when(mockDetails.getAddress()).thenReturn("1450 Guy St");
        when(mockDetails.isAccessible()).thenReturn(true);
        when(mockDetails.hasDirectTunnelToMetro()).thenReturn(false);
        when(mockDetails.getSchedule()).thenReturn(null);
        when(mockDetails.getImage()).thenReturn("");

        // Action: Show the dialog
        manager.showBuildingInfoDialog(mockDetails);

        // Assertion 1: Verify the dialog state
        Dialog dialog = manager.getCurrentBuildingDialog();
        assertNotNull("Dialog should be tracked in the manager", dialog);
        assertTrue("Dialog should be visible to the user", dialog.isShowing());

        // Assertion 2: Verify the UI elements were populated correctly
        TextView txtCode = dialog.findViewById(R.id.txt_building_code);
        TextView txtName = dialog.findViewById(R.id.txt_building_name);
        TextView txtAddress = dialog.findViewById(R.id.txt_building_address);
        View llAccessibility = dialog.findViewById(R.id.item_accessibility);
        View llMetro = dialog.findViewById(R.id.item_metro_connect);
        View llOpeningHours = dialog.findViewById(R.id.layout_building_opening_hours);

        assertEquals("MB", txtCode.getText().toString());
        assertEquals("John Molson School of Business", txtName.getText().toString());
        assertEquals("1450 Guy St", txtAddress.getText().toString());

        assertEquals("Accessibility should be visible", View.VISIBLE, llAccessibility.getVisibility());
        assertEquals("Metro should be hidden", View.GONE, llMetro.getVisibility());
        assertEquals("Opening hours should be hidden when null", View.GONE, llOpeningHours.getVisibility());
    }

    @Test
    public void testDialogCloseButton_dismissesDialogAndClearsReference() {
        // Setup: Show a basic dialog
        BuildingDetails mockDetails = Mockito.mock(BuildingDetails.class);
        when(mockDetails.getCode()).thenReturn("H");
        when(mockDetails.getName()).thenReturn("Hall Building");
        manager.showBuildingInfoDialog(mockDetails);

        Dialog dialog = manager.getCurrentBuildingDialog();

        // Action: Click the close button (ImageButton)
        dialog.findViewById(R.id.btn_close).performClick();

        // Assertion: Verify it cleaned itself up
        assertFalse("Dialog should no longer be showing", dialog.isShowing());
        assertNull("Manager should clear its reference to the dialog", manager.getCurrentBuildingDialog());
    }

    @Test
    public void testShowBuildingInfoDialog_dismissesPreviousDialogIfAlreadyShowing() {
        // Setup: Show a dialog
        BuildingDetails mockDetails1 = Mockito.mock(BuildingDetails.class);
        manager.showBuildingInfoDialog(mockDetails1);
        Dialog firstDialog = manager.getCurrentBuildingDialog();

        // Action: Show a SECOND dialog before closing the first one
        BuildingDetails mockDetails2 = Mockito.mock(BuildingDetails.class);
        manager.showBuildingInfoDialog(mockDetails2);
        Dialog secondDialog = manager.getCurrentBuildingDialog();

        // Assertion: The first one should have been dismissed automatically
        assertFalse("First dialog should be dismissed", firstDialog.isShowing());
        assertTrue("Second dialog should be showing", secondDialog.isShowing());
        assertEquals("Manager should track the new dialog", secondDialog, manager.getCurrentBuildingDialog());
    }
}