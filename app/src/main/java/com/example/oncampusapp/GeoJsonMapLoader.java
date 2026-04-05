package com.example.oncampusapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import java.util.Objects;

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
                processFeature(feature, buildingSuggestions, featureStyler, geofenceManager,
                        buildingManager, map, pointFeatures);
            }

            layer.addLayerToMap();
            for (GeoJsonFeature pf : pointFeatures) layer.addFeature(pf);

            layer.setOnFeatureClickListener(feature -> handleFeatureClick(feature, clickHandler));

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            layer = null;
        }

        Collections.sort(buildingSuggestions);
        onLoaded.onLoaded(buildingSuggestions, layer, buildingManager);
    }

    private void processFeature(GeoJsonFeature feature, ArrayList<String> suggestions,
                                FeatureStyler styler, GeofenceManager geofenceManager,
                                BuildingManager buildingManager, GoogleMap map,
                                List<GeoJsonFeature> pointFeatures) {
        String name     = feature.getProperty("name");
        String building = feature.getProperty("building");
        String operator = feature.getProperty("operator");
        String id       = feature.getProperty("@id");

        if (name != null && !name.trim().isEmpty() && !suggestions.contains(name)) {
            suggestions.add(name);
        }

        boolean isConcordiaBuilding = buildingClassifier.isConcordiaBuilding(building, name, operator);
        applyFeatureStyle(feature, styler.getStyle(feature.getProperty("type"), isConcordiaBuilding));

        if (isConcordiaBuilding && feature.hasGeometry()) {
            processPolygonBuilding(feature, id, name, pointFeatures, geofenceManager, buildingManager, map);
        }
    }

    private void applyFeatureStyle(GeoJsonFeature feature, FeatureStyler.StyleConfig config) {
        if (config.isLineString()) {
            GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
            lineStyle.setColor(config.getStrokeColor());
            lineStyle.setWidth(config.getStrokeWidth());
            feature.setLineStringStyle(lineStyle);
        } else {
            GeoJsonPolygonStyle polyStyle = new GeoJsonPolygonStyle();
            polyStyle.setFillColor(config.getFillColor());
            polyStyle.setStrokeColor(config.getStrokeColor());
            polyStyle.setStrokeWidth(config.getStrokeWidth());
            feature.setPolygonStyle(polyStyle);
        }
    }

    private void handleFeatureClick(Feature feature, FeatureClickHandler clickHandler) {
        if (!(feature.getGeometry() instanceof GeoJsonPolygon)) return;
        String clickedLayer = feature.getProperty("layer");
        if (clickedLayer == null) {
            clickHandler.onBuildingPolygonClicked(feature);
        } else if (clickedLayer.equals("detailButton")) {
            clickHandler.onDetailButtonClicked(feature.getProperty("id"));
        }
    }

    // ── Building polygon processing ───────────────────────────────────────────

    private void processPolygonBuilding(GeoJsonFeature feature, String id, String name,
                                        List<GeoJsonFeature> pointFeatures,
                                        GeofenceManager geofenceManager,
                                        BuildingManager buildingManager,
                                        GoogleMap map) {
        Geometry<?> geometry = feature.getGeometry();
        if (!(geometry instanceof GeoJsonPolygon)) return;

        GeoJsonPolygon polygon = (GeoJsonPolygon) geometry;
        List<LatLng> coordinates = polygon.getCoordinates().get(0);
        LatLng center = GeofenceManager.getPolygonCenter(coordinates);
        float radius  = GeofenceManager.getPolygonRadius(center, coordinates);

        String resolvedId = (id == null || id.isEmpty()) ? feature.getId() : id;
        if (resolvedId == null || resolvedId.isEmpty()) {
            Log.e("Geofence", "Skipping feature, ID is null: " + feature.getProperty("name"));
            return;
        }

        MapsActivity.buildingsMap.put(resolvedId, new Building(resolvedId, name, coordinates));
        buildingManager.addBuilding(new Building(resolvedId, name, coordinates));
        geofenceManager.addGeofence(resolvedId, center.latitude, center.longitude, radius);

        // Special-case for the SP building
        if (resolvedId.equals("way/47331993")) {
            BuildingDetails details = buildingDialogManager
                    .getGeoIdToBuildingDetailsMap().get(resolvedId);
            if (details != null) center = new LatLng(details.getLat(), details.getLng());
        }

        if (buildingDialogManager.getGeoIdToBuildingDetailsMap().containsKey(resolvedId)) {
            pointFeatures.add(createSquareFeature(center, resolvedId));

            String buildingId = Objects.requireNonNull(buildingDialogManager.getGeoIdToBuildingDetailsMap().get(resolvedId)).getCode();

            map.addGroundOverlay(new GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromBitmap(createBuildingMarkerBitmap(buildingId)))
                    .position(center, 20f, 20f)
                    .zIndex(100));


        }
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

    // Helper function used to create the button on top of the button that on click give details
     Bitmap createBuildingMarkerBitmap(String label) {
        int size = 80; // px
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Big white circle in the back for the border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint);

        // Black circle
        float borderWidth = 4f;
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.BLACK);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - borderWidth, circlePaint);

        // Draw white text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Center text vertically
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = size / 2f - (fm.ascent + fm.descent) / 2f;

        canvas.drawText(label, size / 2f, textY, textPaint);

         return bitmap;
    }


}
