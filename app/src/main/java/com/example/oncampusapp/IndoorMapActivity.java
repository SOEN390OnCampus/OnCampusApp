package com.example.oncampusapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class IndoorMapActivity extends AppCompatActivity {

    private List<IndoorNode> currentFloorNodes = new ArrayList<>();
    private IndoorMapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_map);

        String buildingId = getIntent().getStringExtra("BUILDING_ID");
        String floorId = getIntent().getStringExtra("FLOOR_ID");

        if (buildingId == null || floorId == null) {
            Toast.makeText(this, "Error loading floor plan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tv_floor_title);
        tvTitle.setText(buildingId + " FLOOR " + floorId);

        findViewById(R.id.btn_back_indoor).setOnClickListener(v -> finish());

        mapView = findViewById(R.id.indoor_map_view);

        String imageName = (buildingId + "_" + floorId).toLowerCase();
        int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());

        String jsonName = buildingId.toLowerCase();
        int jsonResId = getResources().getIdentifier(jsonName, "raw", getPackageName());

        Bitmap floorPlan = null;
        if (imageResId != 0) {
            floorPlan = BitmapFactory.decodeResource(getResources(), imageResId);
        } else {
            Toast.makeText(this, "Floor plan image not found", Toast.LENGTH_SHORT).show();
        }

        mapView.setMapData(floorPlan);

        // Search Logic
        if (jsonResId != 0) {
            currentFloorNodes = loadNodesForFloor(jsonResId, floorId);
            setupSearchBar();
        }
    }

    private void setupSearchBar() {
        AutoCompleteTextView searchBar = findViewById(R.id.et_indoor_search);

        // Get all room names on this floor
        List<String> roomNames = new ArrayList<>();
        for (IndoorNode node : currentFloorNodes) {
            if (node.getLabel() != null && !node.getLabel().trim().isEmpty()) {
                roomNames.add(node.getLabel());
            }
        }

        // Feed names to the search bar
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roomNames);
        searchBar.setAdapter(adapter);

        // Drop the pin when a room is selected
        searchBar.setOnItemClickListener((parent, view, position, id) -> {
            String selectedRoom = (String) parent.getItemAtPosition(position);

            for (IndoorNode node : currentFloorNodes) {
                if (selectedRoom.equals(node.label)) {
                    // Send coordinates to the map
                    mapView.setPinLocation(node.getX(), node.getY());

                    // Hide the keyboard automatically
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                    searchBar.clearFocus();
                    break;
                }
            }
        });
    }

    private List<IndoorNode> loadNodesForFloor(int resourceId, String targetFloor) {
        List<IndoorNode> nodeList = new ArrayList<>();
        try {
            InputStream is = getResources().openRawResource(resourceId);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            JSONObject root = new JSONObject(json);
            JSONArray nodesArray = root.getJSONArray("nodes");

            for (int i = 0; i < nodesArray.length(); i++) {
                JSONObject obj = nodesArray.getJSONObject(i);

                String nodeFloor = obj.getString("floor");
                String nodeBuildingId = obj.optString("buildingId", ""); // Grab this to check for MB-S2
                String label = obj.optString("label", "").trim();

                // Standard match
                boolean isFloorMatch = nodeFloor.equalsIgnoreCase(targetFloor);

                // Catch the specific MB-S2 from JSON generator!
                if (targetFloor.equalsIgnoreCase("S2") && nodeBuildingId.toUpperCase().contains("S2")) {
                    isFloorMatch = true;
                }

                // ONLY save the node if it matches our rules AND actually has a searchable name
                if (isFloorMatch && !label.isEmpty()) {
                    IndoorNode node = new IndoorNode(
                            label,
                            (float) obj.getDouble("x"),
                            (float) obj.getDouble("y")
                    );

                    nodeList.add(node);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nodeList;
    }
}