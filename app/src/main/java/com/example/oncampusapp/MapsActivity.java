package com.example.oncampusapp;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.oncampusapp.navigation.RouteTravelMode;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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
import java.text.SimpleDateFormat;
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
    private final List<Direction> directionsList = new ArrayList<>();
    private int currentDirectionIndex = 0;
    private Map<String, BuildingDetails> geoIdToBuildingDetailsMap;
    private ActivityMapsBinding binding;
    private boolean isRoutePickerOpen = false;
    private boolean isPreviewActive = false;

    private final Handler bannerHandler = new Handler();
    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndDisplayNextEventBanner();
            // Re-run this check every 30 seconds
            bannerHandler.postDelayed(this, 1000);
        }
    };

    private BuildingClassifier buildingClassifier;
    protected BuildingManager buildingManager;
    private GeoJsonLayer layer;
    private Dialog currentBuildingDialog = null;
    private ImageView currentLocationIcon;
    private AutoCompleteTextView startDestinationText;
    private AutoCompleteTextView endDestinationText;
    private RouteTravelMode selectedMode = RouteTravelMode.WALK;
    private LinearLayout routePicker;
    private ImageButton btnSwapAddress;
    public static final LatLng SGW_COORDS = new LatLng(45.496107243097704, -73.57725834380621);
    public static final LatLng LOY_COORDS = new LatLng(45.4582, -73.6405);
    public ILocationProvider fusedLocationClient;
    private FusedLocationSource myLocationSource;

    private final List<Polyline> routePolylines = new ArrayList<>();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private TextView btnSgwLoy;
    private static final String sgw = "SGW";
    private static final String loy = "LOY";

    // Navigation Variables
    private List<LatLng> currentRoutePoints;
    private LocationCallback navigationLocationCallback;
    private com.google.android.gms.maps.model.Circle startDot;
    private com.google.android.gms.maps.model.Marker endMarker;

    // A counter that tells Espresso tests to wait for the map to load
    public CountingIdlingResource mapIdlingResource = new CountingIdlingResource("MapReadyResource");

    // Shuttle Stop Markers - managed by ShuttleHelper
    private com.google.android.gms.maps.model.Marker[] shuttleMarkers = new com.google.android.gms.maps.model.Marker[2];

    // Transport UI buttons (class-level for access across methods)
    private ImageButton btnWalk;
    private android.widget.Button btnShuttleTimetable;
    private android.widget.Button btnGo;


    //Shuttle 3 leg routes polylines
    private Polyline walkToStopPolyline;
    private Polyline shuttlePolyline;
    private Polyline walkFromStopPolyline;

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

        launchPermissionRequest();

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

        setupRoutePickerUi();

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
        loadBuildingDetails();

        checkAndDisplayNextEventBanner();

    }

    @Override
    protected void onResume() {
        super.onResume();
        bannerHandler.post(bannerRunnable);// Start the timer
    }

    @Override
    protected void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable); // Stop to save battery
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

        // Directions layout
        ConstraintLayout dirLayout = findViewById(R.id.dirLayout);
        ImageButton prevDirBtn = findViewById(R.id.prevDirBtn);
        ImageButton nextDirBtn = findViewById(R.id.nextDirBtn);

        // Buttons & Text
        btnGo = findViewById(R.id.btn_go);
        Button btnEndTrip = findViewById(R.id.btn_end_trip);
        TextView txtNavInstruction = findViewById(R.id.txt_nav_instruction);
        TextView txtDuration = findViewById(R.id.txt_duration);

        // Transport Tabs
        btnWalk = findViewById(R.id.btn_mode_walking);
        ImageButton btnCar = findViewById(R.id.btn_mode_driving);
        ImageButton btnTransit = findViewById(R.id.btn_mode_transit);
        View btnShuttle = findViewById(R.id.btn_mode_shuttle);
        List<View> transportTabs = Arrays.asList(btnWalk, btnCar, btnTransit, btnShuttle);

        //Animations
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Set the search bar margin top based on the device's notification bar height
        ViewCompat.setOnApplyWindowInsetsListener(searchBar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());

            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top + 10;
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.setOnApplyWindowInsetsListener(layoutInputs, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());

            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top + 10;
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });

        Runnable closeRoutePicker = () -> {
            slideUp.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {
                    routePicker.setVisibility(View.GONE);
                    searchBar.setVisibility(View.VISIBLE);
                    slideUp.setAnimationListener(null);

                    isRoutePickerOpen = false;
                    checkAndDisplayNextEventBanner();
                }
                @Override public void onAnimationRepeat(Animation animation) {}
            });
            routePicker.startAnimation(slideUp);
        };

        //Search Bar Click Listener
        searchBar.setOnClickListener(v -> {

            isRoutePickerOpen = true;

            View bannerView = findViewById(R.id.included_banner);
            if (bannerView != null) bannerView.setVisibility(View.GONE);

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
        // Read the selected name directly from the adapter to avoid AutoCompleteTextView
        // timing quirks where getText() may still hold the partial typed string.
        startDestinationText.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            startDestinationText.setText(selected);
            startDestinationText.setSelection(selected.length());
            initiateRoutePreview();
        });
        endDestinationText.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            endDestinationText.setText(selected);
            endDestinationText.setSelection(selected.length());
            initiateRoutePreview();
        });

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

        //Shuttle Timetable Button
        btnShuttleTimetable = findViewById(R.id.btn_shuttle_timetable);
        btnShuttleTimetable.setOnClickListener(v -> ShuttleHelper.openTimetable(this));

        btnWalk.setOnClickListener(v -> {
            // Visuals
            btnModeListener.onClick(v);
            this.selectedMode = RouteTravelMode.WALK;
            btnShuttleTimetable.setVisibility(View.GONE);
            adjustGoButtonWidth(btnGo, false);
            ShuttleHelper.hideShuttleStops(shuttleMarkers);
            clearShuttleRoute();
            initiateRoutePreview();
        });
        btnCar.setOnClickListener(v -> {
            // Visuals
            btnModeListener.onClick(v);
            // Logic
            this.selectedMode = RouteTravelMode.DRIVE;
            btnShuttleTimetable.setVisibility(View.GONE);
            adjustGoButtonWidth(btnGo, false);
            ShuttleHelper.hideShuttleStops(shuttleMarkers);
            clearShuttleRoute();
            initiateRoutePreview();
        });
        btnTransit.setOnClickListener(v -> {
            // Visuals
            btnModeListener.onClick(v);
            // Logic
            this.selectedMode = RouteTravelMode.TRANSIT;
            btnShuttleTimetable.setVisibility(View.GONE);
            adjustGoButtonWidth(btnGo, false);
            ShuttleHelper.hideShuttleStops(shuttleMarkers);
            clearShuttleRoute();
            initiateRoutePreview();
        });
        btnShuttle.setOnClickListener(v -> {
            // Visuals
            btnModeListener.onClick(v);
            // Logic
            this.selectedMode = RouteTravelMode.SHUTTLE;
            btnShuttleTimetable.setVisibility(View.VISIBLE);
            adjustGoButtonWidth(btnGo, true);
            clearNormalRoute();
            shuttleMarkers = ShuttleHelper.showShuttleStops(this, mMap, shuttleMarkers);
            initiateRoutePreview();
        });

        //Helper Buttons
        btnSwapAddress.setOnClickListener(v -> {
            swapAddresses();
            initiateRoutePreview();
        });
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

            // ── Indoor room detection ──────────────────────────────────────
            IndoorNode fromRoom = indoorRoomMap.get(startText);
            IndoorNode toRoom   = indoorRoomMap.get(destText);

            if (fromRoom != null && toRoom != null) {
                launchIndoorRoute(fromRoom, toRoom);
                return;
            }
            if (fromRoom != null || toRoom != null) {
                Toast.makeText(this,
                    "Mix of indoor room and outdoor location is not supported.\n"
                    + "Please enter two rooms (e.g. H-867) or two buildings.",
                    Toast.LENGTH_LONG).show();
                return;
            }
            // ── End indoor detection ───────────────────────────────────────

            // Always re-check same-campus rule before starting navigation
            LatLng startCoords = BuildingLookup.getLatLngFromBuildingName(startText, buildingsMap);
            LatLng destCoords = BuildingLookup.getLatLngFromBuildingName(destText, buildingsMap);
            if (startCoords != null && destCoords != null
                    && applySameCampusCheck(startCoords, destCoords)) {
                // Mode was switched — recalculate route and let user press GO again
                initiateRoutePreview();
                return;
            }

            // SAFETY CHECK
            if (selectedMode != RouteTravelMode.SHUTTLE && !isPreviewActive) {
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
            if (txtDuration != null && txtDuration.getText().length() > 0 && !txtDuration.getText().equals("-- MIN")) {
                String instructionText = txtDuration.getText() + " (" + selectedMode.getValue() + ")";
                txtNavInstruction.setText(instructionText);
            } else {
                String instructionText = "Follow the route (" + selectedMode.getValue() + ")";
                txtNavInstruction.setText(instructionText);
            }

            //Zoom Camera for Navigation
            LatLng cameraTarget = null;
            if (selectedMode == RouteTravelMode.SHUTTLE) {
                // For shuttle mode, focus on the start building directly
                cameraTarget = BuildingLookup.getLatLngFromBuildingName(startText, buildingsMap);
            } else if (currentRoutePoints != null && !currentRoutePoints.isEmpty()) {
                cameraTarget = currentRoutePoints.get(0);
            }
            if (cameraTarget != null) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(cameraTarget)
                        .zoom(19f)
                        .tilt(0)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });

        nextDirBtn.setOnClickListener(v -> {
            if (currentDirectionIndex < directionsList.size() - 1) {
                currentDirectionIndex++;
                showCurrentDirection();
            } else {
                Toast.makeText(this, "You are at the last step", Toast.LENGTH_SHORT).show();
            }
        });

        prevDirBtn.setOnClickListener(v -> {
            if (currentDirectionIndex > 0) {
                currentDirectionIndex--;
                showCurrentDirection();
            } else {
                Toast.makeText(this, "You are at the first step", Toast.LENGTH_SHORT).show();
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
                    if (isPreviewActive) isPreviewActive = false;
                    currentDirectionIndex = 0;
                    clearShuttleRoute();
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
        Button btnGo = findViewById(R.id.btn_go);
        LinearLayout layoutNavActive = findViewById(R.id.layout_navigation_active);
        ConstraintLayout dirLayout = findViewById(R.id.dirLayout);
        ImageButton prevDirBtn = findViewById(R.id.prevDirBtn);
        ImageButton nextDirBtn = findViewById(R.id.nextDirBtn);
        TextView textDir = findViewById(R.id.textDir);
        FrameLayout closeSearchLayout = findViewById(R.id.close_search);

        if (isNavigating) {
            layoutInputs.setVisibility(View.GONE);
            layoutTabs.setVisibility(View.GONE);
            btnGo.setVisibility(View.GONE);
            layoutNavActive.setVisibility(View.VISIBLE);
            if (selectedMode!=RouteTravelMode.SHUTTLE) {
                dirLayout.setVisibility(View.VISIBLE);
                prevDirBtn.setVisibility(View.VISIBLE);
                nextDirBtn.setVisibility(View.VISIBLE);
                textDir.setVisibility(View.VISIBLE);
            }
            closeSearchLayout.setVisibility(View.GONE);

        } else {
            layoutInputs.setVisibility(View.VISIBLE);
            layoutTabs.setVisibility(View.VISIBLE);
            btnGo.setVisibility(View.VISIBLE);
            layoutNavActive.setVisibility(View.GONE);
            dirLayout.setVisibility(View.GONE);
            prevDirBtn.setVisibility(View.GONE);
            nextDirBtn.setVisibility(View.GONE);
            textDir.setVisibility(View.GONE);
            closeSearchLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Tell the map to use our custom FusedLocationSource
        mMap.setLocationSource(myLocationSource);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Enable the blue dot (Requires permission check)
        enableMyLocation();

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
        moveMapToLocation(defaultLatLng, 16f);

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

        GeofenceManager geofenceManager = new GeofenceManager(this);
        FeatureStyler featureStyler = new FeatureStyler();
        buildingManager = new BuildingManager();

        FrameLayout closeSearchLayout = findViewById(R.id.close_search);

        ArrayList<String> buildingSuggestions = new ArrayList<>();

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

                if (name != null && !name.trim().isEmpty() && !buildingSuggestions.contains(name)) {
                    buildingSuggestions.add(name);
                }

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

                if (isConcordiaBuilding && feature.hasGeometry()) {
                    Geometry geometry = feature.getGeometry();

                    if (geometry instanceof GeoJsonPolygon) {
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
                        if (id.equals("way/47331993")) {
                            BuildingDetails details = geoIdToBuildingDetailsMap.get(id);
                            center = new LatLng(details.getLat(), details.getLng());
                        }

                        if (geoIdToBuildingDetailsMap.containsKey(id)) {
                            // Create a feature to allow click
                            pointFeatures.add(createSquareFeature(center, id));

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
                    if (clickedLayer.equals("detailButton")) { // Clicked on the button
                        String clickedId = feature.getProperty("id");
                        handleBuildingDetailsButtonClick(clickedId);
                    }
                }
            });

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Sort suggestions for better UX
        Collections.sort(buildingSuggestions);

        // Create the adapter for building suggestions
        searchSuggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, buildingSuggestions);

        // Set the adapter to both views
        startDestinationText.setAdapter(searchSuggestionsAdapter);
        endDestinationText.setAdapter(searchSuggestionsAdapter);

        // Load indoor room labels in background and merge them into the same adapter
        loadIndoorRoomsIntoAdapter();

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

    private GeoJsonFeature createSquareFeature(LatLng center, String id) {
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
        BuildingDetails details = geoIdToBuildingDetailsMap.get(geojsonId);
        if (details == null) {
            Toast.makeText(this, "No details found for this building", Toast.LENGTH_SHORT).show();
            return;
        }

        String buildingName = details.getName();

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
            }

            else if (endDestinationText != null && endDestinationText.hasFocus()) {
                endDestinationText.setText(buildingName);
                endDestinationText.dismissDropDown();
            }
        }

        if (!mapIdlingResource.isIdleNow())
            mapIdlingResource.decrement();
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
                    ViewGroup.LayoutParams.WRAP_CONTENT
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

        txtBuildingCode.setText(buildingDetails.getCode());
        txtBuildingName.setText(buildingDetails.getName());
        txtBuildingName.setPaintFlags(txtBuildingName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        txtBuildingAddress.setText(buildingDetails.getAddress());
        if (buildingDetails.getAddress() != null && !buildingDetails.getAddress().isEmpty()) {
            txtBuildingAddress.setText(buildingDetails.getAddress());
        }
        if (buildingDetails.isAccessible()) {
            llAccessibility.setVisibility(View.VISIBLE);
        } else {
            llAccessibility.setVisibility(View.GONE);
        }
        if (buildingDetails.hasDirectTunnelToMetro()) {
            llMetroConnect.setVisibility(View.VISIBLE);
        } else {
            llMetroConnect.setVisibility(View.GONE);
        }
        if (buildingDetails.getSchedule() == null) {
            llBuildingOpeningHours.setVisibility(View.GONE);
        } else {
            llBuildingOpeningHours.setVisibility(View.VISIBLE);
            txtBuildingOpeningHours.setText(buildingDetails.getSchedule().toString());
        }
        loadBuildingImage(imgBuilding, buildingDetails);
    }

    private void loadBuildingImage(ImageView imgBuilding, BuildingDetails buildingDetails) {
        if (buildingDetails.getImage() != null && !buildingDetails.getImage().isEmpty()) {
            Glide.with(this)
                    .load(buildingDetails.getImage())
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

    private void loadBuildingDetails() {
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
    private void drawRouteOnMap(List<LatLng> decodedPath, String duration, List<Step> steps) {
        if (mMap == null || decodedPath == null) return;

        // Update the Time Text
        TextView txtDuration = findViewById(R.id.txt_duration);
        if (txtDuration != null && duration != null) {
            txtDuration.setText(duration.toUpperCase());
        }
        // Remove old route
        clearNormalRoute();

        // Draw the Line
        this.currentRoutePoints = new ArrayList<>(decodedPath);
        if (isPreviewActive) isPreviewActive = false;
        currentDirectionIndex = 0;

        // Clear old markers and draw new ones so the Swap button is visually obvious
        // Remove old markers/dots if they exist
        if (startDot != null) startDot.remove();
        if (endMarker != null) endMarker.remove();

        if (!decodedPath.isEmpty()) {
            // Draw a Grey Dot for the Start Location
            startDot = mMap.addCircle(new CircleOptions()
                    .center(decodedPath.get(0))
                    .radius(4) // 4 meters wide
                    .fillColor(Color.GRAY)
                    .strokeColor(Color.DKGRAY)
                    .strokeWidth(3)
                    .zIndex(3));

            // Draw a standard Red Pin for the Destination
            endMarker = mMap.addMarker(new MarkerOptions()
                    .position(decodedPath.get(decodedPath.size() - 1))
                    .title("Destination"));
        }

        for (Step step : steps) {

            int color;

            if (step.getTravelMode() == RouteTravelMode.TRANSIT) {
                if (step.getTransitDetails() != null && step.getTransitDetails().getTransitLine() != null) {
                    System.out.println(step.getTransitDetails().getTransitLine().getColor());
                    color = Color.parseColor(step.getTransitDetails()
                            .getTransitLine()
                            .getColor());

                    PolylineOptions options = new PolylineOptions()
                            .addAll(step.getPoints())
                            .color(color)
                            .width(20)
                            .zIndex(2)
                            .geodesic(true);

                    Polyline polyline = mMap.addPolyline(options);
                    routePolylines.add(polyline);
                    isPreviewActive = true;


                } else {
                    color = Color.parseColor("#4285F4");
                }

            } else if (step.getTravelMode()== RouteTravelMode.WALK) {
                PolylineOptions options = new PolylineOptions()
                        .addAll(step.getPoints())
                        .color(Color.parseColor("#4285F4"))
                        .width(20)
                        .zIndex(2)
                        .geodesic(true)
                        .pattern(Arrays.asList(new Dot(), new Gap(20)));

                Polyline polyline = mMap.addPolyline(options);
                routePolylines.add(polyline);
                isPreviewActive = true;
            } else {
                PolylineOptions options = new PolylineOptions()
                        .addAll(step.getPoints())
                        .color(Color.parseColor("#4285F4"))
                        .width(20)
                        .zIndex(2)
                        .geodesic(true);

                Polyline polyline = mMap.addPolyline(options);
                routePolylines.add(polyline);
                isPreviewActive = true;
            }

            Direction dir = new Direction(
                    step.getInstructions(),
                    step.getDistance(),
                    step.getDuration(),
                    step.getTravelMode(),
                    step.getPoints()
            );


            if (step.getTravelMode() == RouteTravelMode.TRANSIT && step.getTransitDetails() != null) {
                LatLng depStop = step.getTransitDetails().getDepartureStopLocation();
                LatLng arrStop = step.getTransitDetails().getArrivalStopLocation();
                Log.d("TRANSIT_STOPS", "Dep: " + depStop + " | Arr: " + arrStop);
                dir.setTransitStops(depStop, arrStop);


            }

            directionsList.add(dir);


        }

        LatLngBounds.Builder builder =
                new LatLngBounds.Builder();
        for (LatLng latLng : decodedPath) {
            builder.include(latLng);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
    }

    private void showCurrentDirection() {
        TextView textDir = findViewById(R.id.textDir);

        if (directionsList.isEmpty()) return;

        Direction stepDir = directionsList.get(currentDirectionIndex);

        String direction = stepDir.getInstructions() + "\nin " + stepDir.getDistance();

        textDir.setText(direction);

        if (stepDir.getTravelMode() == RouteTravelMode.TRANSIT
                && stepDir.getTransitDepartureStop() != null
                && stepDir.getTransitArrivalStop() != null) {

            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(stepDir.getTransitDepartureStop())
                    .include(stepDir.getTransitArrivalStop())
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        } else if (stepDir.getPoints() != null && !stepDir.getPoints().isEmpty()) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : stepDir.getPoints()) builder.include(point);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        }
    }


    @SuppressLint("MissingPermission")
    private void startNavigationUpdates() {
        // Stop any existing listener to be safe
        if (navigationLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(navigationLocationCallback);
        }

        // Create the request (High accuracy, update every 2 seconds)
        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, 2000
                ).build();

        // Define what happens when location changes
        navigationLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                showCurrentDirection();
                for (Location location : locationResult.getLocations()) {
                    showCurrentDirection();
                    // Pass the new location to your existing update logic
                    updateRouteProgress(new LatLng(location.getLatitude(), location.getLongitude()));
                }
            }
        };

        // 4. Start listening
        fusedLocationClient.requestLocationUpdates(request, navigationLocationCallback, Looper.getMainLooper());
    }

    /**
     * Checks if shuttle mode should auto-switch to walking because both locations are on the same campus.
     * @return true if the mode was switched (caller should re-preview and abort current action)
     */
    private boolean applySameCampusCheck(LatLng startCoords, LatLng destCoords) {
        if (selectedMode != RouteTravelMode.SHUTTLE) return false;
        if (!ShuttleHelper.isSameCampus(startCoords, destCoords, SGW_COORDS, LOY_COORDS)) return false;

        selectedMode = RouteTravelMode.WALK;
        if (btnWalk != null) {
            btnWalk.setBackgroundColor(Color.parseColor("#D3D3D3"));
            btnWalk.setAlpha(1.0f);
            int[] otherTabIds = {R.id.btn_mode_driving, R.id.btn_mode_transit, R.id.btn_mode_shuttle};
            for (int id : otherTabIds) {
                View tab = findViewById(id);
                if (tab != null) { tab.setBackgroundResource(0); tab.setAlpha(0.5f); }
            }
        }
        if (btnShuttleTimetable != null) btnShuttleTimetable.setVisibility(View.GONE);
        if (btnGo != null) adjustGoButtonWidth(btnGo, false);
        ShuttleHelper.hideShuttleStops(shuttleMarkers);
        Toast.makeText(this, "Both locations are on the same campus — switched to walking", Toast.LENGTH_SHORT).show();
        return true;
    }




     // Converts a Google Directions API duration string into total minutes.
    //Definition: total = walkToStop + shuttleRide + walkFromStop (A-->ShuttleX-->ShuttleY-->B)
    private int parseDurationToMinutes(String durationText) {
        if (durationText == null || durationText.trim().isEmpty()) return 0;

        durationText = durationText.toLowerCase().trim();
        int totalMinutes = 0;

        String[] parts = durationText.split(" ");

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("hour")) {
                try {
                    totalMinutes += Integer.parseInt(parts[i - 1]) * 60;
                } catch (Exception ignored) {}
            } else if (parts[i].startsWith("min")) {
                try {
                    totalMinutes += Integer.parseInt(parts[i - 1]);
                } catch (Exception ignored) {}
            }
        }

        return totalMinutes;
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

           // If shuttle mode but both locations are on the same campus, auto-switch to walking
            applySameCampusCheck(startCoords, destCoords);

            // Shuttle mode: draw the fixed KML-based route, but fetch real duration from API
            if (selectedMode == RouteTravelMode.SHUTTLE) {

                if (mMap != null && (shuttleMarkers[0] == null || shuttleMarkers[1] == null)) {
                    shuttleMarkers = ShuttleHelper.showShuttleStops(this, mMap, shuttleMarkers);
                }

                if (shuttleMarkers[0] == null || shuttleMarkers[1] == null) {
                    Toast.makeText(this, "Shuttle stops are still loading, please try again", Toast.LENGTH_SHORT).show();
                    return;
                }

                LatLng stopA = shuttleMarkers[0].getPosition();
                LatLng stopB = shuttleMarkers[1].getPosition();

                double distToA = SphericalUtil.computeDistanceBetween(startCoords, stopA);
                double distToB = SphericalUtil.computeDistanceBetween(startCoords, stopB);

                LatLng pickupStop = (distToA <= distToB) ? stopA : stopB;
                LatLng dropoffStop = (pickupStop == stopA) ? stopB : stopA;

                clearNormalRoute();
                clearShuttleRoute();

                // Shuttle leg
                List<LatLng> shuttlePath =
                        ShuttleHelper.getShuttleRoute(pickupStop, dropoffStop);

                shuttlePolyline = drawSegmentPolyline(shuttlePath, false);

                TextView txtDuration = findViewById(R.id.txt_duration);
                if (txtDuration != null) {
                    txtDuration.setText(
                            ShuttleHelper.SHUTTLE_DURATION_FALLBACK.toUpperCase()
                    );
                }

                final int[] walkToMinutes = {0};
                final int[] shuttleMinutes = {0};
                final int[] walkFromMinutes = {0};

                final boolean[] walkToDone = {false};
                final boolean[] shuttleDone = {false};
                final boolean[] walkFromDone = {false};

                Runnable updateTotalDuration = () -> {
                    if (walkToDone[0] && shuttleDone[0] && walkFromDone[0]) {
                        int totalMinutes = walkToMinutes[0] + shuttleMinutes[0] + walkFromMinutes[0];
                        runOnUiThread(() -> {
                            TextView durationView = findViewById(R.id.txt_duration);
                            if (durationView != null) {
                                durationView.setText((totalMinutes + " MIN").toUpperCase());
                            }
                        });
                    }
                };

                // Walk A → pickup
                NavigationHelper.fetchRoute(
                        startCoords,
                        pickupStop,
                        RouteTravelMode.WALK,
                        BuildConfig.MAPS_API_KEY,
                        new NavigationHelper.RoutesCallback() {
                            @Override
                            public void onSuccess(Route route) {
                                List<LatLng> path = route.getPoints();
                                String durationText = route.getDuration();
                                walkToMinutes[0] = parseDurationToMinutes(durationText);
                                walkToDone[0] = true;
                                runOnUiThread(() -> {
                                    walkToStopPolyline = drawSegmentPolyline(path, true);
                                    View walkToLayout = findViewById(R.id.layout_walk_to_shuttle);
                                    TextView walkToView = findViewById(R.id.txt_walk_to_shuttle);
                                    if (walkToLayout != null && walkToView != null) {
                                        if (walkToMinutes[0] > 0) {
                                            walkToView.setText((walkToMinutes[0] + " MIN TO STOP").toUpperCase());
                                            walkToLayout.setVisibility(View.VISIBLE);
                                        } else {
                                            walkToLayout.setVisibility(View.GONE);
                                        }
                                    }
                                });
                                updateTotalDuration.run();
                            }
                            @Override
                            public void onError(Exception e) {
                                e.printStackTrace();
                                walkToDone[0] = true;
                                updateTotalDuration.run();
                            }
                        }
                );


                // Walk dropoff → B
                NavigationHelper.fetchRoute(
                        dropoffStop,
                        destCoords,
                        RouteTravelMode.WALK,
                        BuildConfig.MAPS_API_KEY,
                        new NavigationHelper.RoutesCallback() {
                            @Override
                            public void onSuccess(Route route) {
                                List<LatLng> path = route.getPoints();
                                String durationText = route.getDuration();
                                walkFromMinutes[0] = parseDurationToMinutes(durationText);
                                walkFromDone[0] = true;
                                runOnUiThread(() ->
                                        walkFromStopPolyline = drawSegmentPolyline(path, true));
                                updateTotalDuration.run();
                            }
                            @Override
                            public void onError(Exception e) {
                                e.printStackTrace();
                                walkFromDone[0] = true;
                                updateTotalDuration.run();
                            }
                        }
                );

                ShuttleHelper.fetchDuration(
                        startCoords,
                        BuildConfig.MAPS_API_KEY,
                        new ShuttleHelper.ShuttleCallback() {
                            @Override
                            public void onSuccess(List<LatLng> path, String durationText) {
                                shuttleMinutes[0] = parseDurationToMinutes(durationText);
                                shuttleDone[0] = true;
                                updateTotalDuration.run();
                            }

                            @Override
                            public void onError(Exception e) {
                                e.printStackTrace();
                                shuttleMinutes[0] = parseDurationToMinutes(ShuttleHelper.SHUTTLE_DURATION_FALLBACK);
                                shuttleDone[0] = true;
                                updateTotalDuration.run();
                            }
                        });

                return;
            }


            // Use NavigationHelper Class for all other modes
            NavigationHelper.fetchRoute(startCoords, destCoords, selectedMode, BuildConfig.MAPS_API_KEY, new NavigationHelper.RoutesCallback() {
                @Override
                public void onSuccess(Route route) {
                        runOnUiThread(() -> drawRouteOnMap(route.getPoints(), route.getDuration(), route.getSteps()));
                        runOnUiThread(() -> drawRouteOnMap(route.getPoints(), route.getDuration(), route.getSteps()));
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Failed to load route", Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            if (buildingsMap.isEmpty()) {
                Toast.makeText(this, "Map is still loading, please wait", Toast.LENGTH_SHORT).show();
            } else {
                if(checkForInsideRooms(startName) || checkForInsideRooms(destName)) {
                    String missing = (startCoords == null) ? startName : destName;
                    Toast.makeText(this, "Could not find: \"" + missing + "\"", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private static boolean checkForInsideRooms(String startName ) {
        return !startName.contains("H-") && !startName.contains("VE-") && !startName.contains("CC-")
                && !startName.contains("LB-") && !startName.contains("MB-") && !startName.contains("VL-");
    }



    private void updateRouteProgress(LatLng userLocation) {

        if (currentRoutePoints == null || currentRoutePoints.isEmpty()) return;
        if (routePolylines == null || routePolylines.isEmpty()) return;


        // helper method to get the sliced path (removing already traveled points)
        List<LatLng> updatedPath = NavigationHelper.getUpdatedPath(userLocation, currentRoutePoints, 50.0);

        // Only update if path has changed
        if (updatedPath.size() != currentRoutePoints.size() && isPreviewActive) {
            for (Polyline polyline : routePolylines) {
                if (polyline != null) {
                    polyline.setPoints(updatedPath);
                }
            }

            currentRoutePoints = updatedPath;
        }

        // Check if user has arrived at destination
        if (NavigationHelper.hasArrived(userLocation, currentRoutePoints, 10.0)) {
            Toast.makeText(this, "You have arrived!", Toast.LENGTH_LONG).show();

            Button btnEndTrip = findViewById(R.id.btn_end_trip);
            if (btnEndTrip != null) btnEndTrip.performClick();
        }
    }

    /**
     * Adjusts the GO button width based on whether timetable button is visible
     */
    private void adjustGoButtonWidth(android.widget.Button btnGo, boolean timetableVisible) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnGo.getLayoutParams();
        if (timetableVisible) {
            // Shared width - GO button takes less space
            params.width = 0;
            params.weight = 1.3f;
            int margin = (int) (4 * getResources().getDisplayMetrics().density);
            params.setMarginEnd(margin);
        } else {
            // Full width - GO button takes all space
            params.width = 0;
            params.weight = 1.0f;
            params.setMarginEnd(0);
        }
        btnGo.setLayoutParams(params);
    }


    //Helper methods for shuttle 3 leg addition
     private void clearNormalRoute() {
         for (Polyline polyline : routePolylines) {
             polyline.remove();
         }
         directionsList.clear();
         routePolylines.clear();

        if (startDot != null) startDot.remove();
        startDot = null;

        if (endMarker != null) endMarker.remove();
        endMarker = null;

        if (currentRoutePoints != null) currentRoutePoints.clear();
    }

    private void clearShuttleRoute() {
        if (walkToStopPolyline != null) walkToStopPolyline.remove();
        walkToStopPolyline = null;

        if (shuttlePolyline != null) shuttlePolyline.remove();
        shuttlePolyline = null;

        if (walkFromStopPolyline != null) walkFromStopPolyline.remove();
        walkFromStopPolyline = null;

        View walkToLayout = findViewById(R.id.layout_walk_to_shuttle);
        if (walkToLayout != null) walkToLayout.setVisibility(View.GONE);
    }

    private Polyline drawSegmentPolyline(List<LatLng> path, boolean isDotted) {
        if (mMap == null || path == null || path.isEmpty()) return null;

        PolylineOptions options = new PolylineOptions()
                .addAll(path)
                .color(Color.parseColor("#4285F4"))
                .width(20)
                .zIndex(2)
                .geodesic(true);

        if (isDotted){
            List<PatternItem> pattern = Arrays.asList(new Dot(), new Gap(20));
            options.pattern(pattern);
        }

        return mMap.addPolyline(options);
    }


    public GeoJsonLayer getLayer(){
        return layer;
    }
    public Dialog getCurrentBuildingDialog(){
        return currentBuildingDialog;
    }
    // ==========================================
    // US-3.3 Methods
    // ==========================================

    /**
     * Requirement 5: Identifies the next event and updates the persistent top banner.
     */
    private void checkAndDisplayNextEventBanner() {
        if (isRoutePickerOpen) return;

        String eventsJson = CalendarEventManager.globalEventsJson;
        if (eventsJson == null || eventsJson.isEmpty()) return;

        JSONObject nextClass = CalendarEventManager.findNextUpcomingEvent(eventsJson);
        View bannerView = findViewById(R.id.included_banner);

        if (nextClass != null && bannerView != null) {

            TextView titleView = bannerView.findViewById(R.id.banner_event_title);
            TextView detailsView = bannerView.findViewById(R.id.banner_event_details);

            // Failsafe if views aren't found
            if (titleView == null || detailsView == null) return;

            String title = nextClass.optString("summary", "Class");

            try {
                long now = System.currentTimeMillis();
                String startStr = nextClass.getJSONObject("start").getString("dateTime");
                String endStr = nextClass.getJSONObject("end").getString("dateTime");
                String rawLocation = nextClass.optString("location", "");
                String description = nextClass.optString("description", "");

                SimpleDateFormat exactTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                long startTime = exactTimeFormat.parse(startStr).getTime();
                long endTime = exactTimeFormat.parse(endStr).getTime();

                // 60-MINUTE FILTER
                long sixtyMinutesInMillis = 60 * 60 * 1000;
                if (now < startTime && (startTime - now) > sixtyMinutesInMillis) {
                    bannerView.setVisibility(View.GONE);
                    return;
                }

                bannerView.setVisibility(View.VISIBLE);

                // Find the extra banner views
                TextView timeStatusView = bannerView.findViewById(R.id.banner_time_status);
                TextView onlineTagView = bannerView.findViewById(R.id.banner_online_tag);

                // Set Main Title
                titleView.setText(title);

                // 2. Set Countdown Timer Status and Force Icons
                if (timeStatusView != null && detailsView != null) {
                    String timeStatus = NotificationTimeFormatter.getBannerTimeStatus(now, startTime, endTime);
                    timeStatusView.setText(timeStatus);

                    int redColor = Color.parseColor("#8B1E2D");
                    int greyColor = Color.parseColor("#808080");

                    // Apply red to the text
                    timeStatusView.setTextColor(redColor);

                    // BULLETPROOF ICON INJECTION
                    try {
                        // 1. Clock icon - using built-in Android system icon
                        Drawable clockIcon = ContextCompat.getDrawable(
                                this, android.R.drawable.ic_menu_recent_history);
                        if (clockIcon != null) {
                            clockIcon = DrawableCompat.wrap(clockIcon).mutate();
                            DrawableCompat.setTint(clockIcon, redColor);
                            // Resize to 16dp
                            int size = (int) (16 * getResources().getDisplayMetrics().density);
                            clockIcon.setBounds(0, 0, size, size);
                            timeStatusView.setCompoundDrawables(clockIcon, null, null, null);
                            timeStatusView.setCompoundDrawablePadding(16);
                        }

                        // 2. Location icon - using built-in Android system icon
                        Drawable targetIcon = ContextCompat.getDrawable(
                                this, android.R.drawable.ic_menu_mylocation);
                        if (targetIcon != null) {
                            targetIcon = DrawableCompat.wrap(targetIcon).mutate();
                            DrawableCompat.setTint(targetIcon, greyColor);
                            int size = (int) (16 * getResources().getDisplayMetrics().density);
                            targetIcon.setBounds(0, 0, size, size);
                            detailsView.setCompoundDrawables(targetIcon, null, null, null);
                            detailsView.setCompoundDrawablePadding(16);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Set Smart Location Data
                String parsedLocation = LocationParser.parseSmartLocation(this, title, rawLocation, description);

                if (parsedLocation.equals("Online")) {
                    if (onlineTagView != null) onlineTagView.setVisibility(View.VISIBLE);

                    String searchString = (rawLocation + " " + description).toLowerCase();
                    if (searchString.contains("zoom")) {
                        detailsView.setText("ZOOM MEETING");
                    } else if (searchString.contains("teams")) {
                        detailsView.setText("MICROSOFT TEAMS");
                    } else if (searchString.contains("meet.google")) {
                        detailsView.setText("GOOGLE MEET");
                    } else {
                        detailsView.setText(rawLocation.isEmpty() ? "ONLINE CLASS" : rawLocation.toUpperCase());
                    }
                } else {
                    if (onlineTagView != null) onlineTagView.setVisibility(View.GONE);
                    detailsView.setText(parsedLocation.equals("TBD") && !rawLocation.isEmpty() ? rawLocation : parsedLocation);
                }

            } catch (Exception e) {
                e.printStackTrace();
                titleView.setText("Next: " + title);
                detailsView.setText("Check schedule for details");
            }
        } else if (bannerView != null) {
            bannerView.setVisibility(View.GONE);
        }
    }

    // ==========================================
    // Main Branch Methods
    // ==========================================

    private void handleCloseSearch() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // This triggers the activate() method in our FusedLocationSource
            mMap.setMyLocationEnabled(true);
        } else {
            // Request permissions if not already granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void launchPermissionRequest() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Check Location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Check Notifications (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }
    public List<Polyline> getRoutePolylines() {
        return this.routePolylines;
    }

    public void moveMapToLocation(LatLng location, float zoom) {
        mapIdlingResource.increment();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(location)
                        .zoom(zoom)
                        .tilt(0f)
                        .build()
            ), new GoogleMap.CancelableCallback() {

            @Override
            public void onFinish() {
                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        if (!mapIdlingResource.isIdleNow())
                            mapIdlingResource.decrement();
                    }
                });
            }

            @Override
            public void onCancel() {
                if (!mapIdlingResource.isIdleNow())
                    mapIdlingResource.decrement();
            }
        });
    }

    // ── Indoor navigation helpers ─────────────────────────────────────────────

    /**
     * Loads every labeled room from every building JSON and merges the labels
     * into the shared search autocomplete adapter.
     */
    private void loadIndoorRoomsIntoAdapter() {
        new Thread(() -> {
            String[] buildingIds = {"H", "MB", "LB", "CC", "VE", "VL"};
            List<String> newLabels = new ArrayList<>();

            for (String bid : buildingIds) {
                processBuilding(bid, newLabels);
            }

            runOnUiThread(() -> {
                if (searchSuggestionsAdapter != null && !newLabels.isEmpty()) {
                    searchSuggestionsAdapter.addAll(newLabels);
                    searchSuggestionsAdapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private void processBuilding(String buildingId, List<String> newLabels) {
        int resId = getResources().getIdentifier(buildingId.toLowerCase(), "raw", getPackageName());
        if (resId == 0) return;

        try (java.io.InputStream is = getResources().openRawResource(resId);
             java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(is, "UTF-8"))) {

            String json = reader.lines().collect(Collectors.joining());
            org.json.JSONArray nodes = new org.json.JSONObject(json).getJSONArray("nodes");

            for (int i = 0; i < nodes.length(); i++) {
                processNode(nodes.getJSONObject(i), newLabels);
            }

        } catch (Exception e) {
            Log.e("MapsActivity", "Failed to load indoor rooms for " + buildingId, e);
        }
    }

    private void processNode(org.json.JSONObject obj, List<String> newLabels) {
        String id    = obj.optString("id", "").trim();
        String label = obj.optString("label", "").trim();
        if (label.isEmpty() || indoorRoomMap.containsKey(label)) return;

        IndoorNode node = new IndoorNode(
                id,
                label,
                obj.optString("type", ""),
                obj.optString("buildingId", ""),
                obj.optString("floor", ""),
                (float) obj.optDouble("x", 0.0),
                (float) obj.optDouble("y", 0.0),
                obj.optBoolean("accessible", true)
        );
        indoorRoomMap.put(label, node);
        newLabels.add(label);
    }

    /**
     * Runs Dijkstra for an indoor route between two rooms and launches
     * IndoorMapActivity with the computed path.
     */
    private void launchIndoorRoute(IndoorNode fromRoom, IndoorNode toRoom) {
        String fromBuilding = fromRoom.getRootBuildingId();
        String toBuilding   = toRoom.getRootBuildingId();

        if (!fromBuilding.equalsIgnoreCase(toBuilding)) {
            Toast.makeText(this,
                "Cross-building indoor navigation is not supported yet.\n"
                + "Both rooms must be in the same building.",
                Toast.LENGTH_LONG).show();
            return;
        }

        int resId = getResources().getIdentifier(
                fromBuilding.toLowerCase(), "raw", getPackageName());
        if (resId == 0) {
            Toast.makeText(this, "Building data not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            IndoorGraph graph = new IndoorGraph();
            try (java.io.InputStream is = getResources().openRawResource(resId)) {
                graph.load(is);
            } catch (Exception e) {
                Log.e("MapsActivity", "Graph load failed", e);
                runOnUiThread(() ->
                    Toast.makeText(this, "Error loading building data.", Toast.LENGTH_SHORT).show());
                return;
            }

            List<String> path = graph.shortestPath(fromRoom.getId(), toRoom.getId());

            runOnUiThread(() -> {
                if (path.isEmpty()) {
                    Toast.makeText(this, "No indoor path found between these rooms.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(this, IndoorMapActivity.class);
                intent.putExtra("BUILDING_ID",   fromBuilding);
                intent.putExtra("FLOOR_ID",      fromRoom.getFloorMenuId());
                intent.putExtra("FROM_NODE_ID",  fromRoom.getId());
                intent.putExtra("TO_NODE_ID",    toRoom.getId());
                intent.putExtra("PATH_NODE_IDS", String.join(",", path));
                startActivity(intent);
            });
        }).start();
    }

}