package com.example.oncampusapp;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.oncampusapp.navigation.RouteTravelMode;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

/**
 * Owns all route-picker UI: view references, click listeners,
 * transport-tab logic, Go/End-Trip behaviour, and navigation UI toggling.
 * Previously all of this lived inside MapsActivity.setupRoutePickerUi()
 * and MapsActivity.toggleNavigationUI().
 */
public class RoutePickerController {

    private final MapsActivity activity;
    private final RouteManager routeManager;
    private final IndoorNavigationController indoorNavController;
    private final EventBannerManager bannerManager;

    // View references that must be accessible across multiple methods
    private AutoCompleteTextView startDestinationText;
    private AutoCompleteTextView endDestinationText;
    private LinearLayout routePicker;
    private GoogleMap mMap;

    public RoutePickerController(MapsActivity activity,
                                  RouteManager routeManager,
                                  IndoorNavigationController indoorNavController,
                                  EventBannerManager bannerManager) {
        this.activity = activity;
        this.routeManager = routeManager;
        this.indoorNavController = indoorNavController;
        this.bannerManager = bannerManager;
    }

    /** Called from onMapReady once the map is available. */
    public void setMap(GoogleMap map) {
        this.mMap = map;
    }

    /** Wires the shared suggestions adapter into both search fields. */
    public void setSearchAdapter(ArrayAdapter<String> adapter) {
        startDestinationText.setAdapter(adapter);
        endDestinationText.setAdapter(adapter);
    }

    /**
     * Opens the route picker pre-filled with start and destination, then
     * kicks off a route preview. Called from notification/banner directions flow.
     */
    public void openWithStartAndDestination(String start, String destination) {
        View bannerView = activity.findViewById(R.id.included_banner);
        if (bannerView != null) bannerView.setVisibility(View.GONE);

        if (startDestinationText == null || endDestinationText == null) return;
        startDestinationText.setText(start);
        endDestinationText.setText(destination);

        View searchBar = activity.findViewById(R.id.search_bar_container);
        if (searchBar != null) searchBar.setVisibility(View.GONE);
        if (routePicker != null) routePicker.setVisibility(View.VISIBLE);

        bannerManager.setRoutePickerOpen(true);
        routeManager.initiateRoutePreview(start, destination);
    }

    /**
     * If the route picker is visible and a search field has focus, fills that
     * field with {@code name} and returns {@code true}.  Returns {@code false}
     * otherwise so the caller can fall back to showing the building-info dialog.
     */
    public boolean tryFillFocusedField(String name) {
        if (routePicker == null || routePicker.getVisibility() != View.VISIBLE) return false;
        if (startDestinationText != null && startDestinationText.hasFocus()) {
            startDestinationText.setText(name);
            startDestinationText.dismissDropDown();
            return true;
        }
        if (endDestinationText != null && endDestinationText.hasFocus()) {
            endDestinationText.setText(name);
            endDestinationText.dismissDropDown();
            return true;
        }
        return false;
    }

    /** Inflates and wires all route-picker views. Call once from onCreate(). */
    public void setup() {
        // ── View initialisation ──────────────────────────────────────────────
        CardView searchBar              = activity.findViewById(R.id.search_bar_container);
        routePicker                     = activity.findViewById(R.id.route_picker_container);
        startDestinationText            = activity.findViewById(R.id.et_start);
        endDestinationText              = activity.findViewById(R.id.et_destination);
        ImageView currentLocationIcon   = activity.findViewById(R.id.currentLocationIcon);
        ImageButton btnSwapAddress      = activity.findViewById(R.id.btn_swap_address);

        LinearLayout layoutInputs       = activity.findViewById(R.id.layout_inputs);
        LinearLayout layoutTabs         = activity.findViewById(R.id.layout_tabs);
        LinearLayout layoutNavActive    = activity.findViewById(R.id.layout_navigation_active);

        ConstraintLayout dirLayout      = activity.findViewById(R.id.dirLayout);
        ImageButton prevDirBtn          = activity.findViewById(R.id.prevDirBtn);
        ImageButton nextDirBtn          = activity.findViewById(R.id.nextDirBtn);

        Button btnGo                    = activity.findViewById(R.id.btn_go);
        Button btnEndTrip               = activity.findViewById(R.id.btn_end_trip);
        TextView txtNavInstruction      = activity.findViewById(R.id.txt_nav_instruction);
        TextView txtDuration            = activity.findViewById(R.id.txt_duration);

        ImageButton btnWalk             = activity.findViewById(R.id.btn_mode_walking);
        ImageButton btnCar              = activity.findViewById(R.id.btn_mode_driving);
        ImageButton btnTransit          = activity.findViewById(R.id.btn_mode_transit);
        View btnShuttle                 = activity.findViewById(R.id.btn_mode_shuttle);
        List<View> transportTabs        = Arrays.asList(btnWalk, btnCar, btnTransit, btnShuttle);

        Animation slideDown = AnimationUtils.loadAnimation(activity, R.anim.slide_down);
        Animation slideUp   = AnimationUtils.loadAnimation(activity, R.anim.slide_up);

        // ── Window-inset listeners ───────────────────────────────────────────
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

        // ── Close-route-picker runnable ──────────────────────────────────────
        Runnable closeRoutePicker = () -> {
            slideUp.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {
                    routePicker.setVisibility(View.GONE);
                    searchBar.setVisibility(View.VISIBLE);
                    slideUp.setAnimationListener(null);
                    bannerManager.setRoutePickerOpen(false);
                    bannerManager.checkAndDisplayNextEventBanner();
                }
                @Override public void onAnimationRepeat(Animation animation) {}
            });
            routePicker.startAnimation(slideUp);
        };

        // ── Search bar click ─────────────────────────────────────────────────
        searchBar.setOnClickListener(v -> {
            bannerManager.setRoutePickerOpen(true);

            View bannerView = activity.findViewById(R.id.included_banner);
            if (bannerView != null) bannerView.setVisibility(View.GONE);

            searchBar.setVisibility(View.GONE);
            routePicker.setVisibility(View.VISIBLE);
            routePicker.startAnimation(slideDown);

            layoutInputs.setVisibility(View.VISIBLE);
            layoutTabs.setVisibility(View.VISIBLE);
            btnGo.setVisibility(View.VISIBLE);
            layoutNavActive.setVisibility(View.GONE);
            currentLocationIcon.setVisibility(View.VISIBLE);

            startDestinationText.setFocusableInTouchMode(true);
            startDestinationText.requestFocus();
            startDestinationText.post(() -> {
                InputMethodManager imm = (InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.showSoftInput(startDestinationText, InputMethodManager.SHOW_IMPLICIT);
            });
        });

        // ── Auto-preview on suggestion selection ─────────────────────────────
        startDestinationText.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            startDestinationText.setText(selected);
            startDestinationText.setSelection(selected.length());
            routeManager.initiateRoutePreview(
                    startDestinationText.getText().toString().trim(),
                    endDestinationText.getText().toString().trim());
        });
        endDestinationText.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            endDestinationText.setText(selected);
            endDestinationText.setSelection(selected.length());
            routeManager.initiateRoutePreview(
                    startDestinationText.getText().toString().trim(),
                    endDestinationText.getText().toString().trim());
        });

        // ── Transport tab highlight logic ────────────────────────────────────
        View.OnClickListener tabHighlight = v -> {
            v.setBackgroundColor(Color.parseColor("#D3D3D3"));
            v.setAlpha(1.0f);
            for (View tab : transportTabs) {
                if (tab != v) {
                    tab.setBackgroundResource(0);
                    tab.setAlpha(0.5f);
                }
            }
        };

        Button btnShuttleTimetable = activity.findViewById(R.id.btn_shuttle_timetable);
        btnShuttleTimetable.setOnClickListener(v -> ShuttleHelper.openTimetable(activity));

        routeManager.setTransportButtons(btnWalk, btnShuttleTimetable, btnGo);

        btnWalk.setOnClickListener(v -> {
            tabHighlight.onClick(v);
            routeManager.setSelectedMode(RouteTravelMode.WALK);
            btnShuttleTimetable.setVisibility(View.GONE);
            routeManager.adjustGoButtonWidth(btnGo, false);
            ShuttleHelper.hideShuttleStops(routeManager.getShuttleMarkers());
            routeManager.clearShuttleRoute();
            routeManager.initiateRoutePreview(
                    startDestinationText.getText().toString().trim(),
                    endDestinationText.getText().toString().trim());
        });
        btnCar.setOnClickListener(v -> {
            tabHighlight.onClick(v);
            routeManager.setSelectedMode(RouteTravelMode.DRIVE);
            btnShuttleTimetable.setVisibility(View.GONE);
            routeManager.adjustGoButtonWidth(btnGo, false);
            ShuttleHelper.hideShuttleStops(routeManager.getShuttleMarkers());
            routeManager.clearShuttleRoute();
            routeManager.initiateRoutePreview(
                    startDestinationText.getText().toString().trim(),
                    endDestinationText.getText().toString().trim());
        });
        btnTransit.setOnClickListener(v -> {
            tabHighlight.onClick(v);
            routeManager.setSelectedMode(RouteTravelMode.TRANSIT);
            btnShuttleTimetable.setVisibility(View.GONE);
            routeManager.adjustGoButtonWidth(btnGo, false);
            ShuttleHelper.hideShuttleStops(routeManager.getShuttleMarkers());
            routeManager.clearShuttleRoute();
            routeManager.initiateRoutePreview(
                    startDestinationText.getText().toString().trim(),
                    endDestinationText.getText().toString().trim());
        });
        btnShuttle.setOnClickListener(v -> {
            tabHighlight.onClick(v);
            routeManager.setSelectedMode(RouteTravelMode.SHUTTLE);
            btnShuttleTimetable.setVisibility(View.VISIBLE);
            routeManager.adjustGoButtonWidth(btnGo, true);
            routeManager.clearNormalRoute();
            routeManager.setShuttleMarkers(
                    ShuttleHelper.showShuttleStops(activity, mMap, routeManager.getShuttleMarkers()));
            routeManager.initiateRoutePreview(
                    startDestinationText.getText().toString().trim(),
                    endDestinationText.getText().toString().trim());
        });

        // ── Helper buttons ───────────────────────────────────────────────────
        btnSwapAddress.setOnClickListener(v -> {
            swapAddresses();
            routeManager.initiateRoutePreview(
                    startDestinationText.getText().toString().trim(),
                    endDestinationText.getText().toString().trim());
        });
        currentLocationIcon.setOnClickListener(v -> setCurrentBuilding());

        // ── Go button ────────────────────────────────────────────────────────
        btnGo.setOnClickListener(v -> {
            String startText = startDestinationText.getText().toString().trim();
            String destText  = endDestinationText.getText().toString().trim();

            if (startText.isEmpty() || destText.isEmpty()) {
                Toast.makeText(activity, "Please enter both locations", Toast.LENGTH_SHORT).show();
                return;
            }

            IndoorNode fromRoom = indoorNavController.getIndoorRoomMap().get(startText);
            IndoorNode toRoom   = indoorNavController.getIndoorRoomMap().get(destText);

            if (fromRoom != null && toRoom != null) {
                indoorNavController.launchIndoorRoute(fromRoom, toRoom);
                return;
            }
            if (fromRoom != null || toRoom != null) {
                Toast.makeText(activity,
                        "Mix of indoor room and outdoor location is not supported.\n"
                                + "Please enter two rooms (e.g. H-867) or two buildings.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            LatLng startCoords = BuildingLookup.getLatLngFromBuildingName(startText, MapsActivity.buildingsMap);
            LatLng destCoords  = BuildingLookup.getLatLngFromBuildingName(destText,  MapsActivity.buildingsMap);
            if (startCoords != null && destCoords != null
                    && routeManager.applySameCampusCheck(startCoords, destCoords)) {
                routeManager.initiateRoutePreview(startText, destText);
                return;
            }

            if (routeManager.getSelectedMode() != RouteTravelMode.SHUTTLE
                    && !routeManager.isPreviewActive()) {
                Toast.makeText(activity, "Calculating route, please wait...", Toast.LENGTH_SHORT).show();
                routeManager.initiateRoutePreview(startText, destText);
                return;
            }

            routeManager.removeStartDot();
            routeManager.startNavigationUpdates();
            Toast.makeText(activity, "Navigation Started", Toast.LENGTH_SHORT).show();
            toggleNavigationUI(true);

            if (txtDuration != null && txtDuration.getText().length() > 0
                    && !txtDuration.getText().equals("-- MIN")) {
                txtNavInstruction.setText(
                        txtDuration.getText() + " (" + routeManager.getSelectedMode().getValue() + ")");
            } else {
                txtNavInstruction.setText(
                        "Follow the route (" + routeManager.getSelectedMode().getValue() + ")");
            }

            LatLng cameraTarget = routeManager.getSelectedMode() == RouteTravelMode.SHUTTLE
                    ? BuildingLookup.getLatLngFromBuildingName(startText, MapsActivity.buildingsMap)
                    : routeManager.getFirstRoutePoint();
            if (cameraTarget != null && mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(cameraTarget).zoom(19f).tilt(0).build()));
            }
        });

        nextDirBtn.setOnClickListener(v -> routeManager.navigateToNextDirection());
        prevDirBtn.setOnClickListener(v -> routeManager.navigateToPreviousDirection());

        // ── End Trip button ──────────────────────────────────────────────────
        btnEndTrip.setOnClickListener(v -> {
            routeManager.stopNavigation();
            toggleNavigationUI(false);
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder(mMap.getCameraPosition())
                                .tilt(0).zoom(16f).build()));
            }
        });

        // ── Back-press handler ───────────────────────────────────────────────
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (routePicker.getVisibility() == View.VISIBLE) {
                    if (layoutNavActive.getVisibility() == View.VISIBLE) {
                        btnEndTrip.performClick();
                        return;
                    }
                    closeRoutePicker.run();
                    startDestinationText.setText("");
                    endDestinationText.setText("");
                    if (txtDuration != null) txtDuration.setText("");
                    routeManager.resetRouteState();
                } else {
                    setEnabled(false);
                    activity.getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ── Navigation UI toggling ───────────────────────────────────────────────

    void toggleNavigationUI(boolean isNavigating) {
        LinearLayout layoutInputs    = activity.findViewById(R.id.layout_inputs);
        LinearLayout layoutTabs      = activity.findViewById(R.id.layout_tabs);
        Button btnGo                 = activity.findViewById(R.id.btn_go);
        LinearLayout layoutNavActive = activity.findViewById(R.id.layout_navigation_active);
        ConstraintLayout dirLayout   = activity.findViewById(R.id.dirLayout);
        ImageButton prevDirBtn       = activity.findViewById(R.id.prevDirBtn);
        ImageButton nextDirBtn       = activity.findViewById(R.id.nextDirBtn);
        TextView textDir             = activity.findViewById(R.id.textDir);
        FrameLayout closeSearchLayout= activity.findViewById(R.id.close_search);

        if (isNavigating) {
            layoutInputs.setVisibility(View.GONE);
            layoutTabs.setVisibility(View.GONE);
            btnGo.setVisibility(View.GONE);
            layoutNavActive.setVisibility(View.VISIBLE);
            if (routeManager.getSelectedMode() != RouteTravelMode.SHUTTLE) {
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

    // ── Private helpers ──────────────────────────────────────────────────────

    private void swapAddresses() {
        String startVal = startDestinationText.getText().toString();
        String endVal   = endDestinationText.getText().toString();
        startDestinationText.setText(endVal);
        endDestinationText.setText(startVal);
        startDestinationText.clearFocus();
        endDestinationText.clearFocus();
    }

    private void setCurrentBuilding() {
        Building currentBuilding = activity.buildingManager.getCurrentBuilding();
        if (currentBuilding != null) {
            startDestinationText.setText(currentBuilding.getName());
        }
    }
}
