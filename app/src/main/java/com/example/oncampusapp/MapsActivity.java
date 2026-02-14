package com.example.oncampusapp;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.example.oncampusapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolygonOptions;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    public static Map<String, Building> buildingsMap = new HashMap<>();
    private ActivityMapsBinding binding;
    private BuildingClassifier buildingClassifier;

    public static final LatLng SGW_COORDS = new LatLng(45.496107243097704, -73.57725834380621);
    public static final LatLng LOY_COORDS = new LatLng(45.4582, -73.6405);

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
        FeatureStyler featureStyler = new FeatureStyler();

        CardView searchBar = findViewById(R.id.search_bar_container);
        LinearLayout routePicker = findViewById(R.id.route_picker_container);
        ImageButton btnSwapAddress = findViewById(R.id.btn_swap_address);

        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Helper method for the "Close" action on the search bar
        Runnable closeRoutePicker = () -> {
            slideUp.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    routePicker.setVisibility(View.GONE);
                    searchBar.setVisibility(View.VISIBLE);
                    // Reset listener so it doesn't trigger on other animations
                    slideUp.setAnimationListener(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            routePicker.startAnimation(slideUp);
        };

        // Handle Search Bar Click (Open)
        searchBar.setOnClickListener(v -> {
            searchBar.setVisibility(View.GONE);
            routePicker.setVisibility(View.VISIBLE);
            routePicker.startAnimation(slideDown);
        });

        // Handle Device/System Back Press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If the route picker is open, slide it up
                if (routePicker.getVisibility() == View.VISIBLE) {
                    closeRoutePicker.run();
                } else {
                    // If it's already closed, perform normal back action (exit app)
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

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
}
