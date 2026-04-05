package com.example.oncampusapp;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BuildingFloorAdapterTest {

    private Activity context;
    private BuildingFloorAdapter adapter;
    private List<BuildingFloorData> mockDataList;

    @Before
    public void setUp() {
        // Build a dummy activity to provide a proper Theme/Context for inflating Material Chips
        context = Robolectric.buildActivity(Activity.class).create().get();
        context.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);

        mockDataList = new ArrayList<>();

        // Mock BuildingFloorData 1
        BuildingFloorData mockBuilding1 = Mockito.mock(BuildingFloorData.class);
        when(mockBuilding1.getId()).thenReturn("H");
        when(mockBuilding1.getFullName()).thenReturn("Henry F. Hall Building");

        // Mock 2 Floors for Building 1
        List<BuildingFloorData.Floor> mockFloors = new ArrayList<>();
        BuildingFloorData.Floor mockFloor1 = Mockito.mock(BuildingFloorData.Floor.class);
        BuildingFloorData.Floor mockFloor2 = Mockito.mock(BuildingFloorData.Floor.class);
        when(mockFloor1.id()).thenReturn("8");
        when(mockFloor2.id()).thenReturn("9");
        mockFloors.add(mockFloor1);
        mockFloors.add(mockFloor2);

        when(mockBuilding1.getFloors()).thenReturn(mockFloors);
        mockDataList.add(mockBuilding1);

        adapter = new BuildingFloorAdapter(mockDataList);
    }

    @Test
    public void testGetItemCount_returnsCorrectSize() {
        assertEquals("Adapter should have exactly 1 item", 1, adapter.getItemCount());
    }

    @Test
    public void testOnBindViewHolder_defaultStateIsCollapsed() {
        // Setup: Create the ViewHolder manually
        ViewGroup parent = new LinearLayout(context);
        BuildingFloorAdapter.IndoorBuildingViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        // Action: Bind the data to the view
        adapter.onBindViewHolder(holder, 0);

        // Assertion: Check that data is mapped correctly
        assertEquals("H", holder.tvBuildingId.getText().toString());
        assertEquals("Henry F. Hall Building", holder.tvFullName.getText().toString());

        // Assertion: Verify it is visually collapsed
        assertEquals("Dropdown arrow should point down (0 degrees)", 0f, holder.ivDropDown.getRotation(), 0.0f);
        assertEquals("Floor container should be hidden", View.GONE, holder.floorContainer.getVisibility());
        assertEquals("Chip group should be empty", 0, holder.floorChipsGroup.getChildCount());
    }

    @Test
    public void testClickingItemRow_expandsContainerAndGeneratesChips() {
        // Setup
        ViewGroup parent = new LinearLayout(context);
        BuildingFloorAdapter.IndoorBuildingViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        // Action: Simulate user clicking the row to expand it
        holder.itemView.performClick();

        // Manually trigger the re-bind that the RecyclerView would normally do
        // after `notifyItemChanged` is called inside the click listener.
        adapter.onBindViewHolder(holder, 0);

        // Assertion: Verify it is now visually expanded
        assertEquals("Dropdown arrow should point up (180 degrees)", 180f, holder.ivDropDown.getRotation(), 0.0f);
        assertEquals("Floor container should be visible", View.VISIBLE, holder.floorContainer.getVisibility());

        // Assertion: Verify Chips were generated based on our mock data (2 floors)
        assertEquals("Should generate 2 floor chips", 2, holder.floorChipsGroup.getChildCount());

        Chip chip1 = (Chip) holder.floorChipsGroup.getChildAt(0);
        assertEquals("First chip should be floor 8", "8", chip1.getText().toString());
    }

    @Test
    public void testClickingFloorChip_startsIndoorMapActivity() {
        // Setup: Create the view holder
        ViewGroup parent = new LinearLayout(context);
        BuildingFloorAdapter.IndoorBuildingViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        // 1. MUST BIND INITIALLY so the click listener gets attached!
        adapter.onBindViewHolder(holder, 0);

        // 2. Now perform the click to expand it
        holder.itemView.performClick();

        // 3. Bind again to simulate the RecyclerView updating to the expanded state
        adapter.onBindViewHolder(holder, 0);

        // Action: Grab the first generated chip and click it
        Chip firstFloorChip = (Chip) holder.floorChipsGroup.getChildAt(0);
        firstFloorChip.performClick();

        // Assertion: Verify the correct Intent was fired
        ShadowActivity shadowActivity = Shadows.shadowOf(context);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Clicking a chip should start an Activity", actualIntent);
        assertEquals(IndoorMapActivity.class.getName(), actualIntent.getComponent().getClassName());

        // Verify the Intent extras contain the right Building and Floor IDs
        assertEquals("H", actualIntent.getStringExtra("BUILDING_ID"));
        assertEquals("8", actualIntent.getStringExtra("FLOOR_ID"));
    }
}