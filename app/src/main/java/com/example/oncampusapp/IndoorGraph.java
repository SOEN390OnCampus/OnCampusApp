package com.example.oncampusapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Weighted undirected graph of a building's indoor map.
 * Nodes and edges are loaded from the building's raw JSON resource.
 * Provides Dijkstra shortest-path between any two node IDs.
 */
public class IndoorGraph {

    public static class Edge {
        public final String  targetId;
        public final double  weight;
        public final String  type;
        public final boolean accessible;

        Edge(String targetId, double weight, String type, boolean accessible) {
            this.targetId   = targetId;
            this.weight     = weight;
            this.type       = type;
            this.accessible = accessible;
        }
    }

    private static final String GRAPH = "GRAPH";
    private final Map<String, IndoorNode> nodes = new HashMap<>();
    private final Map<String, List<Edge>> adj   = new HashMap<>();


    /**
     * Parses nodes and edges from a building JSON InputStream.
     * Edges are treated as undirected (both directions are added).
     */
    public void load(InputStream is) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        JSONObject root       = new JSONObject(sb.toString());
        JSONArray  nodesArray = root.getJSONArray("nodes");
        JSONArray  edgesArray = root.getJSONArray("edges");

        for (int i = 0; i < nodesArray.length(); i++) {
            JSONObject obj        = nodesArray.getJSONObject(i);
            String     id         = obj.getString("id");
            String     label      = obj.optString("label", "").trim();
            String     type       = obj.optString("type", "");
            String     buildingId = obj.optString("buildingId", "");
            String     floor      = obj.optString("floor", "");
            float      x          = (float) obj.optDouble("x", 0.0);
            float      y          = (float) obj.optDouble("y", 0.0);
            boolean    accessible = obj.optBoolean("accessible", true);

            nodes.put(id, new IndoorNode.Builder()
                    .id(id).label(label).type(type)
                    .buildingId(buildingId).floor(floor)
                    .x(x).y(y).accessible(accessible)
                    .build());
            adj.put(id, new ArrayList<>());
        }

        for (int i = 0; i < edgesArray.length(); i++) {
            JSONObject obj        = edgesArray.getJSONObject(i);
            String     source     = obj.getString("source");
            String     target     = obj.getString("target");
            double     weight     = obj.optDouble("weight", 1.0);
            String     type       = obj.optString("type", "");
            boolean    accessible = obj.optBoolean("accessible", true);

            addEdge(source, target, weight, type, accessible);
            addEdge(target, source, weight, type, accessible);
        }
    }

    private void addEdge(String from, String to, double weight, String type, boolean accessible) {
        List<Edge> list = adj.get(from);
        if (list != null) list.add(new Edge(to, weight, type, accessible));
    }

    /**
     * Returns the ordered list of node IDs on the shortest path from
     * {@code sourceId} to {@code targetId}, or an empty list if unreachable.
     * Room-type nodes are never used as intermediate hops.
     */
    public List<String> shortestPath(String sourceId, String targetId) {

        logNeighbors(sourceId);

        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) {
            return Collections.emptyList();
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        for (String node : nodes.keySet()) {
            dist.put(node, Double.MAX_VALUE);
        }

        dist.put(sourceId, 0.0);
        pq.add(sourceId);

        while (!pq.isEmpty()) {
            String current = pq.poll();

            for (Edge edge : adj.getOrDefault(current, Collections.emptyList())) {
                relaxEdge(current, edge, targetId, dist, prev, pq);
            }
        }

        return buildPath(sourceId, targetId, prev);
    }

    private void relaxEdge(String current,
                           Edge edge,
                           String targetId,
                           Map<String, Double> dist,
                           Map<String, String> prev,
                           PriorityQueue<String> pq) {

        double currentDist = dist.get(current);
        double penalty = getPenalty(edge, targetId);
        double newDist = currentDist + edge.weight + penalty;

        if (newDist < dist.getOrDefault(edge.targetId, Double.MAX_VALUE)) {
            dist.put(edge.targetId, newDist);
            prev.put(edge.targetId, current);
            pq.add(edge.targetId);
        }
    }

    /**
     * Fixed penalty logic:
     * - Penalize intermediate rooms.
     * - Penalize edges of type "room_to_door".
     */
    private double getPenalty(Edge edge, String finalTargetId) {
        IndoorNode node = nodes.get(edge.targetId);
        if (node == null) return 0;

        if (edge.targetId.equals(finalTargetId)) return 0;

        if ("room".equals(node.getType()) || "room_to_door".equals(edge.type) || ("hallway".equals(edge.type) && isInsideRoomCluster(edge))) return 10000;

        return 0;
    }

    /**
     * Detects hallway edges that are “inside a room cluster”
     */
    private boolean isInsideRoomCluster(Edge edge) {
        IndoorNode node = nodes.get(edge.targetId);
        if (node == null) return false;

        // A hallway node is "inside a room cluster" if all neighbors are rooms or doorways
        for (Edge neighborEdge : adj.getOrDefault(edge.targetId, Collections.emptyList())) {
            IndoorNode neighbor = nodes.get(neighborEdge.targetId);
            if (neighbor != null) {
                String type = neighbor.getType();
                if (!"room".equals(type) && !"doorway".equals(type)) {
                    return false; // found a real corridor
                }
            }
        }
        return true;
    }

    private List<String> buildPath(String sourceId,
                                   String targetId,
                                   Map<String, String> prev) {

        if (!prev.containsKey(targetId)) {
            Log.e(GRAPH, "Target NOT reached: " + targetId);
            Log.e(GRAPH, "Visited nodes: " + prev.keySet());
            return Collections.emptyList();
        }

        LinkedList<String> path = new LinkedList<>();
        String cursor = targetId;

        while (cursor != null) {
            path.addFirst(cursor);
            cursor = prev.get(cursor);
        }

        return sourceId.equals(path.getFirst()) ? path : Collections.emptyList();
    }

    private void logNeighbors(String sourceId) {
        Log.d(GRAPH, "Neighbors of " + sourceId + ":");
        for (Edge e : adj.getOrDefault(sourceId, Collections.emptyList())) {
            IndoorNode n = nodes.get(e.targetId);
            String type = (n != null) ? n.getType() : "UNKNOWN";
            Log.d(GRAPH, "→ " + e.targetId + " (" + type + ") [edge type=" + e.type + "]");
        }
    }

    public IndoorNode getNode(String id) {
        return nodes.get(id);
    }

    public Map<String, IndoorNode> getAllNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Sums the edge weights along an ordered path (as returned by shortestPath).
     * Returns 0 if the path has fewer than 2 nodes.
     */
    public double pathDistance(List<String> path) {
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to   = path.get(i + 1);
            for (Edge e : adj.getOrDefault(from, Collections.emptyList())) {
                if (e.targetId.equals(to)) { total += e.weight; break; }
            }
        }
        return total;
    }

    /**
     * Returns extra seconds to add for stair / escalator / elevator transitions.
     * Penalties: stair → 30 s, escalator → 20 s, elevator → 45 s.
     */
    public int pathTransitPenaltySeconds(List<String> path) {
        int penalty = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to   = path.get(i + 1);
            for (Edge e : adj.getOrDefault(from, Collections.emptyList())) {
                if (e.targetId.equals(to)) {
                    if (e.type != null) {
                        penalty += penaltyPerType(e.type);
                    }
                    break;
                }
            }
        }
        return penalty;
    }

    public int penaltyPerType(String type) {
        switch(type){
            case "stair":
                return 30;
            case "escalator":
                return 20;
            case "elevator":
                return 45;
            default:
                return 0;
        }
    }

    /** Returns all labeled rooms (nodes with non-empty labels). */
    public List<IndoorNode> getLabeledRooms() {
        List<IndoorNode> rooms = new ArrayList<>();
        for (IndoorNode node : nodes.values()) {
            if (node.getLabel() != null && !node.getLabel().isEmpty()) rooms.add(node);
        }
        return rooms;
    }


 /**
     * Computes an indoor route for reduced mobility users.
     * Stair edges are excluded so the returned path avoids stairs.
     */
    public List<String> shortestAccessiblePath(String sourceId, String targetId) {
        logNeighbors(sourceId);

        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) {
            return Collections.emptyList();
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        for (String node : nodes.keySet()) {
            dist.put(node, Double.MAX_VALUE);
        }

        dist.put(sourceId, 0.0);
        pq.add(sourceId);

        while (!pq.isEmpty()) {
            String current = pq.poll();

            for (Edge edge : adj.getOrDefault(current, Collections.emptyList())) {
                if (!isAccessibleEdge(current, edge, sourceId, targetId)) {
                    continue;
                }
                relaxEdge(current, edge, targetId, dist, prev, pq);
            }
        }

        return buildPath(sourceId, targetId, prev);
    }
    /**
     * Returns true only for edges that are allowed in reduced mobility mode.
     */
    private boolean isAccessibleEdge(String currentId, Edge edge, String sourceId, String targetId) {
        IndoorNode nextNode = nodes.get(edge.targetId);
        if (nextNode == null) return false;

        String edgeType = edge.type == null ? "" : edge.type.trim().toLowerCase();

        //avoid stairs
        if (edgeType.equals("stair") || edgeType.equals("stairs")) {
            return false;
        }

        if (!edge.accessible) {
            return false;
        }

        boolean isSourceOrTarget = edge.targetId.equals(sourceId) || edge.targetId.equals(targetId);
        if (!isSourceOrTarget && !nextNode.isAccessible()) {
            return false;
        }

        return true;
    }



    
}

