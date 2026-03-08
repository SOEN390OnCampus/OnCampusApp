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
        if (apiKey == null || apiKey.isEmpty()){
            callback.onError(new IllegalArgumentException("Invalid API key"));
            return;
        }
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
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(route);
                });
            } catch (JSONException e) {
                Log.e("NavigationHelper", "Failed to parse route response", e);
                callback.onError(e);
            } catch (IOException e) {
                Log.e("NavigationHelper", "Network error fetching route", e);
                callback.onError(e);
            } catch (NullPointerException e) {
                Log.e("NavigationHelper", "Null pointer exception", e);
                callback.onError(e);
            } catch (Exception e) {
                Log.e("NavigationHelper", "Exception", e);
                callback.onError(e);
            }

        }).start();
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
                    // TODO: Change this hardcoded value as a parameter if there are additional types of
                    // routing preferences that a user may have.
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
        if (routes == null || routes.length() == 0) return new Route(); // No response found, return empty route

        JSONObject route = routes.optJSONObject(0);
        JSONArray steps = route
                .getJSONArray("legs").optJSONObject(0)
                .getJSONArray("steps");
        Route routeObj = new Route();
        List<Step> stepList = new ArrayList<>();


        // Overall distance and duration
        JSONObject localizedValues = route.optJSONObject("localizedValues");
        if (localizedValues!=null){
            JSONObject distance = localizedValues.optJSONObject("distance");
            if (distance !=null) {
                String distanceText = distance.optString("text");
                routeObj.setDistance(distanceText);
            }
            JSONObject duration = localizedValues.optJSONObject("duration");
            if (duration != null){
                String durationText = duration.optString("text");
                routeObj.setDuration(durationText);
            }
        }


        // Overall Polyline
        JSONObject polylineObj = route.optJSONObject("polyline");
        if (polylineObj != null) {
            String encodedPolyline = polylineObj.optString("encodedPolyline");
            routeObj.setPoints(PolyUtil.decode(encodedPolyline));
        }

        routeObj.setSteps(stepList);
        for (int i = 0; i < steps.length(); i++) {
            Step stepObj = new Step();
            JSONObject step = steps.optJSONObject(i);

            stepObj.setTravelMode(RouteTravelMode.fromString(step.optString("travelMode")));

            JSONObject polyline = step.optJSONObject("polyline");
            if (polyline!=null){
                List<LatLng> decodedPath = PolyUtil.decode(polyline.optString("encodedPolyline"));
                stepObj.setPoints(decodedPath);
            }


            JSONObject navigationInstruction = step.optJSONObject("navigationInstruction");
            if (navigationInstruction != null){
                stepObj.setInstructions(navigationInstruction.optString("instructions"));
            }

            JSONObject stepLocalizedValues = step.optJSONObject("localizedValues");
            if (stepLocalizedValues != null) {
                JSONObject distanceObj = stepLocalizedValues.optJSONObject("distance");
                if (distanceObj != null) {
                    stepObj.setDistance(distanceObj.optString("text"));
                }
                JSONObject durationObj = stepLocalizedValues.optJSONObject("staticDuration");
                if (durationObj != null) {
                    stepObj.setDuration(durationObj.optString("text"));
                }
            }

            if (step.has("transitDetails")) {
                TransitDetails transitDetailsObj = new TransitDetails();
                stepObj.setTransitDetails(transitDetailsObj);
                JSONObject transitDetails = step.optJSONObject("transitDetails");
                if (transitDetails==null) continue;


                JSONObject stopDetails = transitDetails.optJSONObject("stopDetails");

                // Departure stop details
                if (stopDetails == null) continue;
                JSONObject departureStop = stopDetails.optJSONObject("departureStop");
                if (departureStop != null){

                    transitDetailsObj.setDepartureStopName(departureStop.optString("name"));
                    transitDetailsObj.setDepartureTime(stopDetails.optString("departureTime"));

                    // Latitude and longitude of departure stop
                    JSONObject departureStopLocation = departureStop.optJSONObject("location");
                    if (departureStopLocation == null) continue;
                    JSONObject departureStopLocationLatLng = departureStopLocation.optJSONObject("latLng");
                    if (departureStopLocationLatLng == null) continue;
                    transitDetailsObj.setDepartureStopLocation(new LatLng(departureStopLocationLatLng.optDouble("latitude"),
                            departureStopLocationLatLng.optDouble("longitude")));

                }

                // Arrival stop details
                JSONObject arrivalStop = stopDetails.optJSONObject("arrivalStop");
                if (arrivalStop != null) {
                    transitDetailsObj.setArrivalStopName(arrivalStop.optString("name"));
                    transitDetailsObj.setArrivalTime(stopDetails.optString("arrivalTime"));

                    JSONObject arrivalStopLocation = arrivalStop.optJSONObject("location");
                    if (arrivalStopLocation == null) continue;
                    JSONObject arrivalStopLocationLatLng = arrivalStopLocation.optJSONObject("latLng");
                    if (arrivalStopLocationLatLng == null) continue;
                    transitDetailsObj.setArrivalStopLocation(new LatLng(arrivalStopLocationLatLng.optDouble("latitude"),
                            arrivalStopLocationLatLng.optDouble("longitude")));
                }

                // Vehicle type
                JSONObject transitLine = transitDetails.optJSONObject("transitLine");
                if (transitLine ==  null) continue;
                TransitLine line = new TransitLine();
                line.setName(transitLine.optString("name"));
                line.setColor(transitLine.optString("color"));
                line.setNameShort(transitLine.optString("nameShort"));

                JSONObject vehicle = transitLine.optJSONObject("vehicle");
                if (vehicle !=null){
                    transitDetailsObj.setVehicleType(TransitVehicleType.fromString(vehicle.optString("type")));
                }

                transitDetailsObj.setTransitLine(line);

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
