package com.example.oncampusapp;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Manages the "next event" persistent top banner on the map screen.
 * Owns the polling Handler/Runnable and all banner display logic,
 * previously spread across MapsActivity.
 */
public class EventBannerManager {

    private final MapsActivity activity;
    private boolean isRoutePickerOpen = false;

    private final Handler bannerHandler = new Handler();
    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndDisplayNextEventBanner();
            bannerHandler.postDelayed(this, 1000);
        }
    };

    public EventBannerManager(MapsActivity activity) {
        this.activity = activity;
    }

    /** Call from onResume() to start the polling loop. */
    public void start() {
        bannerHandler.post(bannerRunnable);
    }

    /** Call from onPause() to stop the polling loop. */
    public void stop() {
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    /** Must be updated whenever the route-picker opens or closes. */
    public void setRoutePickerOpen(boolean open) {
        this.isRoutePickerOpen = open;
    }

    /** Evaluates the next calendar event and refreshes the banner view. */
    public void checkAndDisplayNextEventBanner() {
        if (isRoutePickerOpen) return;

        String eventsJson = CalendarEventManager.globalEventsJson;
        if (eventsJson == null || eventsJson.isEmpty()) return;

        JSONObject nextClass = CalendarEventManager.findNextUpcomingEvent(eventsJson);
        View bannerView = activity.findViewById(R.id.included_banner);

        if (nextClass != null && bannerView != null) {

            TextView titleView   = bannerView.findViewById(R.id.banner_event_title);
            TextView detailsView = bannerView.findViewById(R.id.banner_event_details);

            if (titleView == null || detailsView == null) return;

            String title = nextClass.optString("summary", "Class");

            try {
                long now = System.currentTimeMillis();
                String startStr    = nextClass.getJSONObject("start").getString("dateTime");
                String endStr      = nextClass.getJSONObject("end").getString("dateTime");
                String rawLocation = nextClass.optString("location", "");
                String description = nextClass.optString("description", "");

                SimpleDateFormat exactTimeFormat = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                long startTime = exactTimeFormat.parse(startStr).getTime();
                long endTime   = exactTimeFormat.parse(endStr).getTime();

                // 60-MINUTE FILTER
                long sixtyMinutesInMillis = 60 * 60 * 1000;
                if (now < startTime && (startTime - now) > sixtyMinutesInMillis) {
                    bannerView.setVisibility(View.GONE);
                    return;
                }

                bannerView.setVisibility(View.VISIBLE);

                // Wire directions button click listeners
                final JSONObject eventRef = nextClass;
                bannerView.setOnClickListener(v -> activity.handleBannerDirectionsClick(eventRef));
                LinearLayout directionsContainer = bannerView.findViewById(R.id.banner_directions_container);
                if (directionsContainer != null) {
                    directionsContainer.setVisibility(View.VISIBLE);
                    directionsContainer.setOnClickListener(v -> activity.handleBannerDirectionsClick(eventRef));
                }
                ImageView goButton = bannerView.findViewById(R.id.banner_btn_go);
                if (goButton != null) {
                    goButton.setOnClickListener(v -> activity.handleBannerDirectionsClick(eventRef));
                }

                TextView timeStatusView = bannerView.findViewById(R.id.banner_time_status);
                TextView onlineTagView  = bannerView.findViewById(R.id.banner_online_tag);

                titleView.setText(title);

                if (timeStatusView != null) {
                    String timeStatus = NotificationTimeFormatter.getBannerTimeStatus(
                            now, startTime, endTime);
                    timeStatusView.setText(timeStatus);

                    int redColor  = Color.parseColor("#8B1E2D");
                    int greyColor = Color.parseColor("#808080");

                    timeStatusView.setTextColor(redColor);

                    try {
                        Drawable clockIcon = ContextCompat.getDrawable(
                                activity, android.R.drawable.ic_menu_recent_history);
                        if (clockIcon != null) {
                            clockIcon = DrawableCompat.wrap(clockIcon).mutate();
                            DrawableCompat.setTint(clockIcon, redColor);
                            int size = (int) (16 * activity.getResources().getDisplayMetrics().density);
                            clockIcon.setBounds(0, 0, size, size);
                            timeStatusView.setCompoundDrawables(clockIcon, null, null, null);
                            timeStatusView.setCompoundDrawablePadding(16);
                        }

                        Drawable targetIcon = ContextCompat.getDrawable(
                                activity, android.R.drawable.ic_menu_mylocation);
                        if (targetIcon != null) {
                            targetIcon = DrawableCompat.wrap(targetIcon).mutate();
                            DrawableCompat.setTint(targetIcon, greyColor);
                            int size = (int) (16 * activity.getResources().getDisplayMetrics().density);
                            targetIcon.setBounds(0, 0, size, size);
                            detailsView.setCompoundDrawables(targetIcon, null, null, null);
                            detailsView.setCompoundDrawablePadding(16);
                        }
                    } catch (Exception e) {
                        Log.e("EventBannerManager", "Failed to set banner icons", e);
                    }
                }

                String parsedLocation = LocationParser.parseSmartLocation(
                        activity, title, rawLocation, description);

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
                        detailsView.setText(rawLocation.isEmpty()
                                ? "ONLINE CLASS" : rawLocation.toUpperCase());
                    }
                } else {
                    if (onlineTagView != null) onlineTagView.setVisibility(View.GONE);
                    detailsView.setText(
                            parsedLocation.equals("TBD") && !rawLocation.isEmpty()
                                    ? rawLocation : parsedLocation);
                }

            } catch (Exception e) {
                Log.e("EventBannerManager", "Failed to parse event for banner", e);
                titleView.setText("Next: " + title);
                detailsView.setText("Check schedule for details");
            }
        } else if (bannerView != null) {
            bannerView.setVisibility(View.GONE);
        }
    }
}
