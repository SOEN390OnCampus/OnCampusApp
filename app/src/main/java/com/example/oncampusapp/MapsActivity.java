package com.example.oncampusapp;

import androidx.core.app.ActivityCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.WindowCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.Manifest;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.example.oncampusapp.databinding.ActivityMapsBinding;
import com.google.maps.android.data.Geometry;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;

    public static Map<String, Building> buildingsMap = new HashMap<>();
    private ActivityMapsBinding binding;
    private BuildingDetailsService buildingDetailsService;
    private FusedLocationProviderClient fusedLocationClient;

    // UI Components
    private Button btnSGW, btnLoyola;
    private Button fabCurrentLocation;
    private Button fabBuildingInfo;
    private TextView tvStatus;

    // Campus and building data
    private String currentCampus = CampusData.CAMPUS_SGW;
    private List<Polygon> buildingPolygons = new ArrayList<>();
    private List<CampusData.Building> currentBuildings = new ArrayList<>();
    private CampusData.Building selectedBuilding;
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        3001);
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        2001
                );
            }
        }

        createNotificationChannel();
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

        GeofenceManager geofenceManager = new GeofenceManager(this);

        try {
            // Load the GeoJSON file
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.concordia_buildings, getApplicationContext());

            // Iterate through the GeoJSON file to find the features
            for (GeoJsonFeature feature : layer.getFeatures()) {
                String type = feature.getProperty("type");
                String building = feature.getProperty("building");
                String name = feature.getProperty("name");
                String id = feature.getProperty("@id");
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

                if(isConcordiaBuilding && feature.hasGeometry()) {
                    Geometry geometry = feature.getGeometry();

                    if(geometry instanceof GeoJsonPolygon) {
                        GeoJsonPolygon polygon = (GeoJsonPolygon) feature.getGeometry();
                        List<LatLng> coordinates = polygon.getCoordinates().get(0);

                        LatLng center = GeofenceManager.getPolygonCenter(coordinates);
                        float radius = GeofenceManager.getPolygonRadius(center, coordinates);

                        if (id == null || id.isEmpty()) {
                            id = feature.getId();
                        }
                        if (id == null || id.isEmpty()) {
                            Log.e("Geofence", "Skipping feature, ID is null: " + feature.getProperty("name"));
                            continue;
                        }

                        Building building1 = new Building(id, name, coordinates);

                        buildingsMap.put(id, building1);

                        geofenceManager.addGeofence(
                                id,
                                center.latitude,
                                center.longitude,
                                radius
                        );
                    }
                }
            }

            layer.addLayerToMap();
            // Click listener for each building
            layer.setOnFeatureClickListener((GeoJsonLayer.GeoJsonOnFeatureClickListener) feature -> {
                String name = feature.getProperty("name");
                String id = feature.getProperty("@id"); // Note: This is not the same as Google's API placeId

                // Sample
                Toast.makeText(getApplicationContext(),
                        "Clicked on: " + name + " (ID: " + id + ")",
                        Toast.LENGTH_SHORT).show();
            });
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

                LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
            }
        });
    }

    private void showBuildingDetails() {
        if (selectedBuilding == null) {
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(selectedBuilding.getName())
                .setMessage("Building Code: " + selectedBuilding.getBuildingCode() +
                           "\nCampus: " + selectedBuilding.getCampusCode())
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .show();
    }

    private void showBuildingInformation(CampusData.Building building) {
        if (building == null) {
            Toast.makeText(this, "No building information available", Toast.LENGTH_SHORT).show();
            return;
        }

        String buildingInfo = "Building: " + building.getName() +
                             "\n\nCode: " + building.getBuildingCode() +
                             "\n\nCampus: " + building.getCampusCode();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Building Information")
                .setMessage(buildingInfo)
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .setNegativeButton("More Details", (d, w) -> {
                    // Could open a more detailed view here
                    Toast.makeText(this, "More details coming soon", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "(not available)" : s;
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "GEOFENCE_CHANNEL",
                            "Geofence Notifications",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                    );

            android.app.NotificationManager manager =
                    getSystemService(android.app.NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void updateStatus(String message) {
        Log.i("MapsActivity", "STATUS: " + message);
        if (tvStatus != null) {
            runOnUiThread(() -> {
                tvStatus.setText(message);
                tvStatus.setVisibility(View.VISIBLE);
            });
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void checkGooglePlayServices() {
        try {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
            if (resultCode != ConnectionResult.SUCCESS) {
                Log.e("MapsActivity", "Google Play Services not available: " + resultCode);
                updateStatus("Google Play Services Error: " + resultCode);
            } else {
                Log.d("MapsActivity", "Google Play Services available");
                updateStatus("Google Play Services OK - Ready for Maps");
                Toast.makeText(this, "✅ Google Play Services working", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MapsActivity", "Error checking Google Play Services: " + e.getMessage());
            updateStatus("Google Play Services Check Error");
        }
        buildingClassifier = new BuildingClassifier();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        3001);
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        2001
                );
            }
        }
        // Initialize the permission launcher
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
        });

        // Check and Request on Startup
        checkLocationPermissions();
    }

}
