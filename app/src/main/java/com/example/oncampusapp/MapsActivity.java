package com.example.oncampusapp;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.oncampusapp.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private BuildingDetailsService buildingDetailsService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialize buildingDetailsService
        buildingDetailsService = new BuildingDetailsService(this);


        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // 45.496107243097704, -73.57725834380621
        // Add a marker in Sydney and move the camera
        LatLng SGW = new LatLng(45.496107243097704, -73.57725834380621);
        mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(SGW, 17f)
        );;
    // A marker to test interaction
        Marker sgwMarker = mMap.addMarker(
                new MarkerOptions()
                        .position(SGW)
                        .title("SGW Building")
        );
        if (sgwMarker != null) {
            // Tag MUST be a Google Place ID for BuildingDetailsService to work
            // TODO: we have to replace with real Google palce ID/ from API
            sgwMarker.setTag("PLACE_ID");
        }

        // Map interaction: clicking a building marker opens a popup and fetches building details
        mMap.setOnMarkerClickListener(marker -> {
            String placeId = (marker.getTag() != null) ? marker.getTag().toString() : null;

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(marker.getTitle())
                    .setMessage("Loading building info...")
                    .setPositiveButton("Close", (d, w) -> d.dismiss())
                    .show();


            //Safeguard to avoid dummy id while the popup calls the API. Remove later
            if (place == null || placeID.equals("PLACE_ID")){
                dialog.setMessage("Building details not configured yet.");
                return true;

            }
            buildingDetailsService.fetchBuildingDetails(placeId, new BuildingDetailsService.FetchBuildingDetailsCallback() {
                @Override
                public void onSuccess(BuildingDetailsDto dto) {
                    runOnUiThread(() -> {
                        dialog.setTitle(safe(dto.getName()));
                        dialog.setMessage(
                                "Address: " + safe(dto.getAddress()) + "\n\n" +
                                "Image URL: " + safe(dto.getImgUri())
                        );
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> dialog.setMessage("Failed to load building info.\n" + e.getMessage()));
                }
            });

            return true;
        });
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "(not available)" : s;
    }
}
