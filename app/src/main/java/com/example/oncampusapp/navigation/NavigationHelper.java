package com.example.oncampusapp.navigation;

import android.os.Handler;
import android.os.Looper;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NavigationHelper {
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_LAT_LNG = "latLng";
    /**
     * Fetches directions from Google API and parses the JSON.
     */
    public static void fetchRoute(LatLng start, LatLng end, RouteTravelMode mode, String apiKey, RoutesCallback callback) {
        if (apiKey == null || apiKey.isEmpty()){
            callback.onError(new IllegalArgumentException("Invalid API key"));
            return;
        }
        new Thread(() -> {
            try {
                Route route = executeRouteRequest(start, end, mode, apiKey);
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(route));
            } catch (IOException | JSONException e) {
                Log.e("NavigationHelper", "Error fetching route", e);
                callback.onError(e);
            }

        }).start();
    }

    /**
     * Executes the route request and returns the response.
     */
    private static Route executeRouteRequest(LatLng start, LatLng end, RouteTravelMode mode, String apiKey) throws IOException, JSONException  {
        HttpURLConnection conn = getHttpURLConnection(apiKey);

        String requestJson = buildRequestJson(start, end, mode);
        Log.d("Request", requestJson);

        OutputStream os = conn.getOutputStream();
        os.write(requestJson.getBytes());
        os.flush();
        os.close();

        String responseStr = readResponse(conn);
        logResponseInChunks(responseStr);

        return convertResponseJsonToRoute(responseStr);
    }
    /**
     * Reads the response from the connection and returns it as a string.
     */
    private static String readResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        InputStream stream = responseCode == HttpURLConnection.HTTP_OK
                ? conn.getInputStream()
                : conn.getErrorStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        return response.toString();
    }

    /**
     * Logs the response in chunks of 3000 characters.
     * Helpful in debugging
     */
    private static void logResponseInChunks(String responseStr) {
        int chunkSize = 3000;
        for (int i = 0; i < responseStr.length(); i += chunkSize) {
            Log.d("Response", responseStr.substring(i, Math.min(i + chunkSize, responseStr.length())));
        }
    }
    /**
     * Creates an HTTP connection to the Google Directions API.
     * Set the request value to only contains the info we needs
     */
    @NonNull
    private static HttpURLConnection getHttpURLConnection(String apiKey) throws IOException {
        URL url = new URL("https://routes.googleapis.com/directions/v2:computeRoutes");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Goog-Api-Key", apiKey);

        conn.setRequestProperty("X-Goog-FieldMask", "routes");
        conn.setDoOutput(true);
        return conn;
    }


    /**
     * Builds the request JSON. For transit set the routing preference to LESS_WALKING
     */
    public static String buildRequestJson(LatLng start, LatLng end, RouteTravelMode mode) throws JSONException {
        JSONObject originLatLng = new JSONObject()
                .put(KEY_LATITUDE, start.latitude)
                .put(KEY_LONGITUDE, start.longitude);

        JSONObject destinationLatLng = new JSONObject()
                .put(KEY_LATITUDE, end.latitude)
                .put(KEY_LONGITUDE, end.longitude);


        JSONObject requestBody = new JSONObject()
                .put("origin", new JSONObject()
                        .put(KEY_LOCATION, new JSONObject()
                                .put(KEY_LAT_LNG, originLatLng)))
                .put("destination", new JSONObject()
                        .put(KEY_LOCATION, new JSONObject()
                                .put(KEY_LAT_LNG, destinationLatLng)))
                .put("travelMode", mode.getValue());
        if (mode == RouteTravelMode.TRANSIT) {
            requestBody.put("transitPreferences", new JSONObject()
                    // Set to LESS_WALKING, no plan for user preference
                    .put("routingPreference", "LESS_WALKING"));
        }
        return requestBody.toString();

    }

    /**
     * Converts the response JSON to a Route object.
     */
    public static Route convertResponseJsonToRoute(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray routes = jsonResponse.optJSONArray("routes");
        if (routes == null || routes.length() == 0) return new Route();

        JSONObject route = routes.optJSONObject(0);
        JSONArray steps = route.getJSONArray("legs").optJSONObject(0).getJSONArray("steps");

        Route routeObj = new Route();
        parseLocalizedValues(route.optJSONObject("localizedValues"), routeObj);
        parsePolyline(route.optJSONObject("polyline"), routeObj);

        List<Step> stepList = new ArrayList<>();
        for (int i = 0; i < steps.length(); i++) {
            stepList.add(parseStep(steps.optJSONObject(i)));
        }
        routeObj.setSteps(stepList);
        return routeObj;
    }

    private static void parseLocalizedValues(JSONObject localizedValues, Route routeObj) {
        if (localizedValues == null) return;
        JSONObject distance = localizedValues.optJSONObject("distance");
        if (distance != null) routeObj.setDistance(distance.optString("text"));
        JSONObject duration = localizedValues.optJSONObject("duration");
        if (duration != null) routeObj.setDuration(duration.optString("text"));
    }

    private static void parsePolyline(JSONObject polylineObj, Route routeObj) {
        if (polylineObj == null) return;
        routeObj.setPoints(PolyUtil.decode(polylineObj.optString("encodedPolyline")));
    }

    private static Step parseStep(JSONObject step) {
        Step stepObj = new Step();
        stepObj.setTravelMode(RouteTravelMode.fromString(step.optString("travelMode")));

        JSONObject polyline = step.optJSONObject("polyline");
        if (polyline != null) stepObj.setPoints(PolyUtil.decode(polyline.optString("encodedPolyline")));

        JSONObject navigationInstruction = step.optJSONObject("navigationInstruction");
        if (navigationInstruction != null) stepObj.setInstructions(navigationInstruction.optString("instructions"));

        JSONObject stepLocalizedValues = step.optJSONObject("localizedValues");
        if (stepLocalizedValues != null) {
            JSONObject distanceObj = stepLocalizedValues.optJSONObject("distance");
            if (distanceObj != null) stepObj.setDistance(distanceObj.optString("text"));
            JSONObject durationObj = stepLocalizedValues.optJSONObject("staticDuration");
            if (durationObj != null) stepObj.setDuration(durationObj.optString("text"));
        }

        if (step.has("transitDetails")) {
            stepObj.setTransitDetails(parseTransitDetails(step.optJSONObject("transitDetails")));
        }
        return stepObj;
    }

    private static TransitDetails parseTransitDetails(JSONObject transitDetails) {
        TransitDetails transitDetailsObj = new TransitDetails();
        if (transitDetails == null) return transitDetailsObj;

        JSONObject stopDetails = transitDetails.optJSONObject("stopDetails");
        if (stopDetails != null) {
            parseStopDetails(stopDetails, transitDetailsObj);
        }

        JSONObject transitLine = transitDetails.optJSONObject("transitLine");
        if (transitLine != null) {
            TransitLine line = new TransitLine();
            line.setName(transitLine.optString("name"));
            line.setColor(transitLine.optString("color"));
            line.setNameShort(transitLine.optString("nameShort"));
            JSONObject vehicle = transitLine.optJSONObject("vehicle");
            if (vehicle != null) transitDetailsObj.setVehicleType(TransitVehicleType.fromString(vehicle.optString("type")));
            transitDetailsObj.setTransitLine(line);
        }
        return transitDetailsObj;
    }

    private static void parseStopDetails(JSONObject stopDetails, TransitDetails transitDetailsObj) {
        JSONObject departureStop = stopDetails.optJSONObject("departureStop");
        if (departureStop != null) {
            transitDetailsObj.setDepartureStopName(departureStop.optString("name"));
            transitDetailsObj.setDepartureTime(stopDetails.optString("departureTime"));
            JSONObject latLng = getLatLng(departureStop);
            if (latLng != null) transitDetailsObj.setDepartureStopLocation(toLatLng(latLng));
        }

        JSONObject arrivalStop = stopDetails.optJSONObject("arrivalStop");
        if (arrivalStop != null) {
            transitDetailsObj.setArrivalStopName(arrivalStop.optString("name"));
            transitDetailsObj.setArrivalTime(stopDetails.optString("arrivalTime"));
            JSONObject latLng = getLatLng(arrivalStop);
            if (latLng != null) transitDetailsObj.setArrivalStopLocation(toLatLng(latLng));
        }
    }

    private static JSONObject getLatLng(JSONObject stop) {
        JSONObject location = stop.optJSONObject(KEY_LOCATION);
        if (location == null) return null;
        return location.optJSONObject(KEY_LAT_LNG);
    }

    private static LatLng toLatLng(JSONObject latLng) {
        return new LatLng(latLng.optDouble(KEY_LATITUDE), latLng.optDouble(KEY_LONGITUDE));
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
