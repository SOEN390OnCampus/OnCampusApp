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
                long now           = System.currentTimeMillis();
                String startStr    = nextClass.getJSONObject("start").getString("dateTime");
                String endStr      = nextClass.getJSONObject("end").getString("dateTime");
                String rawLocation = nextClass.optString("location", "");
                String description = nextClass.optString("description", "");

                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                long startTime = fmt.parse(startStr).getTime();
                long endTime   = fmt.parse(endStr).getTime();

                if (now < startTime && (startTime - now) > 60 * 60 * 1000L) {
                    bannerView.setVisibility(View.GONE);
                    return;
                }

                bannerView.setVisibility(View.VISIBLE);
                wireBannerClickListeners(bannerView, nextClass);
                titleView.setText(title);

                TextView timeStatusView = bannerView.findViewById(R.id.banner_time_status);
                TextView onlineTagView  = bannerView.findViewById(R.id.banner_online_tag);
                applyTimeStatusView(timeStatusView, detailsView, now, startTime, endTime);

                String parsedLocation = LocationParser.parseSmartLocation(activity, title, rawLocation, description);
                applyLocationText(detailsView, onlineTagView, parsedLocation, rawLocation, description);

            } catch (Exception e) {
                Log.e("EventBannerManager", "Failed to parse event for banner", e);
                titleView.setText("Next: " + title);
                detailsView.setText("Check schedule for details");
            }
        } else if (bannerView != null) {
            bannerView.setVisibility(View.GONE);
        }
    }

    private void wireBannerClickListeners(View bannerView, JSONObject eventRef) {
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
    }

    private void applyTimeStatusView(TextView timeStatusView, TextView detailsView,
                                     long now, long startTime, long endTime) {
        if (timeStatusView == null) return;
        int redColor  = Color.parseColor("#8B1E2D");
        int greyColor = Color.parseColor("#808080");
        timeStatusView.setText(NotificationTimeFormatter.getBannerTimeStatus(now, startTime, endTime));
        timeStatusView.setTextColor(redColor);
        try {
            applyTintedDrawable(timeStatusView, android.R.drawable.ic_menu_recent_history, redColor);
            applyTintedDrawable(detailsView,    android.R.drawable.ic_menu_mylocation,     greyColor);
        } catch (Exception e) {
            Log.e("EventBannerManager", "Failed to set banner icons", e);
        }
    }

    private void applyTintedDrawable(TextView view, int drawableRes, int color) {
        Drawable icon = ContextCompat.getDrawable(activity, drawableRes);
        if (icon == null) return;
        icon = DrawableCompat.wrap(icon).mutate();
        DrawableCompat.setTint(icon, color);
        int size = (int) (16 * activity.getResources().getDisplayMetrics().density);
        icon.setBounds(0, 0, size, size);
        view.setCompoundDrawables(icon, null, null, null);
        view.setCompoundDrawablePadding(16);
    }

    private void applyLocationText(TextView detailsView, TextView onlineTagView,
                                   String parsedLocation, String rawLocation, String description) {
        if (parsedLocation.equals("Online")) {
            if (onlineTagView != null) onlineTagView.setVisibility(View.VISIBLE);
            detailsView.setText(resolveOnlineLabel(rawLocation, description));
        } else {
            if (onlineTagView != null) onlineTagView.setVisibility(View.GONE);
            detailsView.setText(parsedLocation.equals("TBD") && !rawLocation.isEmpty()
                    ? rawLocation : parsedLocation);
        }
    }

    private static String resolveOnlineLabel(String rawLocation, String description) {
        String search = (rawLocation + " " + description).toLowerCase();
        if (search.contains("zoom"))        return "ZOOM MEETING";
        if (search.contains("teams"))       return "MICROSOFT TEAMS";
        if (search.contains("meet.google")) return "GOOGLE MEET";
        return rawLocation.isEmpty() ? "ONLINE CLASS" : rawLocation.toUpperCase();
    }
}
