package com.example.oncampusapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.widget.ImageView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.example.oncampusapp.databinding.ActivityMapsBinding;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Geometry;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;
import com.google.maps.android.SphericalUtil;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Glide;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    public static Map<String, Building> buildingsMap = new HashMap<>();
    private Map<String, String> geoIdToPlaceIdMap = new HashMap<>();
    private ActivityMapsBinding binding;
    private BuildingClassifier buildingClassifier;
    private Dialog currentBuildingDialog = null;
    private boolean isFetchingBuildingDetails = false;

    public static final LatLng SGW_COORDS = new LatLng(45.496107243097704, -73.57725834380621);
    public static final LatLng LOY_COORDS = new LatLng(45.4582, -73.6405);
    public FusedLocationProviderClient fusedLocationClient;

    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private TextView btnSgwLoy;
    private static final String sgw = "SGW";
    private static final String loy = "LOY";

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
    private BuildingDetailsService buildingDetailsService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize buildingDetailsService
        buildingDetailsService = new BuildingDetailsService(this);


        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buildingClassifier = new BuildingClassifier();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
        
        // Load the GeoJSON ID to Place ID mapping
        loadGeoIdToPlaceIdMapping();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Move camera to SGW campus
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(SGW_COORDS, 17f)
        );

        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);

        GeofenceManager geofenceManager = new GeofenceManager(this);
        FeatureStyler featureStyler = new FeatureStyler();

        btnSgwLoy = findViewById(R.id.btn_campus_switch);
        ImageButton btnLocation = findViewById(R.id.btn_location);

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

                // Identify if it is a Concordia Building
                boolean isConcordiaBuilding = buildingClassifier.isConcordiaBuilding(building, name, operator);

                // Get the Style Configuration from Helper (Replaces the big if/else block)
                FeatureStyler.StyleConfig config = featureStyler.getStyle(type, isConcordiaBuilding);

                // 3. Apply the Style
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
            layer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
                @Override
                public void onFeatureClick(Feature feature) {
                    handleBuildingClick(feature);
                }
            });

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Get the last accessed campus from memory
        SharedPreferences sharedPref = getSharedPreferences("OnCampusPrefs", MODE_PRIVATE);
        String savedCampus = sharedPref.getString("campus", "SGW");
        LatLng defaultLatLng;

        if (savedCampus.equals(sgw)) {
            defaultLatLng = SGW_COORDS;
            btnSgwLoy.setText(loy);
        } else {
            defaultLatLng = LOY_COORDS;
            btnSgwLoy.setText(sgw);
        }

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
            new CameraPosition.Builder()
                .target(defaultLatLng)
                .zoom(16f)
                .tilt(0f)  // Set tilt to 0 to remove 3D buildings
                .build()
        ));

        btnSgwLoy.setOnClickListener(v -> switchCampus());
        btnLocation.setOnClickListener(v -> goToCurrentLocation());
    }

        // Click listener to switch between SGW and Loyola campus
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

        editor.apply();
    }

    // Set the map view to the current location
    private void goToCurrentLocation() {
        // Check Permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request Permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            // If granted, enable location and move camera
            mMap.setMyLocationEnabled(true);

            // Get the current location from the device and set it on the map
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Check if the location is not null
                    if (location != null) {
                        // Get the Latitude (as a double)
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        // Use it! (e.g., move the camera)
                        LatLng currentLatLng = new LatLng(lat, lng);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));

                        Log.d("Location", "Current Latitude: " + lat);
                    } else {
                        Log.d("Location", "The location is null");
                    }
                });
        }
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

    private void loadGeoIdToPlaceIdMapping() {
        try (InputStream inputStream = getResources().openRawResource(R.raw.concordia_building_geoid_to_placeid);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonBuilder.toString());
            for (java.util.Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String key = it.next();
                String value = jsonObject.getString(key);
                geoIdToPlaceIdMap.put(key, value);
            }
        } catch (IOException | JSONException e) {
            Log.e("MapsActivity", "Error loading GeoID to PlaceID mapping", e);
        }
    }

    private void handleBuildingClick(Feature feature) {
        // Prevent multiple simultaneous requests
        if (isFetchingBuildingDetails) {
            return;
        }
        
        // Get the feature ID
        String featureId = feature.getProperty("@id");
        String featureName = feature.getProperty("name");
        
        if (featureId == null || featureId.isEmpty()) {
            featureId = feature.getId();
        }

        if (featureId == null || featureId.isEmpty()) {
            Log.e("MapsActivity", "Feature has no ID for: " + featureName);
            Toast.makeText(this, 
                R.string.toast_building_id_not_found, 
                Toast.LENGTH_SHORT).show();
            return;
        }

        // Look up the Place ID from the mapping
        String placeId = geoIdToPlaceIdMap.get(featureId);
        if (placeId == null) {
            Toast.makeText(this, 
                getString(R.string.toast_no_place_id_mapping, featureName), 
                Toast.LENGTH_LONG).show();
            return;
        }
        
        // Set flag to prevent multiple requests
        isFetchingBuildingDetails = true;
        
        // Create final reference for lambda
        final String finalFeatureId = featureId;
        
        // Fetch building details and show popup
        buildingDetailsService.fetchBuildingDetails(placeId, new BuildingDetailsService.FetchBuildingDetailsCallback() {
            @Override
            public void onSuccess(BuildingDetailsDto buildingDetailsDto) {
                isFetchingBuildingDetails = false;
                runOnUiThread(() -> showBuildingInfoDialog(buildingDetailsDto, finalFeatureId));
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("MapsActivity", "Error fetching building details for placeId: " + placeId, e);
                isFetchingBuildingDetails = false;
            }
        });
    }

    private void showBuildingInfoDialog(BuildingDetailsDto buildingDetails, String featureId) {
        // Dismiss any existing dialog first
        if (currentBuildingDialog != null && currentBuildingDialog.isShowing()) {
            currentBuildingDialog.dismiss();
        }
        
        // Determine campus based on building location
        String campus = getString(R.string.concordia_university);
        Building building = buildingsMap.get(featureId);
        if (building != null && building.polygon != null && !building.polygon.isEmpty()) {
            LatLng buildingLocation = building.polygon.get(0);
            double distToSGW = SphericalUtil.computeDistanceBetween(buildingLocation, SGW_COORDS);
            double distToLoyola = SphericalUtil.computeDistanceBetween(buildingLocation, LOY_COORDS);
            campus = (distToSGW < distToLoyola) ? getString(R.string.sgw_campus_en) : getString(R.string.loyola_campus_en);
        }
        
        // Create dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_building_info);
        
        // Set dialog to be full width
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Get views
        ImageView imgBuilding = dialog.findViewById(R.id.img_building);
        TextView txtBuildingName = dialog.findViewById(R.id.txt_building_name);
        TextView txtBuildingAddress = dialog.findViewById(R.id.txt_building_address);
        TextView txtBuildingDescription = dialog.findViewById(R.id.txt_building_description);
        TextView txtBuildingDescriptionFr = dialog.findViewById(R.id.txt_building_description_fr);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);

        // Set building name (uppercase for title)
        if (buildingDetails.getName() != null && !buildingDetails.getName().isEmpty()) {
            // Extract just the building name (before the first comma)
            String fullName = buildingDetails.getName();
            String buildingName = fullName.contains(",") ? fullName.substring(0, fullName.indexOf(",")).trim() : fullName;
            txtBuildingName.setText(buildingName.toUpperCase());

            String description = getString(
                    R.string.building_description_en,
                    buildingName,
                    campus
            );
            txtBuildingDescription.setText(description);

            String campusFr = campus.equals(getString(R.string.sgw_campus_en))
                    ? getString(R.string.sgw_campus_fr)
                    : getString(R.string.loyola_campus_fr);

            // For French, remove "Building" suffix if present since "Édifice" is used in the template
            String buildingNameFr = buildingName.replace(" Building", "").replace(" building", "");

            String descriptionFr = getString(
                    R.string.building_description_fr,
                    buildingNameFr,
                    campusFr
            );
            txtBuildingDescriptionFr.setText(descriptionFr);
        }

        // Set building address
        if (buildingDetails.getAddress() != null && !buildingDetails.getAddress().isEmpty()) {
            txtBuildingAddress.setText(buildingDetails.getAddress());
        }

        // Load building image using Glide
        if (buildingDetails.getImgUri() != null && !buildingDetails.getImgUri().isEmpty()) {
            Glide.with(this)
                .load(buildingDetails.getImgUri())
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(imgBuilding);
        } else {
            imgBuilding.setImageResource(android.R.color.darker_gray);
        }

        // Close button listener
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            currentBuildingDialog = null;
        });
        
        // Dismiss listener to clear reference
        dialog.setOnDismissListener(d -> currentBuildingDialog = null);

        dialog.show();
        currentBuildingDialog = dialog;
    }
}