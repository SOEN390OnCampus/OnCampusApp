package com.example.oncampusapp;

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

            nodes.put(id, new IndoorNode(id, label, type, buildingId, floor, x, y, accessible));
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
        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) {
            return Collections.emptyList();
        }
        if (sourceId.equals(targetId)) {
            return Collections.singletonList(sourceId);
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();

        for (String id : nodes.keySet()) dist.put(id, Double.MAX_VALUE);
        dist.put(sourceId, 0.0);

        PriorityQueue<String> pq = new PriorityQueue<>(
                Comparator.comparingDouble(id -> dist.getOrDefault(id, Double.MAX_VALUE)));
        pq.add(sourceId);

        while (!pq.isEmpty()) {
            String current     = pq.poll();
            if (current.equals(targetId)) break;

            double currentDist = dist.getOrDefault(current, Double.MAX_VALUE);
            if (currentDist == Double.MAX_VALUE) continue;

            for (Edge edge : adj.getOrDefault(current, Collections.emptyList())) {
                IndoorNode neighbor = nodes.get(edge.targetId);
                if (neighbor != null && "room".equals(neighbor.getType())
                        && !edge.targetId.equals(targetId)) {
                    continue;
                }
                double newDist = currentDist + edge.weight;
                if (newDist < dist.getOrDefault(edge.targetId, Double.MAX_VALUE)) {
                    dist.put(edge.targetId, newDist);
                    prev.put(edge.targetId, current);
                    pq.add(edge.targetId);
                }
            }
        }

        if (!prev.containsKey(targetId)) return Collections.emptyList();

        LinkedList<String> path = new LinkedList<>();
        String cursor = targetId;
        while (cursor != null) {
            path.addFirst(cursor);
            cursor = prev.get(cursor);
        }

        if (!sourceId.equals(path.getFirst())) return Collections.emptyList();
        return path;
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
                        if      (e.type.contains("stair"))     penalty += 30;
                        else if (e.type.contains("escalator")) penalty += 20;
                        else if (e.type.contains("elevator"))  penalty += 45;
                    }
                    break;
                }
            }
        }
        return penalty;
    }

    /** Returns all labeled rooms (nodes with non-empty labels). */
    public List<IndoorNode> getLabeledRooms() {
        List<IndoorNode> rooms = new ArrayList<>();
        for (IndoorNode node : nodes.values()) {
            if (node.getLabel() != null && !node.getLabel().isEmpty()) rooms.add(node);
        }
        return rooms;
    }
}
