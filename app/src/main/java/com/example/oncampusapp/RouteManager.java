package com.example.oncampusapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oncampusapp.navigation.Direction;
import com.example.oncampusapp.navigation.NavigationHelper;
import com.example.oncampusapp.navigation.Route;
import com.example.oncampusapp.navigation.RouteTravelMode;
import com.example.oncampusapp.navigation.Step;
import com.example.oncampusapp.location.ILocationProvider;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RouteManager {

    private static final String TAG = "RouteManager";
    private static final String COLOR_ROUTE_BLUE = "#4285F4";

    private final MapsActivity activity;
    private GoogleMap mMap;
    private ILocationProvider fusedLocationClient;
    private Map<String, Building> buildingsMap;

    // Route state
    private final List<Polyline> routePolylines = new ArrayList<>();
    private final List<Direction> directionsList = new ArrayList<>();
    private int currentDirectionIndex = 0;
    private List<LatLng> currentRoutePoints;
    private LocationCallback navigationLocationCallback;
    private Circle startDot;
    private Marker endMarker;
    private boolean isPreviewActive = false;

    // Shuttle state
    private Polyline walkToStopPolyline;
    private Polyline shuttlePolyline;
    private Polyline walkFromStopPolyline;
    private Marker[] shuttleMarkers = new Marker[2];

    // Mode & UI references set after view inflation
    private RouteTravelMode selectedMode = RouteTravelMode.WALK;
    private ImageButton btnWalk;
    private Button btnShuttleTimetable;
    private Button btnGo;

    public RouteManager(MapsActivity activity) {
        this.activity = activity;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setMap(GoogleMap map) { this.mMap = map; }
    public void setLocationClient(ILocationProvider client) { this.fusedLocationClient = client; }
    public void setBuildingsMap(Map<String, Building> map) { this.buildingsMap = map; }
    public void setTransportButtons(ImageButton btnWalk, Button btnShuttleTimetable, Button btnGo) {
        this.btnWalk = btnWalk;
        this.btnShuttleTimetable = btnShuttleTimetable;
        this.btnGo = btnGo;
    }
    public void setSelectedMode(RouteTravelMode mode) { this.selectedMode = mode; }
    public void setShuttleMarkers(Marker[] markers) { this.shuttleMarkers = markers; }

    // ── Getters ──────────────────────────────────────────────────────────────

    public RouteTravelMode getSelectedMode() { return selectedMode; }
    public Marker[] getShuttleMarkers() { return shuttleMarkers; }
    public boolean isPreviewActive() { return isPreviewActive; }

    // ── Route preview ─────────────────────────────────────────────────────────

    public void initiateRoutePreview(String startName, String destName) {
        if (startName.isEmpty() || destName.isEmpty()) return;

        LatLng startCoords = BuildingLookup.getLatLngFromBuildingName(startName, buildingsMap);
        LatLng destCoords = BuildingLookup.getLatLngFromBuildingName(destName, buildingsMap);

        if (startCoords == null || destCoords == null) {
            if (buildingsMap.isEmpty()) {
                Toast.makeText(activity, "Map is still loading, please wait", Toast.LENGTH_SHORT).show();
            } else if (checkForInsideRooms(startName) || checkForInsideRooms(destName)) {
                String missing = (startCoords == null) ? startName : destName;
                Toast.makeText(activity, "Could not find: \"" + missing + "\"", Toast.LENGTH_LONG).show();
            }
            return;
        }

        hideKeyboard();
        applySameCampusCheck(startCoords, destCoords);

        if (selectedMode == RouteTravelMode.SHUTTLE) {
            initiateShuttleRoute(startCoords, destCoords);
        } else {
            initiateStandardRoute(startCoords, destCoords);
        }
    }

    private void initiateShuttleRoute(LatLng startCoords, LatLng destCoords) {
        if (mMap != null && (shuttleMarkers[0] == null || shuttleMarkers[1] == null)) {
            shuttleMarkers = ShuttleHelper.showShuttleStops(activity, mMap, shuttleMarkers);
        }
        if (shuttleMarkers[0] == null || shuttleMarkers[1] == null) {
            Toast.makeText(activity, "Shuttle stops are still loading, please try again", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng stopA = shuttleMarkers[0].getPosition();
        LatLng stopB = shuttleMarkers[1].getPosition();
        double distToA = SphericalUtil.computeDistanceBetween(startCoords, stopA);
        LatLng pickupStop  = (distToA <= SphericalUtil.computeDistanceBetween(startCoords, stopB)) ? stopA : stopB;
        LatLng dropoffStop = (pickupStop == stopA) ? stopB : stopA;

        clearNormalRoute();
        clearShuttleRoute();
        shuttlePolyline = drawSegmentPolyline(ShuttleHelper.getShuttleRoute(pickupStop, dropoffStop), false);

        TextView txtDuration = activity.findViewById(R.id.txt_duration);
        if (txtDuration != null) txtDuration.setText(ShuttleHelper.SHUTTLE_DURATION_FALLBACK.toUpperCase());

        final int[] walkToMinutes   = {0};
        final int[] shuttleMinutes  = {0};
        final int[] walkFromMinutes = {0};
        final boolean[] walkToDone   = {false};
        final boolean[] shuttleDone  = {false};
        final boolean[] walkFromDone = {false};

        Runnable updateTotal = () -> tryUpdateShuttleTotal(
                walkToMinutes, shuttleMinutes, walkFromMinutes, walkToDone, shuttleDone, walkFromDone);

        fetchWalkToStopRoute(startCoords, pickupStop, walkToMinutes, walkToDone, updateTotal);
        fetchWalkFromStopRoute(dropoffStop, destCoords, walkFromMinutes, walkFromDone, updateTotal);
        fetchShuttleSegmentDuration(startCoords, shuttleMinutes, shuttleDone, updateTotal);
    }

    private void tryUpdateShuttleTotal(int[] walkTo, int[] shuttle, int[] walkFrom,
                                       boolean[] toDone, boolean[] shuttleDone, boolean[] fromDone) {
        if (!toDone[0] || !shuttleDone[0] || !fromDone[0]) return;
        int total = walkTo[0] + shuttle[0] + walkFrom[0];
        activity.runOnUiThread(() -> {
            TextView dv = activity.findViewById(R.id.txt_duration);
            if (dv != null) dv.setText((total + " MIN").toUpperCase());
        });
    }

    private void fetchWalkToStopRoute(LatLng start, LatLng stop,
                                      int[] walkToMinutes, boolean[] walkToDone, Runnable onDone) {
        NavigationHelper.fetchRoute(start, stop, RouteTravelMode.WALK,
                BuildConfig.MAPS_API_KEY, new NavigationHelper.RoutesCallback() {
                    @Override public void onSuccess(Route route) {
                        walkToMinutes[0] = parseDurationToMinutes(route.getDuration());
                        walkToDone[0] = true;
                        activity.runOnUiThread(() -> showWalkToStopResult(route.getPoints(), walkToMinutes[0]));
                        onDone.run();
                    }
                    @Override public void onError(Exception e) {
                        Log.e(TAG, "Walk-to-stop route error", e);
                        walkToDone[0] = true;
                        onDone.run();
                    }
                });
    }

    private void showWalkToStopResult(List<LatLng> points, int minutes) {
        walkToStopPolyline = drawSegmentPolyline(points, true);
        View layout = activity.findViewById(R.id.layout_walk_to_shuttle);
        TextView tv = activity.findViewById(R.id.txt_walk_to_shuttle);
        if (layout == null || tv == null) return;
        if (minutes > 0) {
            tv.setText((minutes + " MIN TO STOP").toUpperCase());
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void fetchWalkFromStopRoute(LatLng stop, LatLng dest,
                                        int[] walkFromMinutes, boolean[] walkFromDone, Runnable onDone) {
        NavigationHelper.fetchRoute(stop, dest, RouteTravelMode.WALK,
                BuildConfig.MAPS_API_KEY, new NavigationHelper.RoutesCallback() {
                    @Override public void onSuccess(Route route) {
                        walkFromMinutes[0] = parseDurationToMinutes(route.getDuration());
                        walkFromDone[0] = true;
                        activity.runOnUiThread(() -> walkFromStopPolyline = drawSegmentPolyline(route.getPoints(), true));
                        onDone.run();
                    }
                    @Override public void onError(Exception e) {
                        Log.e(TAG, "Walk-from-stop route error", e);
                        walkFromDone[0] = true;
                        onDone.run();
                    }
                });
    }

    private void fetchShuttleSegmentDuration(LatLng start, int[] shuttleMinutes,
                                             boolean[] shuttleDone, Runnable onDone) {
        ShuttleHelper.fetchDuration(start, BuildConfig.MAPS_API_KEY,
                new ShuttleHelper.ShuttleCallback() {
                    @Override public void onSuccess(List<LatLng> path, String durationText) {
                        shuttleMinutes[0] = parseDurationToMinutes(durationText);
                        shuttleDone[0] = true;
                        onDone.run();
                    }
                    @Override public void onError(Exception e) {
                        Log.e(TAG, "Shuttle duration fetch error", e);
                        shuttleMinutes[0] = parseDurationToMinutes(ShuttleHelper.SHUTTLE_DURATION_FALLBACK);
                        shuttleDone[0] = true;
                        onDone.run();
                    }
                });
    }

    private void initiateStandardRoute(LatLng startCoords, LatLng destCoords) {
        NavigationHelper.fetchRoute(startCoords, destCoords, selectedMode,
                BuildConfig.MAPS_API_KEY, new NavigationHelper.RoutesCallback() {
                    @Override public void onSuccess(Route route) {
                        activity.runOnUiThread(() ->
                                drawRouteOnMap(route.getPoints(), route.getDuration(), route.getSteps()));
                    }
                    @Override public void onError(Exception e) {
                        Log.e(TAG,"Standard route fetch error", e);
                        activity.runOnUiThread(() ->
                                Toast.makeText(activity, "Failed to load route", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    // ── Route drawing ─────────────────────────────────────────────────────────

    public void drawRouteOnMap(List<LatLng> decodedPath, String duration, List<Step> steps) {
        if (mMap == null || decodedPath == null) return;

        TextView txtDuration = activity.findViewById(R.id.txt_duration);
        if (txtDuration != null && duration != null) {
            txtDuration.setText(duration.toUpperCase());
        }

        clearNormalRoute();
        this.currentRoutePoints = new ArrayList<>(decodedPath);
        if (isPreviewActive) isPreviewActive = false;
        currentDirectionIndex = 0;

        if (startDot != null) startDot.remove();
        if (endMarker != null) endMarker.remove();

        if (!decodedPath.isEmpty()) {
            startDot = mMap.addCircle(new CircleOptions()
                    .center(decodedPath.get(0)).radius(4)
                    .fillColor(Color.GRAY).strokeColor(Color.DKGRAY)
                    .strokeWidth(3).zIndex(3));
            endMarker = mMap.addMarker(new MarkerOptions()
                    .position(decodedPath.get(decodedPath.size() - 1))
                    .title("Destination"));
        }

        for (Step step : steps) {
            PolylineOptions options = buildPolylineOptions(step);
            Polyline polyline = mMap.addPolyline(options);
            routePolylines.add(polyline);
            isPreviewActive = true;

            Direction dir = new Direction(step.getInstructions(), step.getDistance(),
                    step.getDuration(), step.getTravelMode(), step.getPoints());
            if (step.getTravelMode() == RouteTravelMode.TRANSIT && step.getTransitDetails() != null) {
                LatLng dep = step.getTransitDetails().getDepartureStopLocation();
                LatLng arr = step.getTransitDetails().getArrivalStopLocation();
                Log.d("TRANSIT_STOPS", "Dep: " + dep + " | Arr: " + arr);
                dir.setTransitStops(dep, arr);
            }
            directionsList.add(dir);
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng pt : decodedPath) builder.include(pt);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
    }

    private PolylineOptions buildPolylineOptions(Step step) {
        PolylineOptions options = new PolylineOptions()
                .addAll(step.getPoints())
                .width(20).zIndex(2).geodesic(true);

        if (step.getTravelMode() == RouteTravelMode.TRANSIT
                && step.getTransitDetails() != null
                && step.getTransitDetails().getTransitLine() != null) {
            options.color(Color.parseColor(step.getTransitDetails().getTransitLine().getColor()));
        } else if (step.getTravelMode() == RouteTravelMode.WALK) {
            options.color(Color.parseColor(COLOR_ROUTE_BLUE))
                   .pattern(Arrays.asList(new Dot(), new Gap(20)));
        } else {
            options.color(Color.parseColor(COLOR_ROUTE_BLUE));
        }
        return options;
    }

    public Polyline drawSegmentPolyline(List<LatLng> path, boolean isDotted) {
        if (mMap == null || path == null || path.isEmpty()) return null;
        PolylineOptions options = new PolylineOptions()
                .addAll(path).color(Color.parseColor(COLOR_ROUTE_BLUE))
                .width(20).zIndex(2).geodesic(true);
        if (isDotted) options.pattern(Arrays.asList(new Dot(), new Gap(20)));
        return mMap.addPolyline(options);
    }

    // ── Navigation updates ────────────────────────────────────────────────────

    public void showCurrentDirection() {
        TextView textDir = activity.findViewById(R.id.textDir);
        if (directionsList.isEmpty() || textDir == null) return;

        Direction stepDir = directionsList.get(currentDirectionIndex);
        textDir.setText(stepDir.getInstructions() + "\nin " + stepDir.getDistance());

        if (stepDir.getTravelMode() == RouteTravelMode.TRANSIT
                && stepDir.getTransitDepartureStop() != null
                && stepDir.getTransitArrivalStop() != null) {
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(stepDir.getTransitDepartureStop())
                    .include(stepDir.getTransitArrivalStop()).build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        } else if (stepDir.getPoints() != null && !stepDir.getPoints().isEmpty()) {
            LatLngBounds.Builder b = new LatLngBounds.Builder();
            for (LatLng p : stepDir.getPoints()) b.include(p);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 150));
        }
    }

    @SuppressLint("MissingPermission")
    public void startNavigationUpdates() {
        if (navigationLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(navigationLocationCallback);
        }
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build();
        navigationLocationCallback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult result) {
                if (result == null) return;
                showCurrentDirection();
                for (android.location.Location loc : result.getLocations()) {
                    showCurrentDirection();
                    updateRouteProgress(new LatLng(loc.getLatitude(), loc.getLongitude()));
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(request, navigationLocationCallback, Looper.getMainLooper());
    }

    public void updateRouteProgress(LatLng userLocation) {
        if (currentRoutePoints == null || currentRoutePoints.isEmpty()) return;
        if (routePolylines == null || routePolylines.isEmpty()) return;

        List<LatLng> updatedPath = NavigationHelper.getUpdatedPath(userLocation, currentRoutePoints, 50.0);
        if (updatedPath.size() != currentRoutePoints.size() && isPreviewActive) {
            for (Polyline polyline : routePolylines) {
                if (polyline != null) polyline.setPoints(updatedPath);
            }
            currentRoutePoints = updatedPath;
        }

        if (NavigationHelper.hasArrived(userLocation, currentRoutePoints, 10.0)) {
            Toast.makeText(activity, "You have arrived!", Toast.LENGTH_LONG).show();
            Button btnEndTrip = activity.findViewById(R.id.btn_end_trip);
            if (btnEndTrip != null) btnEndTrip.performClick();
        }
    }

    // ── Clear helpers ─────────────────────────────────────────────────────────

    public void clearNormalRoute() {
        for (Polyline p : routePolylines) p.remove();
        directionsList.clear();
        routePolylines.clear();
        if (startDot != null) { startDot.remove(); startDot = null; }
        if (endMarker != null) { endMarker.remove(); endMarker = null; }
        if (currentRoutePoints != null) currentRoutePoints.clear();
    }

    public void clearShuttleRoute() {
        if (walkToStopPolyline != null) { walkToStopPolyline.remove(); walkToStopPolyline = null; }
        if (shuttlePolyline != null)    { shuttlePolyline.remove();    shuttlePolyline = null; }
        if (walkFromStopPolyline != null){ walkFromStopPolyline.remove(); walkFromStopPolyline = null; }
        View walkToLayout = activity.findViewById(R.id.layout_walk_to_shuttle);
        if (walkToLayout != null) walkToLayout.setVisibility(View.GONE);
    }

    // ── Same-campus check ─────────────────────────────────────────────────────

    public boolean applySameCampusCheck(LatLng startCoords, LatLng destCoords) {
        if (selectedMode != RouteTravelMode.SHUTTLE) return false;
        if (!ShuttleHelper.isSameCampus(startCoords, destCoords,
                MapsActivity.SGW_COORDS, MapsActivity.LOY_COORDS)) return false;

        selectedMode = RouteTravelMode.WALK;
        if (btnWalk != null) {
            btnWalk.setBackgroundColor(Color.parseColor("#D3D3D3"));
            btnWalk.setAlpha(1.0f);
            int[] otherTabIds = {R.id.btn_mode_driving, R.id.btn_mode_transit, R.id.btn_mode_shuttle};
            for (int id : otherTabIds) {
                View tab = activity.findViewById(id);
                if (tab != null) { tab.setBackgroundResource(0); tab.setAlpha(0.5f); }
            }
        }
        if (btnShuttleTimetable != null) btnShuttleTimetable.setVisibility(View.GONE);
        if (btnGo != null) adjustGoButtonWidth(btnGo, false);
        ShuttleHelper.hideShuttleStops(shuttleMarkers);
        Toast.makeText(activity, "Both locations are on the same campus — switched to walking",
                Toast.LENGTH_SHORT).show();
        return true;
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    public void adjustGoButtonWidth(Button btnGo, boolean timetableVisible) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnGo.getLayoutParams();
        if (timetableVisible) {
            params.width = 0; params.weight = 1.3f;
            int margin = (int) (4 * activity.getResources().getDisplayMetrics().density);
            params.setMarginEnd(margin);
        } else {
            params.width = 0; params.weight = 1.0f;
            params.setMarginEnd(0);
        }
        btnGo.setLayoutParams(params);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && activity.getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    // ── Pure logic (testable without Android) ─────────────────────────────────

    public static int parseDurationToMinutes(String durationText) {
        if (durationText == null || durationText.trim().isEmpty()) return 0;
        durationText = durationText.toLowerCase().trim();
        int totalMinutes = 0;
        String[] parts = durationText.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("hour")) {
                try { totalMinutes += Integer.parseInt(parts[i - 1]) * 60; } catch (Exception ignored) {// For catching exception
                }
            } else if (parts[i].startsWith("min")) {
                try { totalMinutes += Integer.parseInt(parts[i - 1]); } catch (Exception ignored) {// For catching exception
                }
            }
        }
        return totalMinutes;
    }

    public void stopNavigation() {
        if (navigationLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(navigationLocationCallback);
        }
    }

    public void removeStartDot() {
        if (startDot != null) { startDot.remove(); startDot = null; }
    }

    public void navigateToNextDirection() {
        if (currentDirectionIndex < directionsList.size() - 1) {
            currentDirectionIndex++;
            showCurrentDirection();
        } else {
            if (activity.hasPendingFinalIndoorAfterOutdoor()) {
                Button btnEndTrip = activity.findViewById(R.id.btn_end_trip);
                if (btnEndTrip != null) {
                    btnEndTrip.performClick();
                    return;
                }
            }
            Toast.makeText(activity, "You are at the last step", Toast.LENGTH_SHORT).show();
        }
    }

    public void navigateToPreviousDirection() {
        if (currentDirectionIndex > 0) {
            currentDirectionIndex--;
            showCurrentDirection();
        } else {
            Toast.makeText(activity, "You are at the first step", Toast.LENGTH_SHORT).show();
        }
    }

    public void resetRouteState() {
        if (isPreviewActive) isPreviewActive = false;
        currentDirectionIndex = 0;
        clearShuttleRoute();
        stopNavigation();
        if (currentRoutePoints != null) currentRoutePoints.clear();
    }

    public LatLng getFirstRoutePoint() {
        if (currentRoutePoints != null && !currentRoutePoints.isEmpty())
            return currentRoutePoints.get(0);
        return null;
    }

    public List<Polyline> getRoutePolylines() { return routePolylines; }

    public static boolean checkForInsideRooms(String name) {
        return !name.contains("H-") && !name.contains("VE-") && !name.contains("CC-")
                && !name.contains("LB-") && !name.contains("MB-") && !name.contains("VL-");
    }
}
