package com.example.oncampusapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

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

    private static final LatLng SGW_COORDS = new LatLng(45.496107243097704, -73.57725834380621);
    private static final LatLng LOY_COORDS = new LatLng(45.4582, -73.6405);

    private ActivityResultLauncher<String[]> locationPermissionRequest;

    public GoogleMap getMap() {
        return this.mMap;
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            locationPermissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buildingClassifier = new BuildingClassifier();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Initialize the permission launcher
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
        });

        // Check and Request on Startup
        checkLocationPermissions();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(SGW_COORDS, 17f)
        );;

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

        TextView btnSgwLoy = findViewById(R.id.btn_campus_switch);
        ImageButton btnLocation = findViewById(R.id.btn_location);

        // Click listener to switch between SGW and loyola campus
        btnSgwLoy.setOnClickListener(v -> {
            String currentText = btnSgwLoy.getText().toString();
            String sgw = getResources().getString(R.string.campus_sgw);
            String loy = getResources().getString(R.string.campus_loy);

            if (currentText.equals("SGW")) {
                btnSgwLoy.setText(loy);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(SGW_COORDS, 16f));
            } else {
                btnSgwLoy.setText(sgw);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LOY_COORDS, 16f));
            }
        });

        // Click listener to navigate to the current location
        btnLocation.setOnClickListener(v -> {
            // Check Permissions
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request Permissions if not granted
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                // If granted, enable location and move camera
                mMap.setMyLocationEnabled(true);

                // Get the current location from the map's internal "My Location" data
                android.location.Location myLocation = mMap.getMyLocation();

                if (myLocation != null) {
                    LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
                }
            }
        });
    }
}
