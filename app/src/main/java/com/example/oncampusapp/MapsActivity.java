package com.example.oncampusapp;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.oncampusapp.databinding.ActivityMapsBinding;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONException;
import java.io.IOException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private BuildingClassifier buildingClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buildingClassifier = new BuildingClassifier();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);

        try {
            // Load the GeoJSON file
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.concordia_buildings, getApplicationContext());

            // Iterate through the GeoJSON file to find the features
            for (GeoJsonFeature feature : layer.getFeatures()) {
                String type = feature.getProperty("type");
                String building = feature.getProperty("building");
                String name = feature.getProperty("name");
                String operator = feature.getProperty("operator");

                boolean isConcordiaBuilding = buildingClassifier.isConcordiaBuilding(building, name, operator);

                // Check if this is the tunnel (route type)
                if ("route".equals(type)) {
                    // Tunnel style - use LineString style, more transparent
                    GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
                    lineStyle.setColor(0x7F000000);  // 50% transparent black
                    lineStyle.setWidth(8f);
                    feature.setLineStringStyle(lineStyle);
                } else if (isConcordiaBuilding) {
                    // Building style - darker polygon
                    GeoJsonPolygonStyle polyStyle = new GeoJsonPolygonStyle();
                    polyStyle.setFillColor(0xFF912338);  // Fully opaque maroon
                    polyStyle.setStrokeColor(0xFF5E1624);  // Darker maroon outline
                    polyStyle.setStrokeWidth(2f);
                    feature.setPolygonStyle(polyStyle);
                } else {
                    // Irrelevant building, make it invisible
                    GeoJsonPolygonStyle polyStyle = new GeoJsonPolygonStyle();
                    polyStyle.setFillColor(0x00000000);
                    polyStyle.setStrokeColor(0x00000000);
                    polyStyle.setStrokeWidth(0f);
                    feature.setPolygonStyle(polyStyle);
                }
            }

            layer.addLayerToMap();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Move camera to a wider view of Montreal
        LatLng montreal = new LatLng(45.47715, -73.6089);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(montreal)
                        .zoom(13f)
                        .tilt(0f)  // Set tilt to 0 to remove 3D buildings
                        .build()
        ));
    }
}