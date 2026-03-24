package com.example.oncampusapp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IndoorMapActivity extends AppCompatActivity {

    private enum StepType {
        GO_STRAIGHT, TURN_LEFT, TURN_RIGHT,
        TAKE_ELEVATOR, TAKE_STAIRS, TAKE_ESCALATOR,
        ARRIVE
    }

    private static class NavigationStep {
        final StepType type;
        final String   title;
        final String   subtitle;
        final String   floorId;

        NavigationStep(StepType type, String title, String subtitle, String floorId) {
            this.type     = type;
            this.title    = title;
            this.subtitle = subtitle;
            this.floorId  = floorId;
        }
    }

    private List<IndoorNode> currentFloorNodes = new ArrayList<>();
    private IndoorMapView    mapView;

    private static final String TAG = "IndoorMapActivity";

    private static final String FLOOR = "Floor ";

    private String   buildingId;
    private String   fromNodeId;
    private String   toNodeId;
    private String[] pathNodeIds;

    private IndoorGraph graph;

    private Map<String, List<IndoorNode>> byFloor;
    private List<String>                  floorSequence = new ArrayList<>();

    private List<NavigationStep> allSteps         = new ArrayList<>();
    private int                  currentStepIndex = 0;
    private String               displayedFloorId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_map);

        buildingId        = getIntent().getStringExtra("BUILDING_ID");
        String floorId    = getIntent().getStringExtra("FLOOR_ID");
        fromNodeId        = getIntent().getStringExtra("FROM_NODE_ID");
        toNodeId          = getIntent().getStringExtra("TO_NODE_ID");
        String pathString = getIntent().getStringExtra("PATH_NODE_IDS");

        if (pathString != null && !pathString.isEmpty()) {
            pathNodeIds = pathString.split(",");
        }

        if (buildingId == null || floorId == null) {
            Toast.makeText(this, "Error loading floor plan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tv_floor_title);
        tvTitle.setText(buildingId + " FLOOR " + floorId);

        findViewById(R.id.btn_back_indoor).setOnClickListener(v -> finish());

        mapView = findViewById(R.id.indoor_map_view);
        loadFloorImage(floorId);
        displayedFloorId = floorId;

        setupBanner();

        int jsonResId = getResources().getIdentifier(
                buildingId.toLowerCase(), "raw", getPackageName());
        if (jsonResId != 0) {
            currentFloorNodes = loadNodesForFloor(jsonResId, floorId);
            setupSearchBar();
        }

        boolean isRouteMode = pathNodeIds != null && pathNodeIds.length >= 2;
        if (isRouteMode) {
            showRoutePanels();
            final int finalJsonResId = jsonResId;
            new Thread(() -> {
                graph = new IndoorGraph();
                try (InputStream is = getResources().openRawResource(finalJsonResId)) {
                    graph.load(is);
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Failed to load graph", e);
                    return;
                }

                buildFloorGroups();

                List<String> pathList = Arrays.asList(pathNodeIds);
                double totalPixels    = graph.pathDistance(pathList);
                int    transitSecs    = graph.pathTransitPenaltySeconds(pathList);
                int    minutes        = Math.max(1,
                        (int) Math.ceil((totalPixels * 0.05 / 1.4 + transitSecs) / 60));

                List<NavigationStep> steps = generateAllSteps();

                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;

                    updateRouteTime(minutes);
                    allSteps = steps;
                    displayStep(0);
                });
            }).start();
        }
    }

    private void setupBanner() {
        LinearLayout banner = findViewById(R.id.layout_floor_transition);
        if (banner != null) banner.setVisibility(View.GONE);
    }

    private void buildFloorGroups() {
        byFloor = new LinkedHashMap<>();
        List<String> floorOrder = new ArrayList<>();

        for (String raw : pathNodeIds) {
            IndoorNode node = graph.getNode(raw.trim());
            if (node == null) continue;
            String fid = node.getFloorMenuId();
            if (!byFloor.containsKey(fid)) {
                byFloor.put(fid, new ArrayList<>());
                floorOrder.add(fid);
            }
            byFloor.get(fid).add(node);
        }

        floorSequence = new ArrayList<>();
        for (String fid : floorOrder) {
            if (!isTransitOnly(byFloor.get(fid))) floorSequence.add(fid);
        }
    }

    private List<NavigationStep> generateAllSteps() {
        if (floorSequence.isEmpty()) return new ArrayList<>();

        List<NavigationStep> steps = new ArrayList<>();

        for (int f = 0; f < floorSequence.size(); f++) {
            String floorId = floorSequence.get(f);
            List<IndoorNode> floorNodes = byFloor.get(floorId);

            if (isInvalidFloor(floorNodes)) continue;

            addInitialStep(steps, floorId);
            processFloorNodes(steps, floorNodes, floorId);
            addTransitionStepIfNeeded(steps, f, floorId);
        }
        addFinalStep(steps);
        return filterSteps(steps);
    }

    private boolean isInvalidFloor(List<IndoorNode> nodes) {
        return nodes == null || nodes.isEmpty();
    }

    private void addInitialStep(List<NavigationStep> steps, String floorId) {


        steps.add(new NavigationStep(
                StepType.GO_STRAIGHT,
                "Go Straight",
                FLOOR + floorId,
                floorId
        ));
    }

    private void addFinalStep(List<NavigationStep> steps) {
        if (steps.isEmpty()) return;

        IndoorNode destNode  = graph.getNode(pathNodeIds[pathNodeIds.length - 1].trim());
        String lastFloor = floorSequence.get(floorSequence.size()-1);
        String destLabel = (destNode != null && destNode.getLabel() != null)
                ? destNode.getLabel() : "destination";

        steps.add(new NavigationStep(
                StepType.GO_STRAIGHT,
                "You have arrived!",
                "At " + destLabel,
                lastFloor
        ));
    }

    private void processFloorNodes(List<NavigationStep> steps,
                                   List<IndoorNode> nodes,
                                   String floorId) {

        for (int i = 1; i < nodes.size() - 1; i++) {
            IndoorNode prev = nodes.get(i - 1);
            IndoorNode cur  = nodes.get(i);
            IndoorNode next = nodes.get(i + 1);

            if (!isTurnCandidate(cur)) continue;

            StepType turn = computeTurnType(prev, cur, next);
            if (shouldSkipTurn(steps, turn)) continue;

            addTurnSteps(steps, turn, floorId);
        }
    }

    private boolean isTurnCandidate(IndoorNode node) {
        String type = node.getType();
        return type != null &&
                (type.contains("hallway") || type.contains("doorway"));
    }

    private boolean shouldSkipTurn(List<NavigationStep> steps, StepType turn) {
        if (turn == StepType.GO_STRAIGHT) return true;

        NavigationStep last = steps.get(steps.size() - 1);
        return last.type == turn;
    }

    private void addTurnSteps(List<NavigationStep> steps,
                              StepType turn,
                              String floorId) {

        String title = (turn == StepType.TURN_LEFT) ? "Turn Left" : "Turn Right";

        steps.add(new NavigationStep(turn, title, FLOOR + floorId, floorId));
        steps.add(new NavigationStep(StepType.GO_STRAIGHT,
                "Go Straight", FLOOR + floorId, floorId));
    }

    private void addTransitionStepIfNeeded(List<NavigationStep> steps,
                                           int index,
                                           String floorId) {

        if (index >= floorSequence.size() - 1) return;

        String nextFloor = floorSequence.get(index + 1);
        StepType type = findTransitionType(floorId);
        String title = transitionTitle(type, nextFloor);

        steps.add(new NavigationStep(type, title, FLOOR + floorId, floorId));
    }

    private List<NavigationStep> filterSteps(List<NavigationStep> steps) {
        List<NavigationStep> filtered = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            if (shouldFilterOut(steps, i)) continue;
            filtered.add(steps.get(i));
        }

        return filtered;
    }

    private boolean shouldFilterOut(List<NavigationStep> steps, int i) {
        NavigationStep current = steps.get(i);

        if (i == steps.size() - 1) return false;

        if (current.type != StepType.GO_STRAIGHT) return false;

        boolean beforeTransition =
                i > 0 && isTransition(steps.get(i - 1));

        boolean afterTransition =
                i < steps.size() - 1 && isTransition(steps.get(i + 1));

        return beforeTransition || afterTransition;
    }

    private boolean isTransition(NavigationStep step) {
        return step.type != StepType.GO_STRAIGHT &&
                step.type != StepType.TURN_LEFT &&
                step.type != StepType.TURN_RIGHT;
    }

    private StepType findTransitionType(String curFloorId) {
        boolean pastCurFloor = false;
        for (String raw : pathNodeIds) {
            IndoorNode node = graph.getNode(raw.trim());
            if (node == null) continue;
            if (!pastCurFloor) {
                pastCurFloor = isSameFloor(node, curFloorId);
                continue;
            }
            StepType type = getTransitionType(node);
            if (type != null) return type;
        }
        return StepType.TAKE_ELEVATOR;
    }

    private StepType getTransitionType(IndoorNode node) {
        String type = node.getType();
        if (type == null) return null;

        if (type.contains("escalator")) return StepType.TAKE_ESCALATOR;
        if (type.contains("stair"))     return StepType.TAKE_STAIRS;
        if (type.contains("elevator"))  return StepType.TAKE_ELEVATOR;

        return null;
    }

    private boolean isSameFloor(IndoorNode node, String floorId) {
        return floorId.equals(node.getFloorMenuId());
    }

    private String transitionTitle(StepType type, String nextFloor) {
        switch (type) {
            case TAKE_STAIRS:    return "Take Staircase to Floor " + nextFloor;
            case TAKE_ESCALATOR: return "Take Escalator to Floor " + nextFloor;
            default:             return "Take Elevator to Floor "  + nextFloor;
        }
    }

    /**
     * Cross-product turn detection in screen coordinates (y-down).
     * Fires only for turns ≥ ~44° with both segments ≥ 80 px.
     */
    private StepType computeTurnType(IndoorNode a, IndoorNode b, IndoorNode c) {

        float ax = b.getX() - a.getX();
        float ay = b.getY() - a.getY();
        float bx = c.getX() - b.getX();
        float by = c.getY() - b.getY();

        float lenA = (float) Math.sqrt(ax * ax + ay * ay);
        float lenB = (float) Math.sqrt(bx * bx + by * by);
        if (lenA < 80f || lenB < 80f) return StepType.GO_STRAIGHT;
        float sinAngle = (ax * by - ay * bx) / (lenA * lenB);
        if (sinAngle >  0.70f) return StepType.TURN_RIGHT;
        if (sinAngle < -0.70f) return StepType.TURN_LEFT;
        return StepType.GO_STRAIGHT;
    }

    private boolean isTransitOnly(List<IndoorNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return true;
        for (IndoorNode n : nodes) {
            String t = n.getType();
            if (t == null) return false;
            if (!t.contains("elevator") && !t.contains("stair") && !t.contains("escalator"))
                return false;
        }
        return true;
    }

    private void displayStep(int idx) {
        if (idx < 0 || idx >= allSteps.size()) return;
        currentStepIndex = idx;
        NavigationStep step = allSteps.get(idx);

        if (!step.floorId.equals(displayedFloorId)) {
            loadFloorImage(step.floorId);
            displayedFloorId = step.floorId;
            TextView tvTitle = findViewById(R.id.tv_floor_title);
            if (tvTitle != null) tvTitle.setText(buildingId + " FLOOR " + step.floorId);
        }

        drawPathForFloor(step.floorId);
        updateCurrentCard(step);
        updateNextCard(idx + 1 < allSteps.size() ? allSteps.get(idx + 1) : null);
        wireConfirmButton(step);
    }

    private void drawPathForFloor(String floorId) {
        if (byFloor == null) return;
        List<IndoorNode> nodes  = byFloor.get(floorId);
        List<PointF>     points = new ArrayList<>();
        IndoorNode       lastNode = null;
        if (nodes != null) {
            for (IndoorNode n : nodes) {
                points.add(new PointF(n.getX(), n.getY()));
                lastNode = n;
            }
        }
        mapView.setPath(points);
        if (lastNode != null) mapView.setPinLocation(lastNode.getX(), lastNode.getY());
    }

    private void updateCurrentCard(NavigationStep step) {
        TextView  tvTitle    = findViewById(R.id.tv_current_step_title);
        TextView  tvSubtitle = findViewById(R.id.tv_current_step_subtitle);
        ImageView ivIcon     = findViewById(R.id.iv_step_direction_icon);

        if (tvTitle    != null) tvTitle.setText(step.title);
        if (tvSubtitle != null) tvSubtitle.setText(step.subtitle);

        if (ivIcon != null) {
            switch (step.type) {
                case TURN_LEFT:
                    ivIcon.setImageResource(R.drawable.ic_arrow_next);
                    ivIcon.setRotation(180f);
                    break;
                case TURN_RIGHT:
                    ivIcon.setImageResource(R.drawable.ic_arrow_next);
                    ivIcon.setRotation(0f);
                    break;
                case TAKE_ELEVATOR: case TAKE_STAIRS: case TAKE_ESCALATOR:
                    ivIcon.setImageResource(R.drawable.ic_stairs);
                    ivIcon.setRotation(0f);
                    break;
                case ARRIVE:
                    ivIcon.setImageResource(R.drawable.ic_location_solid);
                    ivIcon.setRotation(0f);
                    break;
                default:
                    ivIcon.setImageResource(R.drawable.ic_arrow_next);
                    ivIcon.setRotation(-90f);
                    break;
            }
        }
    }

    private void updateNextCard(NavigationStep next) {
        TextView  tvNext = findViewById(R.id.tv_next_step_title);
        ImageView ivNext = findViewById(R.id.iv_next_step_icon);

        if (next == null) {
            if (tvNext != null) tvNext.setText("—");
            return;
        }
        if (tvNext != null) tvNext.setText(next.title);
        if (ivNext != null) {
            switch (next.type) {
                case TURN_LEFT:
                    ivNext.setImageResource(R.drawable.ic_arrow_next);
                    ivNext.setRotation(180f);
                    break;
                case TURN_RIGHT:
                    ivNext.setImageResource(R.drawable.ic_arrow_next);
                    ivNext.setRotation(0f);
                    break;
                case TAKE_ELEVATOR: case TAKE_STAIRS: case TAKE_ESCALATOR:
                    ivNext.setImageResource(R.drawable.ic_stairs);
                    ivNext.setRotation(0f);
                    break;
                case ARRIVE:
                    ivNext.setImageResource(R.drawable.ic_location_solid);
                    ivNext.setRotation(0f);
                    break;
                default:
                    ivNext.setImageResource(R.drawable.ic_arrow_next);
                    ivNext.setRotation(-90f);
                    break;
            }
        }
    }

    private void wireConfirmButton(NavigationStep step) {
        View btn = findViewById(R.id.btn_step_confirm);
        if (btn == null) return;

        int activeColor = ContextCompat.getColor(this, R.color.step_confirm_active);
        int inactiveColor = ContextCompat.getColor(this, R.color.step_confirm_inactive);

        if (step.type == StepType.ARRIVE) {
            btn.setBackgroundTintList(ColorStateList.valueOf(activeColor));;
            btn.setOnClickListener(null);
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));
            btn.setOnClickListener(v -> {
                v.setBackgroundTintList(ColorStateList.valueOf(activeColor));
                v.postDelayed(() -> {
                    v.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));
                    displayStep(currentStepIndex + 1);
                }, 400);
            });
        }
    }

    private void loadFloorImage(String floorId) {
        String imageName = (buildingId + "_" + floorId).toLowerCase();
        int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        if (resId != 0) mapView.setMapData(
                BitmapFactory.decodeResource(getResources(), resId));
    }

    private void showRoutePanels() {
        findViewById(R.id.et_indoor_search).setVisibility(View.GONE);
        findViewById(R.id.row_info_cards).setVisibility(View.VISIBLE);

        TextView tvTime = findViewById(R.id.tv_route_time);
        if (tvTime != null) tvTime.setText("...");

        CardView cardCurrent = findViewById(R.id.card_current_step);
        CardView cardNext    = findViewById(R.id.card_next_step);
        if (cardCurrent != null) cardCurrent.setVisibility(View.VISIBLE);
        if (cardNext    != null) cardNext.setVisibility(View.VISIBLE);
    }

    private void updateRouteTime(int minutes) {
        TextView tvTime = findViewById(R.id.tv_route_time);
        if (tvTime != null) tvTime.setText(minutes + " min");
    }

    private void setupSearchBar() {
        AutoCompleteTextView searchBar = findViewById(R.id.et_indoor_search);
        List<String> roomNames = new ArrayList<>();
        for (IndoorNode node : currentFloorNodes) {
            if (node.getLabel() != null && !node.getLabel().trim().isEmpty())
                roomNames.add(node.getLabel());
        }
        searchBar.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, roomNames));
        searchBar.setOnItemClickListener((parent, view, position, id) -> {
            String selectedRoom = (String) parent.getItemAtPosition(position);
            for (IndoorNode node : currentFloorNodes) {
                if (selectedRoom.equals(node.getLabel())) {
                    mapView.setPinLocation(node.getX(), node.getY());
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                    searchBar.clearFocus();
                    break;
                }
            }
        });
    }

    private List<IndoorNode> loadNodesForFloor(int resourceId, String targetFloor) {
        List<IndoorNode> nodeList = new ArrayList<>();
        try (InputStream is = getResources().openRawResource(resourceId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            JSONObject root       = new JSONObject(sb.toString());
            JSONArray  nodesArray = root.getJSONArray("nodes");

            for (int i = 0; i < nodesArray.length(); i++) {
                JSONObject obj            = nodesArray.getJSONObject(i);
                String     nodeFloor      = obj.optString("floor", "");
                String     nodeBuildingId = obj.optString("buildingId", "");
                String     label          = obj.optString("label", "").trim();

                boolean isFloorMatch = nodeFloor.equalsIgnoreCase(targetFloor);
                if (targetFloor.equalsIgnoreCase("S2")
                        && nodeBuildingId.toUpperCase().contains("S2")) {
                    isFloorMatch = true;
                }

                if (isFloorMatch && !label.isEmpty()) {
                    nodeList.add(new IndoorNode(
                            obj.optString("id", ""),
                            label,
                            obj.optString("type", ""),
                            nodeBuildingId,
                            nodeFloor,
                            (float) obj.optDouble("x", 0.0),
                            (float) obj.optDouble("y", 0.0),
                            obj.optBoolean("accessible", true)
                    ));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read node resource: " + resourceId, e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse node resource: " + resourceId, e);
        }
        return nodeList;
    }
}
