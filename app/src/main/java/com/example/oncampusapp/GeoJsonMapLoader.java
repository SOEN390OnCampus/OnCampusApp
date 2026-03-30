package com.example.oncampusapp;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Geometry;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Loads the Concordia GeoJSON layer onto the map, processes buildings into
 * {@link Building} objects, registers geofences, and wires feature click listeners.
 * Previously all of this lived inside MapsActivity.onMapReady().
 */
public class GeoJsonMapLoader {

    /** Receives the results once loading completes. */
    public interface OnLoadedCallback {
        void onLoaded(List<String> buildingSuggestions,
                      GeoJsonLayer layer,
                      BuildingManager buildingManager);
    }

    /** Routes feature-click events back to MapsActivity's existing handlers. */
    public interface FeatureClickHandler {
        void onBuildingPolygonClicked(Feature feature);
        void onDetailButtonClicked(String geojsonId);
    }

    private final MapsActivity activity;
    private final BuildingClassifier buildingClassifier;
    private final BuildingDialogManager buildingDialogManager;

    public GeoJsonMapLoader(MapsActivity activity,
                            BuildingClassifier buildingClassifier,
                            BuildingDialogManager buildingDialogManager) {
        this.activity = activity;
        this.buildingClassifier = buildingClassifier;
        this.buildingDialogManager = buildingDialogManager;
    }

    /**
     * Loads the GeoJSON, builds the buildings map and suggestions list, registers
     * geofences, and invokes {@code onLoaded} when done.
     */
    public void load(GoogleMap map,
                     FeatureClickHandler clickHandler,
                     OnLoadedCallback onLoaded) {

        FeatureStyler featureStyler = new FeatureStyler();
        BuildingManager buildingManager = new BuildingManager();
        GeofenceManager geofenceManager = new GeofenceManager(activity);

        ArrayList<String> buildingSuggestions = new ArrayList<>();
        GeoJsonLayer layer;

        try {
            layer = new GeoJsonLayer(map, R.raw.concordia_buildings, activity.getApplicationContext());
            List<GeoJsonFeature> pointFeatures = new ArrayList<>();

            for (GeoJsonFeature feature : layer.getFeatures()) {
                String type     = feature.getProperty("type");
                String building = feature.getProperty("building");
                String name     = feature.getProperty("name");
                String id       = feature.getProperty("@id");
                String operator = feature.getProperty("operator");

                if (name != null && !name.trim().isEmpty()
                        && !buildingSuggestions.contains(name)) {
                    buildingSuggestions.add(name);
                }

                boolean isConcordiaBuilding = buildingClassifier
                        .isConcordiaBuilding(building, name, operator);

                FeatureStyler.StyleConfig config = featureStyler.getStyle(type, isConcordiaBuilding);

                if (config.isLineString) {
                    GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
                    lineStyle.setColor(config.strokeColor);
                    lineStyle.setWidth(config.strokeWidth);
                    feature.setLineStringStyle(lineStyle);
                } else {
                    GeoJsonPolygonStyle polyStyle = new GeoJsonPolygonStyle();
                    polyStyle.setFillColor(config.fillColor);
                    polyStyle.setStrokeColor(config.strokeColor);
                    polyStyle.setStrokeWidth(config.strokeWidth);
                    feature.setPolygonStyle(polyStyle);
                }

                if (isConcordiaBuilding && feature.hasGeometry()) {
                    Geometry geometry = feature.getGeometry();

                    if (geometry instanceof GeoJsonPolygon) {
                        GeoJsonPolygon polygon = (GeoJsonPolygon) feature.getGeometry();
                        List<LatLng> coordinates = polygon.getCoordinates().get(0);

                        LatLng center = GeofenceManager.getPolygonCenter(coordinates);
                        float radius  = GeofenceManager.getPolygonRadius(center, coordinates);

                        if (id == null || id.isEmpty()) id = feature.getId();
                        if (id == null || id.isEmpty()) {
                            Log.e("Geofence", "Skipping feature, ID is null: "
                                    + feature.getProperty("name"));
                            continue;
                        }

                        Building building1 = new Building(id, name, coordinates);
                        MapsActivity.buildingsMap.put(id, building1);
                        buildingManager.addBuilding(building1);
                        geofenceManager.addGeofence(id, center.latitude, center.longitude, radius);

                        // Special-case for the SP building
                        if (id.equals("way/47331993")) {
                            BuildingDetails details = buildingDialogManager
                                    .getGeoIdToBuildingDetailsMap().get(id);
                            center = new LatLng(details.getLat(), details.getLng());
                        }

                        if (buildingDialogManager.getGeoIdToBuildingDetailsMap()
                                .containsKey(id)) {
                            pointFeatures.add(createSquareFeature(center, id));
                            map.addGroundOverlay(new GroundOverlayOptions()
                                    .image(BitmapDescriptorFactory.fromResource(
                                            R.drawable.ic_building_details))
                                    .position(center, 10f, 10f)
                                    .zIndex(100));
                        }
                    }
                }
            }

            layer.addLayerToMap();
            for (GeoJsonFeature pf : pointFeatures) {
                layer.addFeature(pf);
            }

            layer.setOnFeatureClickListener(feature -> {
                if (feature.getGeometry() instanceof GeoJsonPolygon) {
                    String clickedLayer = feature.getProperty("layer");
                    if (clickedLayer == null) {
                        clickHandler.onBuildingPolygonClicked(feature);
                        return;
                    }
                    if (clickedLayer.equals("detailButton")) {
                        clickHandler.onDetailButtonClicked(feature.getProperty("id"));
                    }
                }
            });

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            layer = null;
        }

        Collections.sort(buildingSuggestions);
        onLoaded.onLoaded(buildingSuggestions, layer, buildingManager);
    }

    // ── Pure geometry helpers (previously private in MapsActivity) ────────────

    GeoJsonFeature createSquareFeature(LatLng center, String id) {
        List<List<LatLng>> coords = new ArrayList<>();
        coords.add(createSquareCorners(center, 10));
        GeoJsonPolygon polygon = new GeoJsonPolygon(coords);

        HashMap<String, String> props = new HashMap<>();
        props.put("id", id);
        props.put("layer", "detailButton");

        GeoJsonFeature feature = new GeoJsonFeature(polygon, id, props, null);

        GeoJsonPolygonStyle invisibleStyle = new GeoJsonPolygonStyle();
        invisibleStyle.setFillColor(Color.TRANSPARENT);
        invisibleStyle.setStrokeColor(Color.TRANSPARENT);
        invisibleStyle.setStrokeWidth(0f);
        feature.setPolygonStyle(invisibleStyle);
        return feature;
    }

    List<LatLng> createSquareCorners(LatLng center, float sideMeters) {
        double latOffset = (sideMeters / 2.0) / 111000f;
        double lngOffset = (sideMeters / 2.0)
                / (111000f * Math.cos(Math.toRadians(center.latitude)));

        List<LatLng> corners = new ArrayList<>();
        corners.add(new LatLng(center.latitude + latOffset, center.longitude - lngOffset)); // NW
        corners.add(new LatLng(center.latitude + latOffset, center.longitude + lngOffset)); // NE
        corners.add(new LatLng(center.latitude - latOffset, center.longitude + lngOffset)); // SE
        corners.add(new LatLng(center.latitude - latOffset, center.longitude - lngOffset)); // SW
        corners.add(new LatLng(center.latitude + latOffset, center.longitude - lngOffset)); // back to NW
        return corners;
    }
}
