package com.example.oncampusapp;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33) // REQUIRED: Your code uses is.readAllBytes() which is API 33 (Tiramisu)+
public class BuildingFloorSelectDialogTest {

    private BuildingFloorSelectDialog dialog;
    private FragmentActivity activity;

    @Before
    public void setUp() {
        // Build a dummy FragmentActivity to host the DialogFragment
        activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        // Instantiate and show the dialog
        dialog = new BuildingFloorSelectDialog();
        dialog.show(activity.getSupportFragmentManager(), "BuildingFloorSelectDialog");

        // Execute pending fragment transactions so the dialog is immediately ready for testing
        activity.getSupportFragmentManager().executePendingTransactions();
    }

    @Test
    public void testDialog_isCreatedAndShowing() {
        assertNotNull("Dialog should not be null", dialog.getDialog());
        assertTrue("Dialog should be currently showing", dialog.getDialog().isShowing());
    }

    @Test
    public void testRecyclerView_isInitializedCorrectly() {
        // Find the RecyclerView inside the dialog's view
        RecyclerView recyclerView = dialog.getDialog().findViewById(R.id.rv_indoor_building);

        assertNotNull("RecyclerView should be present in the layout", recyclerView);

        // Verify the LayoutManager
        assertTrue("LayoutManager should be a LinearLayoutManager",
                recyclerView.getLayoutManager() instanceof LinearLayoutManager);

        // Verify the Adapter is attached
        // Note: Even if the real res/raw/floor_menu.json is empty or fails to parse
        // in the test environment, the adapter instance itself should still be created and set.
        assertNotNull("BuildingFloorAdapter should be attached to the RecyclerView",
                recyclerView.getAdapter());
        assertTrue("Adapter should be of type BuildingFloorAdapter",
                recyclerView.getAdapter() instanceof BuildingFloorAdapter);
    }

    @Test
    public void testOnStart_setsCorrectWindowWidthAndBackground() {
        // Action: The layout modifications happen in onStart(), which was automatically
        // triggered by Robolectric during our setUp() phase.

        // 1. Get the screen size of the simulated device
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int expectedWidth = (int) (size.x * 0.80);

        // 2. Verify the Window dimensions
        int actualWidth = dialog.getDialog().getWindow().getAttributes().width;
        assertEquals("Dialog width should be 80% of the screen width", expectedWidth, actualWidth);

        int actualHeightParam = dialog.getDialog().getWindow().getAttributes().height;
        // Initially, the height is set to WRAP_CONTENT before the view posts its actual height
        assertEquals("Initial height should be WRAP_CONTENT",
                ViewGroup.LayoutParams.WRAP_CONTENT, actualHeightParam);

        // 3. Verify the Transparent Background
        Drawable background = dialog.getDialog().getWindow().getDecorView().getBackground();
        assertTrue("Background should be a ColorDrawable", background instanceof ColorDrawable);
        assertEquals("Background color should be transparent",
                Color.TRANSPARENT, ((ColorDrawable) background).getColor());
    }

    @Test
    public void testOnStart_capsHeightAt50PercentIfContentIsTaller() {
        // Note: Because your height capping logic is inside a decorView.post(...) block,
        // it gets added to the end of the main thread's message queue.
        // We have to simulate the view having a massive height, and then advance the looper.

        View decorView = dialog.getDialog().getWindow().getDecorView();

        // Force the decor view to think it is extremely tall (e.g., 5000 pixels)
        decorView.setBottom(5000);

        // Execute the Runnable that was posted in onStart()
        ShadowLooper.runUiThreadTasks();

        // Calculate expected max height (50% of display height)
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int expectedMaxHeight = (int) (size.y * 0.50);

        // Verify the window height was capped
        int actualHeightParam = dialog.getDialog().getWindow().getAttributes().height;
        assertEquals("Dialog height should be capped at 50% of the screen height",
                expectedMaxHeight, actualHeightParam);
    }
}