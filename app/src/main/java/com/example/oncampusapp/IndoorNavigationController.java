package com.example.oncampusapp;

import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IndoorNavigationController {

    private final MapsActivity activity;
    private final Map<String, IndoorNode> indoorRoomMap;
    private final Map<String, IndoorNode> allIndoorNodesById = new HashMap<>();
    private ArrayAdapter<String> searchSuggestionsAdapter;

    public IndoorNavigationController(MapsActivity activity, Map<String, IndoorNode> indoorRoomMap) {
        this.activity = activity;
        this.indoorRoomMap = indoorRoomMap;
    }

    public void setSearchSuggestionsAdapter(ArrayAdapter<String> adapter) {
        this.searchSuggestionsAdapter = adapter;
    }

    public Map<String, IndoorNode> getIndoorRoomMap() {
        return indoorRoomMap;
    }

    public void loadIndoorRoomsIntoAdapter() {
        new Thread(() -> {
            String[] buildingIds = {"H", "MB", "LB", "CC", "VE", "VL"};
            List<String> newLabels = new ArrayList<>();
            for (String bid : buildingIds) {
                processBuilding(bid, newLabels);
            }
            activity.runOnUiThread(() -> {
                if (activity.isDestroyed() || activity.isFinishing()) return;
                if (searchSuggestionsAdapter != null && !newLabels.isEmpty()) {
                    searchSuggestionsAdapter.addAll(newLabels);
                    searchSuggestionsAdapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private void processBuilding(String buildingId, List<String> newLabels) {
        int resId = activity.getResources().getIdentifier(
                buildingId.toLowerCase(), "raw", activity.getPackageName());
        if (resId == 0) return;
        try (InputStream is = activity.getResources().openRawResource(resId);
             java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(is, "UTF-8"))) {
            String json = reader.lines().collect(Collectors.joining());
            JSONArray nodes = new JSONObject(json).getJSONArray("nodes");
            for (int i = 0; i < nodes.length(); i++) {
                processNode(nodes.getJSONObject(i), newLabels);
            }
        } catch (IOException | JSONException e) {
            Log.e("IndoorNavController", "Failed to load indoor rooms for " + buildingId, e);
        }
    }

    private void processNode(JSONObject obj, List<String> newLabels) {
        String id    = obj.optString("id", "").trim();
        String label = obj.optString("label", "").trim();

        IndoorNode node = new IndoorNode.Builder()
                .id(id).label(label)
                .type(obj.optString("type", ""))
                .buildingId(obj.optString("buildingId", ""))
                .floor(obj.optString("floor", ""))
                .x((float) obj.optDouble("x", 0.0))
                .y((float) obj.optDouble("y", 0.0))
                .accessible(obj.optBoolean("accessible", true))
                .build();

        if (!id.isEmpty()) {
            allIndoorNodesById.put(id, node);
        }

        if (label.isEmpty() || indoorRoomMap.containsKey(label)) return;
        indoorRoomMap.put(label, node);
        newLabels.add(label);
    }

    public void launchIndoorRoute(IndoorNode fromRoom, IndoorNode toRoom) {
        String fromBuilding = fromRoom.getRootBuildingId();
        String toBuilding   = toRoom.getRootBuildingId();

        if (!fromBuilding.equalsIgnoreCase(toBuilding)) {
            IndoorNode fromDoor = findPreferredDoorway(fromBuilding);
            IndoorNode toDoor   = findPreferredDoorway(toBuilding);

            if (fromDoor == null || toDoor == null) {
                Toast.makeText(activity,
                        "Cross-building indoor navigation is not supported for these buildings.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            loadIndoorPath(fromBuilding, fromRoom.getId(), fromDoor.getId(),
                    path -> {
                        if (activity.isDestroyed() || activity.isFinishing()) return;

                        Intent intent = new Intent(activity, IndoorMapActivity.class);
                        intent.putExtra("BUILDING_ID",          fromBuilding);
                        intent.putExtra("FLOOR_ID",             fromRoom.getFloorMenuId());
                        intent.putExtra("FROM_NODE_ID",         fromRoom.getId());
                        intent.putExtra("TO_NODE_ID",           fromDoor.getId());
                        intent.putExtra("PATH_NODE_IDS",        String.join(",", path));
                        intent.putExtra("CROSS_BUILDING_STAGE", "FIRST_INDOOR");
                        intent.putExtra("DISPLAY_DEST_LABEL",   fromDoor.getLabel());

                        activity.setPendingCrossBuilding(toDoor, toRoom, fromBuilding, toBuilding);
                        activity.startActivity(intent);
                    },
                    "Error loading source building graph.");
            return;
        }

        loadIndoorPath(fromBuilding, fromRoom.getId(), toRoom.getId(),
                path -> {
                    if (activity.isDestroyed() || activity.isFinishing()) return;
                    if (path.isEmpty()) {
                        Toast.makeText(activity, "No indoor path found between these rooms.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent intent = new Intent(activity, IndoorMapActivity.class);
                    intent.putExtra("BUILDING_ID",   fromBuilding);
                    intent.putExtra("FLOOR_ID",      fromRoom.getFloorMenuId());
                    intent.putExtra("FROM_NODE_ID",  fromRoom.getId());
                    intent.putExtra("TO_NODE_ID",    toRoom.getId());
                    intent.putExtra("PATH_NODE_IDS", String.join(",", path));
                    activity.startActivity(intent);
                },
                "Error loading building data.");
    }

    public void loadIndoorPath(String buildingId, String fromNodeId, String toNodeId,
                               java.util.function.Consumer<List<String>> onSuccess,
                               String errorMessage) {
        int resId = activity.getResources().getIdentifier(
                buildingId.toLowerCase(), "raw", activity.getPackageName());
        if (resId == 0) {
            Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            IndoorGraph graph = new IndoorGraph();
            try (InputStream is = activity.getResources().openRawResource(resId)) {
                graph.load(is);
                List<String> path = graph.shortestPath(fromNodeId, toNodeId);
                activity.runOnUiThread(() -> onSuccess.accept(path));
            } catch (Exception e) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private List<IndoorNode> getDoorwayNodesForBuilding(String buildingId) {
        List<IndoorNode> result = new ArrayList<>();
        for (IndoorNode node : allIndoorNodesById.values()) {
            if (node == null) continue;
            if (!buildingId.equalsIgnoreCase(node.getRootBuildingId())) continue;
            if (node.getId() != null && node.getId().toLowerCase().contains("building_entry_exit")) {
                result.add(node);
            }
        }
        return result;
    }

    private IndoorNode findPreferredDoorway(String buildingId) {
        List<IndoorNode> doorways = getDoorwayNodesForBuilding(buildingId);
        IndoorNode secondFloorAlternative = null;
        for (IndoorNode doorway : doorways) {
            String floor = doorway.getFloor();
            if ("1".equals(floor)) return doorway;
            if ("2".equals(floor) && secondFloorAlternative == null) secondFloorAlternative = doorway;
        }
        if (secondFloorAlternative != null) return secondFloorAlternative;
        return doorways.isEmpty() ? null : doorways.get(0);
    }
}
