package com.example.oncampusapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NavigationHelper {

    public interface DirectionsCallback {
        void onSuccess(List<LatLng> path, String durationText);
        void onError(Exception e);
    }
    public enum Mode {
        WALKING("walking"),
        DRIVING("driving"),
        TRANSIT("transit");
        private final String value;
        Mode(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    /**
     * Fetches directions from Google API and parses the JSON.
     */
    public static void fetchDirections(LatLng start, LatLng end, Mode mode, String apiKey, DirectionsCallback callback) {
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + start.latitude + "," + start.longitude +
                "&destination=" + end.latitude + "," + end.longitude +
                "&mode=" + mode.getValue() +
                "&key=" + apiKey;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String response = fetchUrl(urlString);
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray routes = jsonResponse.optJSONArray("routes");

                if (routes != null && routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);

                    // Decode Path
                    String encodedString = route.getJSONObject("overview_polyline").getString("points");
                    List<LatLng> decodedPath = PolyUtil.decode(encodedString);

                    // Get Duration
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    String durationText = leg.getJSONObject("duration").getString("text");

                    callback.onSuccess(decodedPath, durationText);
                } else {
                    callback.onError(new Exception("No routes found"));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    static String fetchUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        InputStream inputStream = conn.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }
    /**
     * Slices the route to start exactly at the user's current location.
     * Returns the updated path, or the original path if the user is off-route.
     */
    public static List<LatLng> getUpdatedPath(LatLng userLocation, List<LatLng> currentRoute, double toleranceMeters) {
        if (currentRoute == null || currentRoute.isEmpty()) return currentRoute;

        int index = PolyUtil.locationIndexOnPath(userLocation, currentRoute, true, toleranceMeters);

        if (index >= 0 && index < currentRoute.size() - 1) {
            List<LatLng> newPath = new ArrayList<>();
            newPath.add(userLocation);
            List<LatLng> remainingPoints = currentRoute.subList(index + 1, currentRoute.size());
            newPath.addAll(remainingPoints);
            return newPath;
        }

        return currentRoute; // Return original if user is off-path
    }

    /**
     * Checks if the user is within a certain distance of the final destination.
     */
    public static boolean hasArrived(LatLng userLocation, List<LatLng> currentRoute, double arrivalThresholdMeters) {
        if (currentRoute == null || currentRoute.isEmpty()) return false;

        LatLng destination = currentRoute.get(currentRoute.size() - 1);
        double distanceToFinish = SphericalUtil.computeDistanceBetween(userLocation, destination);

        return distanceToFinish <= arrivalThresholdMeters;
    }
}