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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.example.oncampusapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Geometry;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.bumptech.glide.Glide;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    public static Map<String, Building> buildingsMap = new HashMap<>();
    private Map<String, BuildingDetails> geoIdToBuildingDetailsMap;
    private ActivityMapsBinding binding;
    private BuildingClassifier buildingClassifier;
    protected BuildingManager buildingManager;
    private GeoJsonLayer layer;
    private Dialog currentBuildingDialog = null;
    private ImageView currentLocationIcon;
    private AutoCompleteTextView startDestinationText;
    private AutoCompleteTextView endDestinationText;
    private NavigationHelper.Mode selectedMode = NavigationHelper.Mode.WALKING;
    private LinearLayout routePicker;
    private ImageButton btnSwapAddress;
    public static final LatLng SGW_COORDS = new LatLng(45.496107243097704, -73.57725834380621);
    public static final LatLng LOY_COORDS = new LatLng(45.4582, -73.6405);
    public FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private TextView btnSgwLoy;
    private static final String sgw = "SGW";
    private static final String loy = "LOY";

    // Navigation Variables
    private com.google.android.gms.maps.model.Polyline bluePolyline;
    private List<LatLng> currentRoutePoints;
    private com.google.android.gms.location.LocationCallback navigationLocationCallback;
    private com.google.android.gms.maps.model.Circle startDot;
    private com.google.android.gms.maps.model.Marker endMarker;

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
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // ViewBinding: inflate, then set content view ONCE
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupRoutePickerUi();

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

        // Load building details
        loadBuildingDetails();
    }

    private void setupRoutePickerUi() {
        //Initialize Views
        CardView searchBar = findViewById(R.id.search_bar_container);
        routePicker = findViewById(R.id.route_picker_container);
        startDestinationText = findViewById(R.id.et_start);
        endDestinationText = findViewById(R.id.et_destination);
        currentLocationIcon = findViewById(R.id.currentLocationIcon);
        btnSwapAddress = findViewById(R.id.btn_swap_address);

        // The Container Layouts (For hiding/showing)
        LinearLayout layoutInputs = findViewById(R.id.layout_inputs);
        LinearLayout layoutTabs = findViewById(R.id.layout_tabs);
        LinearLayout layoutNavActive = findViewById(R.id.layout_navigation_active);

        // Buttons & Text
        android.widget.Button btnGo = findViewById(R.id.btn_go);
        android.widget.Button btnEndTrip = findViewById(R.id.btn_end_trip);
        TextView txtNavInstruction = findViewById(R.id.txt_nav_instruction);
        TextView txtDuration = findViewById(R.id.txt_duration);

        // Transport Tabs
        ImageButton btnWalk = findViewById(R.id.btn_mode_walking);
        ImageButton btnCar = findViewById(R.id.btn_mode_driving);
        ImageButton btnTransit = findViewById(R.id.btn_mode_transit);
        View btnShuttle = findViewById(R.id.btn_mode_shuttle);
        List<View> transportTabs = Arrays.asList(btnWalk, btnCar, btnTransit, btnShuttle);


        //Animations
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        Runnable closeRoutePicker = () -> {
            slideUp.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {
                    routePicker.setVisibility(View.GONE);
                    searchBar.setVisibility(View.VISIBLE);
                    slideUp.setAnimationListener(null);
                }
                @Override public void onAnimationRepeat(Animation animation) {}
            });
            routePicker.startAnimation(slideUp);
        };

        //Search Bar Click Listener
        searchBar.setOnClickListener(v -> {
            searchBar.setVisibility(View.GONE);
            routePicker.setVisibility(View.VISIBLE);
            routePicker.startAnimation(slideDown);

            // Reset Visibility of sub-layouts in case we came back from navigation
            layoutInputs.setVisibility(View.VISIBLE);
            layoutTabs.setVisibility(View.VISIBLE);
            btnGo.setVisibility(View.VISIBLE);
            layoutNavActive.setVisibility(View.GONE);
            currentLocationIcon.setVisibility(View.VISIBLE);

            startDestinationText.setFocusableInTouchMode(true);
            startDestinationText.requestFocus();

            startDestinationText.post(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(startDestinationText, InputMethodManager.SHOW_IMPLICIT);
            });
        });

        // AUTO-PREVIEW LOGIC
        // Trigger preview immediately when user selects from the dropdown list
        startDestinationText.setOnItemClickListener((parent, view, position, id) -> initiateRoutePreview());
        endDestinationText.setOnItemClickListener((parent, view, position, id) -> initiateRoutePreview());

        //Transport Tab Logic
        View.OnClickListener btnModeListener = v -> {
            v.setBackgroundColor(Color.parseColor("#D3D3D3"));
            v.setAlpha(1.0f);
            for (View tab : transportTabs) {
                if (tab != v) {
                    tab.setBackgroundResource(0);
                    tab.setAlpha(0.5f);
                }
            }
        };

        btnWalk.setOnClickListener(v -> {
            // Visuals
            btnModeListener.onClick(v);
            this.selectedMode = NavigationHelper.Mode.WALKING;
            initiateRoutePreview();
        });
        btnCar.setOnClickListener(v -> {
            // Visuals
            btnModeListener.onClick(v);
            // Logic
            this.selectedMode = NavigationHelper.Mode.DRIVING;
            initiateRoutePreview();
        });
        btnTransit.setOnClickListener(v -> {
            // Visuals
            btnModeListener.onClick(v);
            // Logic
            this.selectedMode = NavigationHelper.Mode.TRANSIT;
            initiateRoutePreview();
        });
        btnShuttle.setOnClickListener(v -> Toast.makeText(this, "Concordia Shuttle is currently unavailable", Toast.LENGTH_SHORT).show());

        //Helper Buttons
        btnSwapAddress.setOnClickListener(v -> { swapAddresses(); initiateRoutePreview(); });
        currentLocationIcon.setOnClickListener(v -> setCurrentBuilding());

        //GO BUTTON (Start Navigation Mode)
        btnGo.setOnClickListener(v -> {
            String startText = startDestinationText.getText().toString().trim();
            String destText = endDestinationText.getText().toString().trim();

            // Block navigation if the user deleted the text
            if (startText.isEmpty() || destText.isEmpty()) {
                Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show();
                return;
            }

            // SAFETY CHECK
            if (bluePolyline == null) {
                Toast.makeText(this, "Calculating route, please wait...", Toast.LENGTH_SHORT).show();
                initiateRoutePreview();
                return;
            }

            // REMOVE THE GREY START DOT SO ONLY THE USER'S LIVE LOCATION SHOWS
            if (startDot != null) {
                startDot.remove();
                startDot = null;
            }

            // Start GPS Tracking
            startNavigationUpdates();
            Toast.makeText(this, "Navigation Started", Toast.LENGTH_SHORT).show();

            toggleNavigationUI(true);

            //Update Nav Bar Text safely
            if(txtDuration != null && txtDuration.getText().length() > 0 && !txtDuration.getText().equals("-- MIN")) {
                String instructionText = txtDuration.getText() + " ("+selectedMode.getValue()+")";
                txtNavInstruction.setText(instructionText);
            } else {
                String instructionText = "Follow the route ("+selectedMode.getValue()+")";
                txtNavInstruction.setText(instructionText);
            }

            //Zoom Camera for Navigation
            if (currentRoutePoints != null && !currentRoutePoints.isEmpty()) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(currentRoutePoints.get(0))
                        .zoom(19f)
                        .tilt(0)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });

        //END TRIP BUTTON (Exit Navigation Mode)
        btnEndTrip.setOnClickListener(v -> {
            //Stop GPS Tracking
            if (navigationLocationCallback != null) {
                fusedLocationClient.removeLocationUpdates(navigationLocationCallback);
            }

            toggleNavigationUI(false);

            //Reset Camera
            CameraPosition flatCam = new CameraPosition.Builder(mMap.getCameraPosition())
                    .tilt(0)
                    .zoom(16f)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(flatCam));
        });

        //Handle Back Press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (routePicker.getVisibility() == View.VISIBLE) {
                    // If in Nav mode, end trip first
                    if (layoutNavActive.getVisibility() == View.VISIBLE) {
                        btnEndTrip.performClick();
                        return;
                    }

                    // Close Picker
                    closeRoutePicker.run();
                    startDestinationText.setText("");
                    endDestinationText.setText("");
                    if (txtDuration != null) txtDuration.setText("");

                    // Clean up map
                    if (bluePolyline != null) bluePolyline.remove();
                    if (navigationLocationCallback != null) {
                        fusedLocationClient.removeLocationUpdates(navigationLocationCallback);
                    }
                    if (currentRoutePoints != null) currentRoutePoints.clear();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    //Helper method to reduce repetitive code
    private void toggleNavigationUI(boolean isNavigating) {
        LinearLayout layoutInputs = findViewById(R.id.layout_inputs);
        LinearLayout layoutTabs = findViewById(R.id.layout_tabs);
        android.widget.Button btnGo = findViewById(R.id.btn_go);
        LinearLayout layoutNavActive = findViewById(R.id.layout_navigation_active);

        if (isNavigating) {
            layoutInputs.setVisibility(View.GONE);
            layoutTabs.setVisibility(View.GONE);
            btnGo.setVisibility(View.GONE);
            layoutNavActive.setVisibility(View.VISIBLE);
        } else {
            layoutInputs.setVisibility(View.VISIBLE);
            layoutTabs.setVisibility(View.VISIBLE);
            btnGo.setVisibility(View.VISIBLE);
            layoutNavActive.setVisibility(View.GONE);
        }
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
        buildingManager = new BuildingManager();

        btnSgwLoy = findViewById(R.id.btn_campus_switch);
        ImageButton btnLocation = findViewById(R.id.btn_location);

        try {
            // Load the GeoJSON file
            layer = new GeoJsonLayer(mMap, R.raw.concordia_buildings, getApplicationContext());
            List<GeoJsonFeature> pointFeatures = new ArrayList<>();

            // Iterate through the GeoJSON file to find the features
            for (GeoJsonFeature feature : layer.getFeatures()) {
                String type = feature.getProperty("type");
                String building = feature.getProperty("building");
                String name = feature.getProperty("name");
                String id = feature.getProperty("@id");
                String operator = feature.getProperty("operator");

                // Identify if it is a Concordia Building
                boolean isConcordiaBuilding = buildingClassifier.isConcordiaBuilding(building, name, operator);

                // Get the Style Configuration
                FeatureStyler.StyleConfig config = featureStyler.getStyle(type, isConcordiaBuilding);

                // Apply the Style
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
                        buildingManager.addBuilding(building1);

                        geofenceManager.addGeofence(
                                id,
                                center.latitude,
                                center.longitude,
                                radius
                        );

                        // Special case for the SP building
                        if (id.equals("way/47331993")){
                            center = new LatLng(45.45786742002923, -73.64158635998182);
                        }

                        if (geoIdToBuildingDetailsMap.containsKey(id)) {
                            // Create a feature to allow click
                            pointFeatures.add(createSquareFeature(center,id));

                            // Ground overlay to visually show the button
                            mMap.addGroundOverlay(new GroundOverlayOptions()
                                    .image(BitmapDescriptorFactory.fromResource(R.drawable.ic_building_details))
                                    .position(center, 10f, 10f)
                                    .zIndex(100));
                        }
                    }
                }
            }
            layer.addLayerToMap();
            // Add the clickable features
            for (GeoJsonFeature pf : pointFeatures) {
                layer.addFeature(pf);
            }

            layer.setOnFeatureClickListener(feature -> {
                if (feature.getGeometry() instanceof GeoJsonPolygon) {
                    String clickedLayer = feature.getProperty("layer");
                    if (clickedLayer == null) { //Clicked on the building
                        handleBuildingClick(feature);
                        return;
                    }
                    if (clickedLayer.equals("detailButton")){ // Clicked on the button
                        String clickedId = feature.getProperty("id");
                        handleBuildingDetailsButtonClick(clickedId);
                    }
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

        // String array for building suggestions
        String[] buildingSuggestions = buildingsMap.values()
                .stream()
                .map(Building::getName)
                .filter(Objects::nonNull)
                .toArray(String[]::new);

        // Create the adapter for building suggestions
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, buildingSuggestions);

        // Set the adapter to both views
        startDestinationText.setAdapter(adapter);
        endDestinationText.setAdapter(adapter);

        // Move camera to a wider view of Montreal
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(defaultLatLng)
                        .zoom(16f)
                        .tilt(0f)
                        .build()
        ));

        btnSgwLoy.setOnClickListener(v -> switchCampus());
        btnLocation.setOnClickListener(v -> goToCurrentLocation());
    }

    private GeoJsonFeature createSquareFeature(LatLng center, String id){
        List<LatLng> squareCorners = createSquareCorners(center, 10);
        List<List<LatLng>> coords = new ArrayList<>();
        coords.add(squareCorners);
        GeoJsonPolygon detailsButtonPolygon = new GeoJsonPolygon(coords);

        HashMap<String, String> props = new HashMap<>();
        props.put("id", id);
        props.put("layer", "detailButton");
        GeoJsonFeature feature = new GeoJsonFeature(
                detailsButtonPolygon,
                id,
                props,
                null
        );
        GeoJsonPolygonStyle invisibleStyle = new GeoJsonPolygonStyle();
        invisibleStyle.setFillColor(Color.TRANSPARENT);
        invisibleStyle.setStrokeColor(Color.TRANSPARENT);
        invisibleStyle.setStrokeWidth(0f);

        feature.setPolygonStyle(invisibleStyle);
        return feature;
    }

    private List<LatLng> createSquareCorners(LatLng center, float sideMeters) {
        double latOffset = (sideMeters / 2.0) / 111000f;
        double lngOffset = (sideMeters / 2.0) / (111000f * Math.cos(Math.toRadians(center.latitude)));

        List<LatLng> corners = new ArrayList<>();
        corners.add(new LatLng(center.latitude + latOffset, center.longitude - lngOffset)); // NW
        corners.add(new LatLng(center.latitude + latOffset, center.longitude + lngOffset)); // NE
        corners.add(new LatLng(center.latitude - latOffset, center.longitude + lngOffset)); // SE
        corners.add(new LatLng(center.latitude - latOffset, center.longitude - lngOffset)); // SW
        corners.add(new LatLng(center.latitude + latOffset, center.longitude - lngOffset)); // back to NW
        return corners;
    }

    private void switchCampus() {
        String currentText = btnSgwLoy.getText().toString();
        SharedPreferences sharedPref = getSharedPreferences("OnCampusPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (currentText.equals(sgw)) {
            btnSgwLoy.setText(loy);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(SGW_COORDS, 16f));
            editor.putString("campus", sgw);
        } else {
            btnSgwLoy.setText(sgw);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LOY_COORDS, 16f));
            editor.putString("campus", loy);
        }
        editor.apply();
    }

    private void goToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double lat = location.getLatitude();
                            double lng = location.getLongitude();
                            LatLng currentLatLng = new LatLng(lat, lng);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
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

    protected void handleBuildingDetailsButtonClick(String geojsonId) {
        BuildingDetails details = geoIdToBuildingDetailsMap.get(geojsonId);
        if (details == null){
            Toast.makeText(this, "No details found for this building", Toast.LENGTH_SHORT).show();
            return;
        }

        String buildingName = details.name;

        if (routePicker != null && routePicker.getVisibility() == View.VISIBLE) {
            if (startDestinationText != null && startDestinationText.hasFocus()) {
                startDestinationText.setText(buildingName);
                startDestinationText.dismissDropDown();
                return;
            }
            if (endDestinationText != null && endDestinationText.hasFocus()) {
                endDestinationText.setText(buildingName);
                endDestinationText.dismissDropDown();
                return;
            }
        }

        showBuildingInfoDialog(details);
    }

    /**
     * Handle on click for the building polygon on the map
     * Set the name of the building into start destination search box
     * @param feature feature representing the building
     */
    private void handleBuildingClick(Feature feature) {
        String buildingName = feature.getProperty("name");

        if (routePicker != null && routePicker.getVisibility() == View.VISIBLE) {

            if (startDestinationText != null && startDestinationText.hasFocus()) {
                startDestinationText.setText(buildingName);
                startDestinationText.dismissDropDown();
                return;
            }

            if (endDestinationText != null && endDestinationText.hasFocus()) {
                endDestinationText.setText(buildingName);
                endDestinationText.dismissDropDown();
            }
        }
    }

    /**
     * Displays a dialog containing detailed information about a building.
     * Dismisses any existing dialog before showing the new one. Coordinates the creation,
     * population, and display of the building information dialog.
     *
     * @param buildingDetails the building details retrieved from the Places API
     */
    private void showBuildingInfoDialog(BuildingDetails buildingDetails) {
        if (currentBuildingDialog != null && currentBuildingDialog.isShowing()) {
            currentBuildingDialog.dismiss();
        }

        Dialog dialog = createAndConfigureDialog();
        populateDialogViews(dialog, buildingDetails);
        setupDialogListeners(dialog);

        dialog.show();
        currentBuildingDialog = dialog;
    }

    private Dialog createAndConfigureDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_building_details);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.8),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void populateDialogViews(Dialog dialog, BuildingDetails buildingDetails) {

        TextView txtBuildingCode = dialog.findViewById(R.id.txt_building_code);

        TextView txtBuildingName = dialog.findViewById(R.id.txt_building_name);
        TextView txtBuildingAddress = dialog.findViewById(R.id.txt_building_address);

        LinearLayout llBuildingOpeningHours = dialog.findViewById(R.id.layout_building_opening_hours);
        TextView txtBuildingOpeningHours = dialog.findViewById(R.id.txt_building_opening_hours);

        LinearLayout llAccessibility = dialog.findViewById(R.id.item_accessibility);
        LinearLayout llMetroConnect = dialog.findViewById(R.id.item_metro_connect);

        ImageView imgBuilding = dialog.findViewById(R.id.img_building);

        txtBuildingCode.setText(buildingDetails.code);
        txtBuildingName.setText(buildingDetails.name);
        txtBuildingName.setPaintFlags(txtBuildingName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        txtBuildingAddress.setText(buildingDetails.address);
        if (buildingDetails.address != null && !buildingDetails.address.isEmpty()) {
            txtBuildingAddress.setText(buildingDetails.address);
        }
        if (buildingDetails.accessibility) {
            llAccessibility.setVisibility(View.VISIBLE);
        } else {
            llAccessibility.setVisibility(View.GONE);
        }
        if (buildingDetails.hasDirectTunnelToMetro) {
            llMetroConnect.setVisibility(View.VISIBLE);
        } else {
            llMetroConnect.setVisibility(View.GONE);
        }
        if (buildingDetails.schedule == null) {
            llBuildingOpeningHours.setVisibility(View.GONE);
        } else {
            llBuildingOpeningHours.setVisibility(View.VISIBLE);
            txtBuildingOpeningHours.setText(buildingDetails.schedule.toString());
        }
        loadBuildingImage(imgBuilding, buildingDetails);
    }

    private void loadBuildingImage(ImageView imgBuilding, BuildingDetails buildingDetails) {
        if (buildingDetails.image != null && !buildingDetails.image.isEmpty()) {
            Glide.with(this)
                    .load(buildingDetails.image)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.darker_gray)
                    .into(imgBuilding);
        } else {
            imgBuilding.setImageResource(android.R.color.darker_gray);
        }
    }

    private void setupDialogListeners(Dialog dialog) {
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            currentBuildingDialog = null;
        });

        dialog.setOnDismissListener(d -> currentBuildingDialog = null);
    }

    private void loadBuildingDetails(){
        try {
            InputStream is = this.getResources().openRawResource(R.raw.concordia_building_details);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            String json = jsonBuilder.toString();

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, BuildingDetails>>(){}.getType();
            geoIdToBuildingDetailsMap = gson.fromJson(json, type);
        } catch (Resources.NotFoundException | IOException e) {
            throw new RuntimeException("File not found:" + e.getMessage());
        }
    }

    private void swapAddresses() {
        String startDestinationTextValue = startDestinationText.getText().toString();
        String endDestinationTextValue = endDestinationText.getText().toString();

        startDestinationText.setText(endDestinationTextValue);
        endDestinationText.setText((startDestinationTextValue));

        startDestinationText.clearFocus();
        endDestinationText.clearFocus();
    }

    private void setCurrentBuilding() {
        Building currentBuilding = buildingManager.getCurrentBuilding();
        if (currentBuilding != null) {
            startDestinationText.setText(currentBuilding.getName());
        }
    }

    // Update signature to accept duration
    private void drawRouteOnMap(List<LatLng> decodedPath, String duration, boolean isDotted) {
        if (mMap == null || decodedPath == null) return;

        // Update the Time Text
        TextView txtDuration = findViewById(R.id.txt_duration);
        if (txtDuration != null && duration != null) {
            txtDuration.setText(duration.toUpperCase());
        }

        // Draw the Line
        this.currentRoutePoints = new ArrayList<>(decodedPath);
        if (bluePolyline != null) bluePolyline.remove();

        // Clear old markers and draw new ones so the Swap button is visually obvious
        // Remove old markers/dots if they exist
        if (startDot != null) startDot.remove();
        if (endMarker != null) endMarker.remove();

        if (!decodedPath.isEmpty()) {
            // Draw a Grey Dot for the Start Location
            startDot = mMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                    .center(decodedPath.get(0))
                    .radius(4) // 4 meters wide
                    .fillColor(Color.GRAY)
                    .strokeColor(Color.DKGRAY)
                    .strokeWidth(3)
                    .zIndex(3));

            // Draw a standard Red Pin for the Destination
            endMarker = mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .position(decodedPath.get(decodedPath.size() - 1))
                    .title("Destination"));
        }

        PolylineOptions blueOptions = new PolylineOptions()
                .addAll(decodedPath)
                .color(Color.parseColor("#4285F4"))
                .width(20)
                .zIndex(2)
                .geodesic(true);
        if (isDotted){
            List<PatternItem> pattern = Arrays.asList(new Dot(), new Gap(20));
            blueOptions.pattern(pattern);
        }

        bluePolyline = mMap.addPolyline(blueOptions);

        // Zoom Camera
        com.google.android.gms.maps.model.LatLngBounds.Builder builder =
                new com.google.android.gms.maps.model.LatLngBounds.Builder();
        for (LatLng latLng : decodedPath) {
            builder.include(latLng);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void startNavigationUpdates() {
        // Stop any existing listener to be safe
        if (navigationLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(navigationLocationCallback);
        }

        // Create the request (High accuracy, update every 2 seconds)
        com.google.android.gms.location.LocationRequest request =
                new com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000
                ).build();

        // Define what happens when location changes
        navigationLocationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    // Pass the new location to your existing update logic
                    updateRouteProgress(new LatLng(location.getLatitude(), location.getLongitude()));
                }
            }
        };

        // 4. Start listening
        fusedLocationClient.requestLocationUpdates(request, navigationLocationCallback, android.os.Looper.getMainLooper());
    }

    private void initiateRoutePreview() {
        String startName = startDestinationText.getText().toString().trim();
        String destName = endDestinationText.getText().toString().trim();

        if (startName.isEmpty() || destName.isEmpty()) return;

        LatLng startCoords = BuildingLookup.getLatLngFromBuildingName(startName, buildingsMap);
        LatLng destCoords = BuildingLookup.getLatLngFromBuildingName(destName, buildingsMap);

        if (startCoords != null && destCoords != null) {
            // Hide Keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            // Use NavigationHelper Class
            NavigationHelper.fetchDirections(startCoords, destCoords, selectedMode, BuildConfig.MAPS_API_KEY, new NavigationHelper.DirectionsCallback() {
                @Override
                public void onSuccess(List<LatLng> path, String durationText) {
                    if (selectedMode== NavigationHelper.Mode.WALKING){
                        runOnUiThread(() -> drawRouteOnMap(path, durationText, true)); // Dotted line for walking
                    } else {
                        runOnUiThread(() -> drawRouteOnMap(path, durationText, false)); // Straight line for everything else
                    }
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Failed to load route", Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            Toast.makeText(this, "Could not find one of the locations", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateRouteProgress(LatLng userLocation) {
        if (currentRoutePoints == null || currentRoutePoints.isEmpty()) return;

        // Use Helper To Get The Sliced Path
        List<LatLng> updatedPath = NavigationHelper.getUpdatedPath(userLocation, currentRoutePoints, 50.0);

        // Update the UI immediately if the path changed
        if (updatedPath.size() != currentRoutePoints.size() && bluePolyline != null) {
            bluePolyline.setPoints(updatedPath);
            currentRoutePoints = updatedPath; // Save the new state
        }

        // Use Helper To Check Arrival
        if (NavigationHelper.hasArrived(userLocation, currentRoutePoints, 10.0)) {
            Toast.makeText(this, "You have arrived!", Toast.LENGTH_LONG).show();

            android.widget.Button btnEndTrip = findViewById(R.id.btn_end_trip);
            if (btnEndTrip != null) btnEndTrip.performClick(); // Reset the UI
        }
    }
    public Polyline getBluePolyline(){
        return bluePolyline;
    }
    public GeoJsonLayer getLayer(){
        return layer;
    }
    public Dialog getCurrentBuildingDialog(){
        return currentBuildingDialog;
    }
}