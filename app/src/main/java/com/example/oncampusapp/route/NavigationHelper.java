package com.example.oncampusapp.route;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NavigationHelper {
    /**
     * Fetches directions from Google API and parses the JSON.
     */
    public static void fetchRoute(LatLng start, LatLng end, RouteTravelMode mode, String apiKey, RoutesCallback callback) {
        new Thread(() -> {
            try {

                HttpURLConnection conn = getHttpURLConnection(apiKey);

                String requestJson = buildRequestJson(start, end, mode);
                Log.d("Request", requestJson);

                // send request
                OutputStream os = conn.getOutputStream();
                os.write(requestJson.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                // read response
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
                BufferedReader br;
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                // Logcat has a 4000 char limit per line, split it
                String responseStr = response.toString();
                int chunkSize = 3000;
                for (int i = 0; i < responseStr.length(); i += chunkSize) {
                    Log.d("Response", responseStr.substring(i, Math.min(i + chunkSize, responseStr.length())));
                }

               Route route = convertResponseJsonToRoute(response.toString());
                callback.onSuccess(route);
            } catch (Exception e) {
                Log.e("RouteError", "Error: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @NonNull
    private static HttpURLConnection getHttpURLConnection(String apiKey) throws IOException {
        URL url = new URL("https://routes.googleapis.com/directions/v2:computeRoutes");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Goog-Api-Key", apiKey);
        conn.setRequestProperty("X-Goog-FieldMask",
                        "routes.legs.steps.navigationInstruction," +
                        "routes.legs.steps.transitDetails," +
                                "routes.legs.steps.polyline," +
                        "routes.legs.steps.travelMode," +
                        "routes.legs.steps.localizedValues," +
                        "routes.duration," +
                        "routes.distanceMeters");

        conn.setRequestProperty("X-Goog-FieldMask", "routes");
        conn.setDoOutput(true);
        return conn;
    }


    private static String buildRequestJson(LatLng start, LatLng end, RouteTravelMode mode) throws JSONException {
        JSONObject originLatLng = new JSONObject()
                .put("latitude", start.latitude)
                .put("longitude", start.longitude);

        JSONObject destinationLatLng = new JSONObject()
                .put("latitude", end.latitude)
                .put("longitude", end.longitude);


        JSONObject requestBody = new JSONObject()
                .put("origin", new JSONObject()
                        .put("location", new JSONObject()
                                .put("latLng", originLatLng)))
                .put("destination", new JSONObject()
                        .put("location", new JSONObject()
                                .put("latLng", destinationLatLng)))
                .put("travelMode", mode.getValue());
        if (mode == RouteTravelMode.TRANSIT) {
            requestBody.put("transitPreferences", new JSONObject()
                    .put("routingPreference", "LESS_WALKING"));
        }
        return requestBody.toString();

    }

    public static Route convertResponseJsonToRoute(String response) throws JSONException {

        JSONObject jsonResponse = new JSONObject(response);
        JSONArray routes = jsonResponse.optJSONArray("routes");
        JSONObject route = routes.getJSONObject(0);
        JSONArray steps = route
                .getJSONArray("legs").getJSONObject(0)
                .getJSONArray("steps");
        Route routeObj = new Route();
        List<Step> stepList = new ArrayList<>();

        JSONObject localizedValues = route.getJSONObject("localizedValues");
        JSONObject distance = localizedValues.getJSONObject("distance");
        String distanceText = distance.getString("text");
        JSONObject duration = localizedValues.getJSONObject("duration");
        String durationText = duration.getString("text");

        routeObj.setDistance(distanceText);
        routeObj.setDuration(durationText);

        routeObj.setPoints(PolyUtil.decode(route.getJSONObject("polyline").getString("encodedPolyline")));

        routeObj.setSteps(stepList);
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.getJSONObject(i);

            String travelMode = step.getString("travelMode");

            JSONObject polyline = step.getJSONObject("polyline");
            String encodedPolyline = polyline.getString("encodedPolyline");
            List<LatLng> decodedPath = PolyUtil.decode(encodedPolyline);

            Step stepObj = new Step();
            stepObj.setTravelMode(RouteTravelMode.fromString(travelMode));
            stepObj.setPoints(decodedPath);

            if (step.has("transitDetails")) {
                TransitDetails transitDetailsObj = new TransitDetails();
                JSONObject transitDetails = step.getJSONObject("transitDetails");
                JSONObject stopDetails = transitDetails.getJSONObject("stopDetails");

                // Departure stop details
                JSONObject departureStop = stopDetails.getJSONObject("departureStop");
                String departureStopName = departureStop.getString("name");
                JSONObject departureStopLocation = departureStop.getJSONObject("location");
                JSONObject departureStopLocationLatLng = departureStopLocation.getJSONObject("latLng");
                double departureStopLat = departureStopLocationLatLng.getDouble("latitude");
                double departureStopLng = departureStopLocationLatLng.getDouble("longitude");
                String departureTime = stopDetails.getString("departureTime");


                transitDetailsObj.setDepartureStopName(departureStopName);
                transitDetailsObj.setDepartureStopLocation(new LatLng(departureStopLat, departureStopLng));
                transitDetailsObj.setDepartureTime(departureTime);

                // Arrival stop details
                JSONObject arrivalStop = stopDetails.getJSONObject("arrivalStop");
                String arrivalStopName = arrivalStop.getString("name");
                JSONObject arrivalStopLocation = arrivalStop.getJSONObject("location");
                JSONObject arrivalStopLocationLatLng = arrivalStopLocation.getJSONObject("latLng");
                double arrivalStopLat = arrivalStopLocationLatLng.getDouble("latitude");
                double arrivalStopLng = arrivalStopLocationLatLng.getDouble("longitude");
                String arrivalTime = stopDetails.getString("arrivalTime");

                transitDetailsObj.setArrivalStop(arrivalStopName);
                transitDetailsObj.setArrivalStopLocation(new LatLng(arrivalStopLat, arrivalStopLng));
                transitDetailsObj.setArrivalTime(arrivalTime);

                // Vehicle type
                JSONObject transitLine = transitDetails.getJSONObject("transitLine");
                JSONObject vehicle = transitLine.getJSONObject("vehicle");
                String vehicleType = vehicle.getString("type");

                transitDetailsObj.setVehicleType(TransitVehicleType.fromString(vehicleType));

                stepObj.setTransitDetails(transitDetailsObj);

            }
            stepList.add(stepObj);
        }
        return routeObj;
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


    public interface RoutesCallback {
        void onSuccess(Route route);
        void onError(Exception e);
    }

}
