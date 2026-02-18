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
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.example.oncampusapp.databinding.ActivityMapsBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.data.Geometry;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;
import com.google.maps.android.SphericalUtil;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
    private Dialog currentBuildingDialog = null;
    private boolean isFetchingBuildingDetails = false;

    private ImageView currentLocationIcon;
    private AutoCompleteTextView startDestinationText;
    private AutoCompleteTextView endDestinationText;
    private LinearLayout routePicker;
    private ImageButton btnSwapAddress;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

// ViewBinding: inflate, then set content view ONCE [web:83]
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

        // Load the GeoJSON ID to Place ID mapping

        // Load building details
        loadBuildingDetails();
    }
    private void setupRoutePickerUi() {
        CardView searchBar = findViewById(R.id.search_bar_container);
        routePicker = findViewById(R.id.route_picker_container);
        startDestinationText = findViewById(R.id.et_start);
        endDestinationText = findViewById(R.id.et_destination);
        currentLocationIcon = findViewById(R.id.currentLocationIcon);

        btnSwapAddress = findViewById(R.id.btn_swap_address);

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

        searchBar.setOnClickListener(v -> {
            searchBar.setVisibility(View.GONE);
            routePicker.setVisibility(View.VISIBLE);
            routePicker.startAnimation(slideDown);
            currentLocationIcon.setVisibility(View.VISIBLE);

            startDestinationText.setFocusableInTouchMode(true);
            startDestinationText.requestFocus();

            startDestinationText.post(() -> {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(startDestinationText, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        });

        btnSwapAddress.setOnClickListener(v -> { swapAddresses(); });
        currentLocationIcon.setOnClickListener(v -> { setCurrentBuilding(); });

        // Handle Device/System Back Press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (routePicker.getVisibility() == View.VISIBLE) {
                    closeRoutePicker.run();
                    startDestinationText.setText("");
                    endDestinationText.setText("");
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.concordia_buildings, getApplicationContext());


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
                        buildingManager.addBuilding(building1);

                        geofenceManager.addGeofence(
                                id,
                                center.latitude,
                                center.longitude,
                                radius
                        );

                        if (geoIdToBuildingDetailsMap.containsKey(id)) { // Check if building has additional details
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
                    if (clickedLayer == null) {
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
        LatLng montreal = new LatLng(45.47715, -73.6089);
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

    /**
     * Helper function used to create a invisible square feature on the map to allow clicking
     * @param center center LatLng coordinate of the feature
     * @param id geojson id of the building feature is bound to
     * @return GeoJsonFeature representing the square feature
     */
    private GeoJsonFeature createSquareFeature(LatLng center, String id){
        // Optional: clickable polygon for “details button”
        List<LatLng> squareCorners = createSquareCorners(center, 10); // 10 meters
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

    /**
     * Helper function for createSquareFeature to calculate the corner coordinates of the feature
     * @param center center LatLng coordinate of the button
     * @param sideMeters side length of the button
     * @return List of LatLng coordinates representing the corners of the button
     */
    private List<LatLng> createSquareCorners(LatLng center, float sideMeters) {
        // Calculate offset in latlng
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
    // Switch between SGW and Loyola campus on the map
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

    /**
     * Handle on click for the information icon on buildings
     * @param geojsonId geojson id of the building
     */
    private void handleBuildingDetailsButtonClick(String geojsonId) {
        BuildingDetails details = geoIdToBuildingDetailsMap.get(geojsonId);

        if (details == null){
            Toast.makeText(this, "No details found for this building", Toast.LENGTH_SHORT).show();
            return;
        }

        String buildingName = details.name;

        // If search view is open, autofill instead of showing dialog
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

        BuildingDetailsDto buildingDetailsDto = new BuildingDetailsDto();
        buildingDetailsDto.setName(details.name);
        buildingDetailsDto.setAddress(details.address);
        buildingDetailsDto.setImgUri(details.image);
        buildingDetailsDto.setAccessibility(details.accessibility);
        showBuildingInfoDialog(buildingDetailsDto, geojsonId);
    }

    /**
     * Displays a dialog containing detailed information about a building.
     * Dismisses any existing dialog before showing the new one. Coordinates the creation,
     * population, and display of the building information dialog.
     *
     * @param buildingDetails the building details retrieved from the Places API
     * @param featureId the building's feature ID used for campus determination
     */
    private void showBuildingInfoDialog(BuildingDetailsDto buildingDetails, String featureId) {
        // Dismiss any existing dialog first
        if (currentBuildingDialog != null && currentBuildingDialog.isShowing()) {
            currentBuildingDialog.dismiss();
        }

        String campus = determineCampus(featureId);
        Dialog dialog = createAndConfigureDialog();
        populateDialogViews(dialog, buildingDetails, campus);
        setupDialogListeners(dialog);

        dialog.show();
        currentBuildingDialog = dialog;
    }

    /**
     * Determines which campus a building belongs to based on its location.
     *
     * @param featureId the building's feature ID
     * @return the campus name as a string resource
     */
    private String determineCampus(String featureId) {
        String campus = getString(R.string.concordia_university);
        Building building = buildingsMap.get(featureId);

        if (building != null && building.polygon != null && !building.polygon.isEmpty()) {
            LatLng buildingLocation = building.polygon.get(0);
            double distToSGW = SphericalUtil.computeDistanceBetween(buildingLocation, SGW_COORDS);
            double distToLoyola = SphericalUtil.computeDistanceBetween(buildingLocation, LOY_COORDS);
            campus = (distToSGW < distToLoyola) ? getString(R.string.sgw_campus_en) : getString(R.string.loyola_campus_en);
        }

        return campus;
    }

    /**
     * Creates and configures the building info dialog window.
     *
     * @return the configured Dialog instance
     */
    private Dialog createAndConfigureDialog() {
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

        return dialog;
    }

    /**
     * Populates the dialog views with building details including name, address, description, and image.
     *
     * @param dialog the Dialog to populate
     * @param buildingDetails the building details data
     * @param campus the campus name
     */
    private void populateDialogViews(Dialog dialog, BuildingDetailsDto buildingDetails, String campus) {
        ImageView imgBuilding = dialog.findViewById(R.id.img_building);
        TextView txtBuildingName = dialog.findViewById(R.id.txt_building_name);
        TextView txtBuildingAddress = dialog.findViewById(R.id.txt_building_address);
        TextView txtBuildingDescription = dialog.findViewById(R.id.txt_building_description);
        TextView txtBuildingDescriptionFr = dialog.findViewById(R.id.txt_building_description_fr);
        ImageView imgAccessibility = dialog.findViewById(R.id.img_accessibility);
        TextView txtAccessibility = dialog.findViewById(R.id.txt_accessibility);


        // Set building name and bilingual descriptions
        if (buildingDetails.getName() != null && !buildingDetails.getName().isEmpty()) {
            String fullName = buildingDetails.getName();
            String buildingName = fullName.contains(",") ? fullName.substring(0, fullName.indexOf(",")).trim() : fullName;
            txtBuildingName.setText(buildingName.toUpperCase());

            String description = getString(R.string.building_description_en, buildingName, campus);
            txtBuildingDescription.setText(description);

            String campusFr = campus.equals(getString(R.string.sgw_campus_en))
                    ? getString(R.string.sgw_campus_fr)
                    : getString(R.string.loyola_campus_fr);

            String buildingNameFr = buildingName.replace(" Building", "").replace(" building", "");
            String descriptionFr = getString(R.string.building_description_fr, buildingNameFr, campusFr);
            txtBuildingDescriptionFr.setText(descriptionFr);
        }

        // Set building address
        if (buildingDetails.getAddress() != null && !buildingDetails.getAddress().isEmpty()) {
            txtBuildingAddress.setText(buildingDetails.getAddress());
        }
        if (buildingDetails.getAccessibility()) {
            imgAccessibility.setVisibility(View.VISIBLE);
            txtAccessibility.setText(R.string.building_accessible);
        } else {
            imgAccessibility.setVisibility(View.GONE);
            txtAccessibility.setText(R.string.building_not_accessible);
        }


        // Load building image
        loadBuildingImage(imgBuilding, buildingDetails);
    }

    /**
     * Loads the building image into the ImageView using Glide.
     *
     * @param imgBuilding ImageView for the building image
     * @param buildingDetails the building details data
     */
    private void loadBuildingImage(ImageView imgBuilding, BuildingDetailsDto buildingDetails) {
        if (buildingDetails.getImgUri() != null && !buildingDetails.getImgUri().isEmpty()) {
            Glide.with(this)
                .load(buildingDetails.getImgUri())
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(imgBuilding);
        } else {
            imgBuilding.setImageResource(android.R.color.darker_gray);
        }
    }

    /**
     * Sets up dialog event listeners for close and dismiss actions.
     *
     * @param dialog the Dialog to set up listeners for
     */
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

    /**
     * Swap the start and end destination
     */
    private void swapAddresses() {
        String startDestinationTextValue = startDestinationText.getText().toString();
        String endDestinationTextValue = endDestinationText.getText().toString();

        startDestinationText.setText(endDestinationTextValue);
        endDestinationText.setText((startDestinationTextValue));

        startDestinationText.clearFocus();
        endDestinationText.clearFocus();
    }

    /**
     *  Set building user is currently in as start destination
     */
    private void setCurrentBuilding() {
        Building currentBuilding = buildingManager.getCurrentBuilding();
        if (currentBuilding != null) {
            startDestinationText.setText(currentBuilding.getName());
        }
    }
}