package com.example.oncampusapp;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.test.espresso.idling.CountingIdlingResource;

import android.os.Handler;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.widget.ImageView;

import com.example.oncampusapp.navigation.NavigationHelper;
import com.example.oncampusapp.navigation.Route;
import com.example.oncampusapp.navigation.Step;
import com.example.oncampusapp.navigation.Direction;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.maps.android.SphericalUtil;
import com.example.oncampusapp.location.FusedLocationProvider;
import com.example.oncampusapp.location.FusedLocationSource;
import com.example.oncampusapp.location.ILocationProvider;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.example.oncampusapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.bumptech.glide.Glide;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public static Map<String, Building> buildingsMap = new HashMap<>();
    private ActivityMapsBinding binding;
    // Managers
    private RouteManager routeManager;
    private BuildingDialogManager buildingDialogManager;
    private IndoorNavigationController indoorNavController;
    private LocationPermissionManager locationPermManager;
    private EventBannerManager bannerManager;
    private GeoJsonMapLoader geoJsonMapLoader;
    private RoutePickerController routePickerController;

    private BuildingClassifier buildingClassifier;
    protected BuildingManager buildingManager;
    private GeoJsonLayer layer;
    public static final LatLng SGW_COORDS = new LatLng(45.496107243097704, -73.57725834380621);
    public static final LatLng LOY_COORDS = new LatLng(45.4582, -73.6405);
    public ILocationProvider fusedLocationClient;
    private FusedLocationSource myLocationSource;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private TextView btnSgwLoy;
    private static final String sgw = "SGW";
    private static final String loy = "LOY";

    // A counter that tells Espresso tests to wait for the map to load
    public CountingIdlingResource mapIdlingResource = new CountingIdlingResource("MapReadyResource");

    // Indoor room data – populated in background once map is ready
    private final java.util.Map<String, IndoorNode> indoorRoomMap = new java.util.LinkedHashMap<>();
    private ArrayAdapter<String> searchSuggestionsAdapter;




    public GoogleMap getMap() {
        return this.mMap;
    }

    // for tests. to mock location
    public void setLocationProvider(ILocationProvider provider) {
        this.fusedLocationClient = provider;
        this.myLocationSource = new FusedLocationSource(this, this.fusedLocationClient);
        if (mMap != null) {
            mMap.setLocationSource(this.myLocationSource);
        }

        // Set it globally so the Service can use the mock too!
        if (getApplication() instanceof OnCampusApplication) {
            ((OnCampusApplication) getApplication()).setLocationProvider(provider);
        }
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

    // Register for multiple permissions
    private final ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                Boolean fineLocationGranted = isGranted.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean postNotificationsGranted = isGranted.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);

                if (Boolean.TRUE.equals(fineLocationGranted))
                    Log.d("LocationPermission", "Precise location access granted.");
                else if (Boolean.TRUE.equals(isGranted.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)))
                    Log.d("LocationPermission", "Only approximate location access granted.");

                if (Boolean.TRUE.equals(postNotificationsGranted))
                    Log.d("NotificationPermission", "Notifications granted.");
                else
                    Log.d("NotificationPermission", "Notifications denied.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // locationPermManager is initialized after fusedLocationClient — use direct launcher here
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!perms.isEmpty()) requestMultiplePermissionsLauncher.launch(perms.toArray(new String[0]));

        // ViewBinding: inflate, then set content view ONCE
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Preload shuttle route data from bundled JSON
        ShuttleHelper.init(this);
        View bannerView = findViewById(R.id.included_banner);
        if (bannerView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bannerView, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                // Push the banner down by the height of the status bar + 16 pixels for a nice gap
                mlp.topMargin = insets.top + 16;
                v.setLayoutParams(mlp);
                return windowInsets; // Return the original insets untouched
            });
        }

        buildingClassifier = new BuildingClassifier();

        OnCampusApplication app = (OnCampusApplication) getApplication();

        // If a mock hasn't been injected yet, set the default one
        if (app.getLocationProvider() == null) {
            app.setLocationProvider(new FusedLocationProvider(this));
        }

        // Use the provider from the application
        fusedLocationClient = app.getLocationProvider();

        // Initialize our custom Location Source
        myLocationSource = new FusedLocationSource(this, fusedLocationClient);

        // Initialize managers
        routeManager = new RouteManager(this);
        routeManager.setLocationClient(fusedLocationClient);
        routeManager.setBuildingsMap(buildingsMap);

        buildingDialogManager = new BuildingDialogManager(this);

        indoorNavController = new IndoorNavigationController(this, indoorRoomMap);

        bannerManager = new EventBannerManager(this);
        geoJsonMapLoader = new GeoJsonMapLoader(this, buildingClassifier, buildingDialogManager);

        routePickerController = new RoutePickerController(this, routeManager, indoorNavController, bannerManager);
        routePickerController.setup();

        binding.bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, GoogleCalendarAuthActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });

        locationPermManager = new LocationPermissionManager(this, requestMultiplePermissionsLauncher);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;

        // Increment before starting the async task
        mapIdlingResource.increment();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        buildingDialogManager.loadBuildingDetails();

        bannerManager.checkAndDisplayNextEventBanner();

    }

    @Override
    protected void onResume() {
        super.onResume();
        bannerManager.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        bannerManager.stop();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        routeManager.setMap(mMap);
        routePickerController.setMap(mMap);
        locationPermManager.setMap(mMap);

        // Tell the map to use our custom FusedLocationSource
        mMap.setLocationSource(myLocationSource);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Enable the blue dot (Requires permission check)
        locationPermManager.enableMyLocation();

        btnSgwLoy = findViewById(R.id.btn_campus_switch);
        ImageButton btnLocation = findViewById(R.id.btn_location);

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

        // Move camera to the saved campus
        locationPermManager.moveMapToLocation(defaultLatLng, 16f);

        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);

        // Set up marker click listener for shuttle stops
        mMap.setOnMarkerClickListener(marker -> {
            if (ShuttleHelper.isShuttleStopMarker(this, marker)) {
                marker.showInfoWindow();
                ShuttleHelper.openTimetable(this);
                return true;
            }
            return false; // Let default behavior handle other markers
        });

        FrameLayout closeSearchLayout = findViewById(R.id.close_search);

        geoJsonMapLoader.load(mMap,
                new GeoJsonMapLoader.FeatureClickHandler() {
                    @Override
                    public void onBuildingPolygonClicked(Feature feature) {
                        handleBuildingClick(feature);
                    }
                    @Override
                    public void onDetailButtonClicked(String geojsonId) {
                        handleBuildingDetailsButtonClick(geojsonId);
                    }
                },
                (suggestions, loadedLayer, loadedBuildingManager) -> {
                    layer = loadedLayer;
                    buildingManager = loadedBuildingManager;

                    // Create the adapter for building suggestions
                    searchSuggestionsAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line, suggestions);
                    routePickerController.setSearchAdapter(searchSuggestionsAdapter);
                    indoorNavController.setSearchSuggestionsAdapter(searchSuggestionsAdapter);
                    indoorNavController.loadIndoorRoomsIntoAdapter();
                });

        btnSgwLoy.setOnClickListener(v -> switchCampus());
        btnLocation.setOnClickListener(v -> goToCurrentLocation());
        closeSearchLayout.setOnClickListener(v -> handleCloseSearch());

        // Decrement to signal that the map is now Idle
        if (!mapIdlingResource.isIdleNow())
            mapIdlingResource.decrement();

        ImageButton btnIndoorMap = findViewById(R.id.btn_indoor_map);
        btnIndoorMap.setOnClickListener(v -> {
            BuildingFloorSelectDialog dialog = new BuildingFloorSelectDialog();
            dialog.show(getSupportFragmentManager(), "BuildingFloorSelectDialog");
        });
    }

    private void switchCampus() {
        String currentText = btnSgwLoy.getText().toString();
        SharedPreferences sharedPref = getSharedPreferences("OnCampusPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (currentText.equals(sgw)) {
            btnSgwLoy.setText(loy);
            moveMapToLocation(SGW_COORDS, 16f);
            editor.putString("campus", sgw);
        } else {
            btnSgwLoy.setText(sgw);
            moveMapToLocation(LOY_COORDS, 16f);
            editor.putString("campus", loy);
        }
        editor.apply();
    }

    private void goToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double lat = location.getLatitude();
                            double lng = location.getLongitude();
                            LatLng currentLatLng = new LatLng(lat, lng);
                            moveMapToLocation(currentLatLng, 16f);
                        }
                    });
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            "GEOFENCE_CHANNEL",
                            "Geofence Notifications",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    protected void handleBuildingDetailsButtonClick(String geojsonId) {
        BuildingDetails details = buildingDialogManager.getGeoIdToBuildingDetailsMap().get(geojsonId);
        if (details == null) {
            Toast.makeText(this, "No details found for this building", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!routePickerController.tryFillFocusedField(details.getName())) {
            showBuildingInfoDialog(details);
        }
    }

    /**
     * Handle on click for the building polygon on the map
     * Set the name of the building into start destination search box
     * @param feature feature representing the building
     */
    private void handleBuildingClick(Feature feature) {
        routePickerController.tryFillFocusedField(feature.getProperty("name"));

        if (!mapIdlingResource.isIdleNow())
            mapIdlingResource.decrement();
    }

    private void showBuildingInfoDialog(BuildingDetails buildingDetails) {
        buildingDialogManager.showBuildingInfoDialog(buildingDetails);
    }



    public GeoJsonLayer getLayer(){
        return layer;
    }
    public Dialog getCurrentBuildingDialog(){
        return buildingDialogManager.getCurrentBuildingDialog();
    }
    // ==========================================
    // Main Branch Methods
    // ==========================================

    private void handleCloseSearch() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    public List<Polyline> getRoutePolylines() {
        return routeManager.getRoutePolylines();
    }

    public void moveMapToLocation(LatLng location, float zoom) {
        locationPermManager.moveMapToLocation(location, zoom);
    }

}