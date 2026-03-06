package com.example.oncampusapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.json.JSONObject;

public class AccountPage extends AppCompatActivity {

    private ImageView backButton;

    // 1. The Timer Variables (Auto-Refresh)
    private final android.os.Handler bannerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            refreshBannerUI();
            bannerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_page);

        String email = getIntent().getStringExtra("email");
        String eventsJson = CalendarEventManager.globalEventsJson;

        // --- Find the next class and schedule alarm ---
        if (eventsJson != null && !eventsJson.isEmpty()) {
            JSONObject nextClass = CalendarEventManager.findNextUpcomingEvent(eventsJson);

            if (nextClass != null) {
                // Requirement 2: Notification
                NotificationScheduler.scheduleClassNotification(this, nextClass);
            }
        }

        // 2. Call the new smart banner logic once on startup
        refreshBannerUI();

        backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountPage.this, MapsActivity.class);
            startActivity(intent);
        });

        Button btnOpenCalendar = findViewById(R.id.btnOpenCalendar);
        TextView txtUserEmail = findViewById(R.id.txtUserEmail);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            txtUserEmail.setText("Logged in as:\n" + account.getEmail());
        } else {
            txtUserEmail.setText("Not signed in");
        }

        btnOpenCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleViewer.class);
            startActivity(intent);
        });
    }

    // 3. Lifecycle methods to start and stop the timer safely
    @Override
    protected void onResume() {
        super.onResume();
        bannerHandler.post(bannerRunnable); // Start the timer
    }

    @Override
    protected void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable); // Stop the timer
    }

    /**
     * Requirement 5: Populates the top banner and updates based on real-time.
     */
    private void refreshBannerUI() {
        String eventsJson = CalendarEventManager.globalEventsJson;
        if (eventsJson == null || eventsJson.isEmpty()) return;

        JSONObject nextClass = CalendarEventManager.findNextUpcomingEvent(eventsJson);
        View bannerView = findViewById(R.id.included_banner);

        if (nextClass != null && bannerView != null) {
            TextView titleView = bannerView.findViewById(R.id.banner_event_title);
            TextView detailsView = bannerView.findViewById(R.id.banner_event_details);

            if (titleView == null || detailsView == null) {
                android.util.Log.e("BannerCrash", "Missing TextViews in XML!");
                return;
            }

            String title = nextClass.optString("summary", "Class");

            try {
                long now = System.currentTimeMillis();
                String startStr = nextClass.getJSONObject("start").getString("dateTime");
                String endStr = nextClass.getJSONObject("end").getString("dateTime");
                String rawLocation = nextClass.optString("location", "");
                String description = nextClass.optString("description", "");

                java.text.SimpleDateFormat exactTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault());
                long startTime = exactTimeFormat.parse(startStr).getTime();
                long endTime = exactTimeFormat.parse(endStr).getTime();

                long sixtyMinutesInMillis = 60 * 60 * 1000;
                if (now < startTime && (startTime - now) > sixtyMinutesInMillis) {
                    bannerView.setVisibility(View.GONE);
                    return;
                }
                bannerView.setVisibility(View.VISIBLE);

                TextView timeStatusView = bannerView.findViewById(R.id.banner_time_status);
                TextView onlineTagView = bannerView.findViewById(R.id.banner_online_tag);

                titleView.setText(title);

                // --- 1. SET COLORS AND ICON SIZE (16dp) ---
                int redColor = Color.parseColor("#8B1E2D");
                int greyColor = Color.parseColor("#808080");
                int iconSizePx = (int) (16 * getResources().getDisplayMetrics().density);

                // --- 2. TIME STATUS LOGIC ---
                String timeStatus = NotificationTimeFormatter.getBannerTimeStatus(now, startTime, endTime);
                timeStatusView.setText(timeStatus);
                timeStatusView.setTextColor(redColor);

                // Inject Scaled Red Clock Icon
                android.graphics.drawable.Drawable clockIcon = androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_menu_recent_history);
                if (clockIcon != null) {
                    clockIcon = androidx.core.graphics.drawable.DrawableCompat.wrap(clockIcon).mutate();
                    androidx.core.graphics.drawable.DrawableCompat.setTint(clockIcon, redColor);
                    clockIcon.setBounds(0, 0, iconSizePx, iconSizePx);
                    timeStatusView.setCompoundDrawables(clockIcon, null, null, null);
                    timeStatusView.setCompoundDrawablePadding(16);
                }

                // --- 3. LOCATION LOGIC ---
                String parsedLocation = LocationParser.parseSmartLocation(title, rawLocation, description);

                // Inject Scaled Grey Pin Icon
                android.graphics.drawable.Drawable targetIcon = androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation);
                if (targetIcon != null) {
                    targetIcon = androidx.core.graphics.drawable.DrawableCompat.wrap(targetIcon).mutate();
                    androidx.core.graphics.drawable.DrawableCompat.setTint(targetIcon, greyColor);
                    targetIcon.setBounds(0, 0, iconSizePx, iconSizePx);
                    detailsView.setCompoundDrawables(targetIcon, null, null, null);
                    detailsView.setCompoundDrawablePadding(16);
                }

                if (parsedLocation.equals("Online")) {
                    onlineTagView.setVisibility(View.VISIBLE);
                    String searchString = (rawLocation + " " + description).toLowerCase();
                    if (searchString.contains("zoom")) detailsView.setText("ZOOM MEETING");
                    else if (searchString.contains("teams")) detailsView.setText("MICROSOFT TEAMS");
                    else if (searchString.contains("meet.google")) detailsView.setText("GOOGLE MEET");
                    else detailsView.setText(rawLocation.isEmpty() ? "ONLINE CLASS" : rawLocation.toUpperCase());
                } else {
                    onlineTagView.setVisibility(View.GONE);
                    detailsView.setText(parsedLocation.equals("TBD") && !rawLocation.isEmpty() ? rawLocation : parsedLocation);
                }

            } catch (Exception e) {
                e.printStackTrace();
                titleView.setText(title);
                detailsView.setText("Check schedule for details");
            }
        } else if (bannerView != null) {
            bannerView.setVisibility(View.GONE);
        }
    }
}