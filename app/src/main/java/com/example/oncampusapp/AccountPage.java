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

import java.util.concurrent.TimeUnit;

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

            // --- THE CRASH FAILSAFE ---
            // If the text fields are missing from the XML, stop here!
            if (titleView == null || detailsView == null) {
                android.util.Log.e("BannerCrash", "Missing TextViews in XML!");
                return;
            }

            bannerView.setVisibility(View.VISIBLE);
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

                // Find views in XML
                TextView timeStatusView = bannerView.findViewById(R.id.banner_time_status);
                TextView onlineTagView = bannerView.findViewById(R.id.banner_online_tag);

                // Set the main title
                titleView.setText(title);

                // Dynamic Time and Placement Logic
                if (now >= startTime && now <= endTime) {
                    timeStatusView.setText("Class is ongoing");

                } else if (now < startTime) {
                    long diffInMillis = startTime - now;

                    long diffInMins = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
                    long diffInSecs = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60;

                    // Condition to switch from minutes to seconds when very close
                    if (diffInMins < 1) {
                        // At 0min, transform to seconds count
                        timeStatusView.setText("Next class starting in " + diffInSecs + " secs");
                    } else {
                        // General minute countdown
                        timeStatusView.setText("Next class starting in " + diffInMins + " mins");
                    }
                    timeStatusView.setTextColor(Color.parseColor("#8B1E2D"));
                }

                // Identify and format online vs. physical location
                String searchString = (rawLocation + " " + description).toLowerCase();

                // Check if any online keywords exist
                if (searchString.contains("zoom") || searchString.contains("teams") ||
                        searchString.contains("online") || searchString.contains("meet.google")) {

                    onlineTagView.setVisibility(View.VISIBLE);

                    // Set the bottom location pin text based on the specific platform
                    if (searchString.contains("zoom")) {
                        detailsView.setText("ZOOM MEETING");
                    } else if (searchString.contains("teams")) {
                        detailsView.setText("MICROSOFT TEAMS");
                    } else if (searchString.contains("meet.google")) {
                        detailsView.setText("GOOGLE MEET");
                    } else {
                        // Fallback if it just says "online" somewhere
                        detailsView.setText(rawLocation.isEmpty() ? "ONLINE CLASS" : rawLocation.toUpperCase());
                    }

                } else {
                    // It's a physical class
                    onlineTagView.setVisibility(View.GONE);
                    detailsView.setText(rawLocation.isEmpty() ? "Check schedule for details" : rawLocation);
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