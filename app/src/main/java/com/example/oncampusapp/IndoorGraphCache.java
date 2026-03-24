package com.example.oncampusapp;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class IndoorGraphCache {
    private IndoorGraphCache() { }
    private static final Map<String, IndoorGraph> cache = new HashMap<>();

    public static synchronized IndoorGraph getGraph(Context context, String buildingId) {
        if (cache.containsKey(buildingId)) {
            return cache.get(buildingId);
        }

        int resId = context.getResources().getIdentifier(
                buildingId.toLowerCase(), "raw", context.getPackageName());

        if (resId == 0) return null;

        IndoorGraph graph = new IndoorGraph();

        try (InputStream is = context.getResources().openRawResource(resId)) {
            graph.load(is);
        } catch (IOException | JSONException e) {
            Log.e("GRAPH", "Error getting graph", e);
            return null;
        }

        cache.put(buildingId, graph);
        return graph;
    }
}
