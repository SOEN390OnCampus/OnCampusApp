package com.example.oncampusapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Initial indoor-navigation screen.
 *
 * The user types room numbers (e.g. "H-867", "MB-1.294") in the From / To
 * fields.  Autocomplete is populated from every room label in every building
 * JSON.  On "Find Route" the app runs Dijkstra inside the matching building
 * and launches IndoorMapActivity with the computed path.
 */
public class IndoorDirectionsActivity extends AppCompatActivity {

    // ── Room data loaded from all building JSONs ──────────────────────────────

    /** label → full IndoorNode (holds id, buildingId, floorMenuId, …) */
    private final Map<String, IndoorNode> roomLabelToNode = new LinkedHashMap<>();

    /** All room labels for the autocomplete adapter */
    private final List<String> allRoomLabels = new ArrayList<>();

    // Map of root building ID → raw resource ID
    private static final String[] BUILDING_IDS  = {"H",    "MB",   "LB",   "CC",   "VE",   "VL"};

    // ── UI references ─────────────────────────────────────────────────────────

    private AutoCompleteTextView etFrom;
    private AutoCompleteTextView etTo;
    private TextView tvStatus;
    private ArrayAdapter<String> autocompleteAdapter;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_directions);

        etFrom = findViewById(R.id.et_from_room);
        etTo   = findViewById(R.id.et_to_room);
        tvStatus = findViewById(R.id.tv_status);

        autocompleteAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, allRoomLabels);
        etFrom.setAdapter(autocompleteAdapter);
        etTo.setAdapter(autocompleteAdapter);

        // Load room data off the main thread

        new Thread(() -> {
            loadAllRooms();
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                tvStatus.setText("Rooms loaded");
            });
        }).start();

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Swap button
        findViewById(R.id.btn_swap).setOnClickListener(v -> {
            String tmp = etFrom.getText().toString();
            etFrom.setText(etTo.getText().toString());
            etTo.setText(tmp);
            // Move cursor to end
            etFrom.setSelection(etFrom.getText().length());
            etTo.setSelection(etTo.getText().length());
        });

        // Find Route button
        findViewById(R.id.btn_find_route).setOnClickListener(v -> onFindRouteClicked());

        // Building cards → open the building browser (reuse existing dialog)
        View.OnClickListener openBrowser = v -> {
            BuildingFloorSelectDialog dialog = new BuildingFloorSelectDialog();
            dialog.show(getSupportFragmentManager(), "BuildingFloorSelectDialog");
        };
        findViewById(R.id.card_building_h).setOnClickListener(openBrowser);
        findViewById(R.id.card_building_mb).setOnClickListener(openBrowser);
        findViewById(R.id.card_building_lb).setOnClickListener(openBrowser);
        findViewById(R.id.card_building_ve).setOnClickListener(openBrowser);
    }

    // ── Room loading ──────────────────────────────────────────────────────────

    private void loadAllRooms() {
        for (String buildingId : BUILDING_IDS) {
            int resId = getBuildingRawResId(buildingId);
            if (resId == 0) continue;
            try (InputStream is = getResources().openRawResource(resId)) {
                parseRoomsFromStream(is);
            } catch (IOException | JSONException e) {
                Log.e("LoadRooms", "Error loading rooms for building: " + buildingId, e);
            }
        }

        runOnUiThread(() -> {
            autocompleteAdapter.notifyDataSetChanged();
        });
    }

    private void parseRoomsFromStream(InputStream is) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        JSONObject root = new JSONObject(sb.toString());
        JSONArray nodesArray = root.getJSONArray("nodes");

        for (int i = 0; i < nodesArray.length(); i++) {
            JSONObject obj = nodesArray.getJSONObject(i);
            String label = obj.optString("label", "").trim();
            if (label.isEmpty()) continue;

            String id         = obj.getString("id");
            String type       = obj.optString("type", "");
            String buildingId = obj.optString("buildingId", "");
            String floor      = obj.optString("floor", "");
            float  x          = (float) obj.optDouble("x", 0.0);
            float  y          = (float) obj.optDouble("y", 0.0);
            boolean accessible = obj.optBoolean("accessible", true);

            IndoorNode node = new IndoorNode(id, label, type, buildingId, floor, x, y, accessible);
            if (!roomLabelToNode.containsKey(label)) {
                roomLabelToNode.put(label, node);
                allRoomLabels.add(label);
            }
        }
    }

    // ── Route finding ─────────────────────────────────────────────────────────

    private void onFindRouteClicked() {
        hideKeyboard();
        tvStatus.setVisibility(View.GONE);

        String fromLabel = etFrom.getText().toString().trim();
        String toLabel   = etTo.getText().toString().trim();

        if (fromLabel.isEmpty() || toLabel.isEmpty()) {
            showStatus("Please enter both a start room and a destination room.");
            return;
        }

        IndoorNode fromNode = roomLabelToNode.get(fromLabel);
        IndoorNode toNode   = roomLabelToNode.get(toLabel);

        if (fromNode == null) {
            showStatus("Room \"" + fromLabel + "\" not found. Check the room number.");
            return;
        }
        if (toNode == null) {
            showStatus("Room \"" + toLabel + "\" not found. Check the room number.");
            return;
        }

        String fromBuilding = fromNode.getRootBuildingId();
        String toBuilding   = toNode.getRootBuildingId();

        if (!fromBuilding.equalsIgnoreCase(toBuilding)) {
            showStatus("Cross-building navigation is not yet supported.\n"
                     + "Both rooms must be in the same building.");
            return;
        }

        // Run pathfinding in background
        String finalFromBuilding = fromBuilding;
        IndoorNode finalFromNode = fromNode;
        IndoorNode finalToNode   = toNode;

        new Thread(() -> {
            int resId = getBuildingRawResId(finalFromBuilding);
            if (resId == 0) {
                runOnUiThread(() -> showStatus("Building data not found."));
                return;
            }

            IndoorGraph graph = IndoorGraphCache.getGraph(this, finalFromBuilding);
            if(graph == null) {
                runOnUiThread(() -> showStatus("Error loading building map data."));
                return;
            }

            List<String> path = graph.shortestPath(finalFromNode.getId(), finalToNode.getId());

            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;

                if (path.isEmpty()) {
                    showStatus("No path found between these rooms.");
                    return;
                }
                launchIndoorMap(finalFromBuilding, finalFromNode, finalToNode, path);
            });
        }).start();
    }

    private void launchIndoorMap(String buildingId,
                                  IndoorNode fromNode,
                                  IndoorNode toNode,
                                  List<String> pathNodeIds) {
        // Determine the starting floor from the FROM node
        String startFloorId = fromNode.getFloorMenuId();

        // Pack path as comma-separated node IDs
        String pathString = String.join(",", pathNodeIds);

        Intent intent = new Intent(this, IndoorMapActivity.class);
        intent.putExtra("BUILDING_ID",    buildingId);
        intent.putExtra("FLOOR_ID",       startFloorId);
        intent.putExtra("FROM_NODE_ID",   fromNode.getId());
        intent.putExtra("TO_NODE_ID",     toNode.getId());
        intent.putExtra("PATH_NODE_IDS",  pathString);
        startActivity(intent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getBuildingRawResId(String buildingId) {
        return getResources().getIdentifier(
                buildingId.toLowerCase(), "raw", getPackageName());
    }

    private void showStatus(String message) {
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
