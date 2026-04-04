package com.example.oncampusapp;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class IndoorDirectionsActivityTest {

    private IndoorDirectionsActivity activity;
    private AutoCompleteTextView etFrom;
    private AutoCompleteTextView etTo;
    private TextView tvStatus;

    @Before
    public void setUp() {
        // Build, create, and resume the Activity
        activity = Robolectric.buildActivity(IndoorDirectionsActivity.class)
                .create()
                .resume()
                .get();

        // Bind the UI elements for easy access in tests
        etFrom = activity.findViewById(R.id.et_from_room);
        etTo = activity.findViewById(R.id.et_to_room);
        tvStatus = activity.findViewById(R.id.tv_status);

        // Advance the looper to process any immediate runOnUiThread calls from onCreate
        ShadowLooper.idleMainLooper();
    }

    @Test
    public void testActivity_isNotNull() {
        assertNotNull("Activity should be successfully created", activity);
    }

    @Test
    public void testBackButton_finishesActivity() {
        View backButton = activity.findViewById(R.id.btn_back);
        backButton.performClick();

        assertTrue("Activity should be finishing after back button click", activity.isFinishing());
    }

    @Test
    public void testSwapButton_swapsFromAndToText() {
        View swapButton = activity.findViewById(R.id.btn_swap);

        // Setup: Enter different text into the fields
        etFrom.setText("H-820");
        etTo.setText("MB-1.210");

        // Action: Click the swap button
        swapButton.performClick();

        // Assertion: Verify the texts have been successfully swapped
        assertEquals("MB-1.210", etFrom.getText().toString());
        assertEquals("H-820", etTo.getText().toString());
    }

    // --- Validation Tests (Synchronous) ---

    @Test
    public void testFindRoute_withEmptyInputs_showsErrorMessage() {
        View findRouteButton = activity.findViewById(R.id.btn_find_route);

        // Setup: Leave inputs empty
        etFrom.setText("");
        etTo.setText("");

        // Action: Click find route
        findRouteButton.performClick();

        // Assertion: Validation should fail synchronously and update the status text
        assertEquals(View.VISIBLE, tvStatus.getVisibility());
        assertEquals("Please enter both a start room and a destination room.", tvStatus.getText().toString());
    }

    @Test
    public void testFindRoute_withOneEmptyInput_showsErrorMessage() {
        View findRouteButton = activity.findViewById(R.id.btn_find_route);

        // Setup: Leave one input empty
        etFrom.setText("H-820");
        etTo.setText("");

        // Action
        findRouteButton.performClick();

        // Assertion
        assertEquals(View.VISIBLE, tvStatus.getVisibility());
        assertEquals("Please enter both a start room and a destination room.", tvStatus.getText().toString());
    }

    @Test
    public void testFindRoute_withNonExistentRooms_showsNotFoundError() {
        View findRouteButton = activity.findViewById(R.id.btn_find_route);

        // Setup: Enter completely fake rooms that definitely aren't in your JSONs
        String fakeRoom = "FAKE-999";
        etFrom.setText(fakeRoom);
        etTo.setText("H-820");

        // Action
        findRouteButton.performClick();

        // Assertion: Verify it catches the fake room
        assertEquals(View.VISIBLE, tvStatus.getVisibility());
        assertEquals("Room \"" + fakeRoom + "\" not found. Check the room number.", tvStatus.getText().toString());
    }

    // --- Fragment Dialog Tests ---

    @Test
    public void testBuildingCardH_opensBuildingBrowserDialog() {
        View cardH = activity.findViewById(R.id.card_building_h);

        // Action: Click the building card
        cardH.performClick();

        // FIX: Force the FragmentManager to execute the dialog transaction instantly!
        activity.getSupportFragmentManager().executePendingTransactions();

        // Assertion: Check if the SupportFragmentManager is currently hosting our DialogFragment
        Fragment dialog = activity.getSupportFragmentManager().findFragmentByTag("BuildingFloorSelectDialog");

        assertNotNull("Building browser dialog should be instantiated", dialog);
        assertTrue("Dialog should be an instance of BuildingFloorSelectDialog",
                dialog instanceof BuildingFloorSelectDialog);
    }

    @Test
    public void testBuildingCardMB_opensBuildingBrowserDialog() {
        View cardMB = activity.findViewById(R.id.card_building_mb);

        // Action
        cardMB.performClick();

        // FIX: Force the transaction to execute instantly
        activity.getSupportFragmentManager().executePendingTransactions();

        // Assertion
        Fragment dialog = activity.getSupportFragmentManager().findFragmentByTag("BuildingFloorSelectDialog");
        assertNotNull("Building browser dialog should be instantiated for MB", dialog);
    }
}