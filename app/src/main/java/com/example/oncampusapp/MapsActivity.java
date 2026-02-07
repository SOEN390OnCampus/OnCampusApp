package com.example.oncampusapp;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.example.oncampusapp.databinding.ActivityMapsBinding;

import java.util.List;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MapsActivity", "=== MapsActivity onCreate started ===");

        // Initialize services
        buildingDetailsService = new BuildingDetailsService(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Log.d("MapsActivity", "Services initialized");

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("MapsActivity", "Layout set successfully");

        // Initialize UI components
        setupUIComponents();

        // Check Google Play Services availability
        checkGooglePlayServices();

        // Get map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            Log.d("MapsActivity", "Map fragment found, requesting map");
            updateStatus("Loading Google Maps...");
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapsActivity", "Map fragment is null!");
            updateStatus("ERROR: Map fragment not found!");
        }
    }

    private void setupUIComponents() {
        Log.d("MapsActivity", "Setting up UI components...");

        // Find UI elements
        btnSGW = findViewById(R.id.btnSGW);
        btnLoyola = findViewById(R.id.btnLoyola);
        fabCurrentLocation = findViewById(R.id.fabCurrentLocation);
        fabBuildingInfo = findViewById(R.id.fabBuildingInfo);
        tvStatus = findViewById(R.id.tvStatus);

        updateStatus("UI Components Ready");

        // Set up button listeners
        if (btnSGW != null) {
            btnSGW.setOnClickListener(v -> {
                Log.d("MapsActivity", "SGW button clicked");
                switchToCampus(CampusData.CAMPUS_SGW);
                updateButtonStates(true, false);
            });
        }

        if (btnLoyola != null) {
            btnLoyola.setOnClickListener(v -> {
                Log.d("MapsActivity", "Loyola button clicked");
                switchToCampus(CampusData.CAMPUS_LOYOLA);
                updateButtonStates(false, true);
            });
        }

        if (fabCurrentLocation != null) {
            fabCurrentLocation.setOnClickListener(v -> {
                Log.d("MapsActivity", "Current location button clicked");
                showCurrentLocation();
            });
        }

        if (fabBuildingInfo != null) {
            fabBuildingInfo.setOnClickListener(v -> {
                Log.d("MapsActivity", "Building info button clicked");
                showBuildingDetails();
            });
        }

        // Set initial button states
        updateButtonStates(true, false);
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

    private void hideStatus() {
        if (tvStatus != null) {
            runOnUiThread(() -> tvStatus.setVisibility(View.GONE));
        }
    }

    private void updateButtonStates(boolean sgwSelected, boolean loyolaSelected) {
        if (btnSGW != null) {
            btnSGW.setEnabled(!sgwSelected);
        }
        if (btnLoyola != null) {
            btnLoyola.setEnabled(!loyolaSelected);
        }
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
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        android.util.Log.e("MapsActivity", "=== onMapReady called ===");
        mMap = googleMap;

        try {
            // Use LITE mode for better emulator compatibility
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            LatLng montreal = new LatLng(45.5017, -73.5673);
            LatLng sgwCampus = new LatLng(45.496107243097704, -73.57725834380621);

            // Add markers
            mMap.addMarker(new MarkerOptions()
                    .position(montreal)
                    .title("Montreal")
                    .snippet("Test location"));

            mMap.addMarker(new MarkerOptions()
                    .position(sgwCampus)
                    .title("SGW Campus")
                    .snippet("Concordia University"));

            // Move camera
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(montreal, 12f));
            android.util.Log.e("MapsActivity", "✅ Map setup complete - markers added");
            Toast.makeText(this, "✅ Map loaded - tap markers to see info", Toast.LENGTH_LONG).show();

            new android.os.Handler().postDelayed(() -> {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sgwCampus, 16f));
            }, 4000);

            enableMyLocationIfPermitted();
            switchToCampus(CampusData.CAMPUS_SGW);

        } catch (Exception e) {
            android.util.Log.e("MapsActivity", "Error: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void switchToCampus(String campusCode) {
        if (mMap == null) return;

        currentCampus = campusCode;
        selectedBuilding = null;

        if (fabBuildingInfo != null) {
            fabBuildingInfo.setVisibility(View.GONE);
        }

        // Clear existing buildings
        clearBuildingPolygons();

        // Load campus data
        LatLng campusCenter;
        List<CampusData.Building> buildings;

        if (campusCode.equals(CampusData.CAMPUS_SGW)) {
            campusCenter = CampusData.SGW_CENTER;
            buildings = CampusData.getSGWBuildings();
        } else {
            campusCenter = CampusData.LOYOLA_CENTER;
            buildings = CampusData.getLoyolaBuildings();
        }

        // Move camera to campus
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 17f));

        // Add building polygons
        addBuildingPolygons(buildings);
        currentBuildings = buildings;
    }

    private void addBuildingPolygons(List<CampusData.Building> buildings) {
        buildingPolygons.clear();

        for (CampusData.Building building : buildings) {
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(building.getPolygonCoordinates())
                    .strokeColor(Color.parseColor("#1976D2"))
                    .strokeWidth(3f)
                    .fillColor(Color.parseColor("#331976D2"))
                    .clickable(true);

            Polygon polygon = mMap.addPolygon(polygonOptions);
            buildingPolygons.add(polygon);
        }
    }

    private void clearBuildingPolygons() {
        for (Polygon polygon : buildingPolygons) {
            polygon.remove();
        }
        buildingPolygons.clear();
    }

    private void enableMyLocationIfPermitted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void showCurrentLocation() {
        updateStatus("Getting current location...");
        // Location functionality would go here
        Toast.makeText(this, "Current location feature - implementation pending", Toast.LENGTH_SHORT).show();
    }

    private void showBuildingDetails() {
        if (selectedBuilding == null) {
            Toast.makeText(this, "No building selected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(selectedBuilding.getName())
                .setMessage("Building Code: " + selectedBuilding.getBuildingCode() +
                           "\nCampus: " + selectedBuilding.getCampusCode())
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .show();
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "(not available)" : s;
    }
}
