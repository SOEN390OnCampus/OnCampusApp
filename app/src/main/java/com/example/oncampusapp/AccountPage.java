package com.example.oncampusapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

public class AccountPage extends AppCompatActivity {

    private ImageView backButton;

    private String eventsJson;
    private String calendarListJson;
    private String calendarToken;

    private BottomNavigationView bottomNav;

    private Button btnRefresh;

    private CalendarRepository repository;

    private String email;

    private LinearLayout calendarContainer;

    // --- Timer Variables for the Live Banner ---
    private android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#7A1C1C"));

        email = getIntent().getStringExtra("email");

        calendarListJson = getIntent().getStringExtra("calendar_list_json");
        calendarToken = getIntent().getStringExtra("calendar_token");

        repository = CalendarRepository.getInstance();

        super.onCreate(savedInstanceState);

        // Pull from global variable instead of Intent to avoid crashes
        eventsJson = CalendarEventManager.globalEventsJson;

        // US-3.3 REQUIREMENT: Schedule the notification for the next class
        if (eventsJson != null && !eventsJson.isEmpty()) {
            try {
                JSONObject nextClass = CalendarEventManager.findNextUpcomingEvent(eventsJson);
                if (nextClass != null) {
                    NotificationScheduler.scheduleClassNotification(this, nextClass);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        repository = CalendarRepository.getInstance();

        setContentView(R.layout.account_page);
        setViews();
        setUpBottomNav();

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountPage.this, MapsActivity.class);
            startActivity(intent);
        });

        TextView txtUserEmail = findViewById(R.id.txtUserEmail);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if (account != null) {
            txtUserEmail.setText("Logged in as:\n" + account.getEmail());
        } else {
            txtUserEmail.setText("Not signed in");
        }

        btnRefresh.setOnClickListener(v -> {
            showConnectDialog();
        });

        // Populate the dynamic calendar list
        populateCalendarList();

        // --- Start the Live Ticker Loop ---
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                refreshBannerUI();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateCalendarList();
    }

    // --- Stop the timer when leaving to prevent crashes ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void setViews() {
        bottomNav = findViewById(R.id.bottom_nav);
        calendarContainer = findViewById(R.id.calendarListContainer);
        btnRefresh = findViewById(R.id.refreshCalendar);
        backButton = findViewById(R.id.btn_back);
    }

    private void setUpBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_account); // highlight account tab

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(AccountPage.this, MapsActivity.class);
                startActivity(intent);
                finish();
                return true;
            }

            else if (id == R.id.nav_account) {
                // Already on account page
                return true;
            }

            else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    public void setRefreshButton() {
        btnRefresh.setOnClickListener(v -> {

            GoogleSignInAccount account =
                    GoogleSignIn.getLastSignedInAccount(this);

            btnRefresh.setEnabled(false);

            new Thread(() -> {

                try {

                    String accessToken = GoogleAuthUtil.getToken(
                            this,
                            account.getAccount(),
                            "oauth2:https://www.googleapis.com/auth/calendar.readonly"
                    );

                    JSONArray events =
                            repository.fetchAllEvents(accessToken);

                    eventsJson = events.toString();

                    // Update global events JSON so the banner hydrates dynamically
                    CalendarEventManager.globalEventsJson = eventsJson;

                    runOnUiThread(() -> {
                        btnRefresh.setEnabled(true);
                        Toast.makeText(this,
                                "Calendar updated",
                                Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {

                    e.printStackTrace();

                    runOnUiThread(() -> {
                        btnRefresh.setEnabled(true);
                        Toast.makeText(this,
                                "Refresh failed",
                                Toast.LENGTH_SHORT).show();
                    });
                }

            }).start();
        });
    }

    private void showConnectDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_google_connect);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().getDecorView().setPadding(32,0,32,0);

        // Populate account info
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            TextView txtName = dialog.findViewById(R.id.txt_account_name);
            TextView txtEmail = dialog.findViewById(R.id.txt_account_email);
            TextView txtAvatar = dialog.findViewById(R.id.txt_avatar);

            txtName.setText(account.getDisplayName());
            txtEmail.setText(account.getEmail());

            // Set initials from name
            if (account.getDisplayName() != null) {
                String[] parts = account.getDisplayName().split(" ");
                String initials = parts.length >= 2
                        ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                        : String.valueOf(parts[0].charAt(0));
                txtAvatar.setText(initials.toUpperCase());
            }
        }

        dialog.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btn_allow).setOnClickListener(v -> {
            dialog.dismiss();
            setRefreshButton(); // triggers the actual calendar sync
            btnRefresh.performClick();
        });

        dialog.show();
    }

    private void populateCalendarList() {
        if (calendarListJson == null || calendarListJson.isEmpty()) {
            return;
        }

        // Get the selected calendar ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("OnCampusPrefs", MODE_PRIVATE);
        String selectedCalendarId = prefs.getString("selected_calendar", "");

        calendarContainer.removeAllViews(); // Clear any existing items just in case

        try {
            JSONObject root = new JSONObject(calendarListJson);
            JSONArray items = root.getJSONArray("items");

            TextView calendarCount = findViewById(R.id.calendar_count);
            calendarCount.setText("YOUR CALENDARS (" + items.length() + ")");

            for (int i = 0; i < items.length(); i++) {
                JSONObject calendar = items.getJSONObject(i);

                String id = calendar.getString("id");
                String summary = calendar.getString("summary");
                String bgColor = calendar.optString("backgroundColor", "#888888");

                // Inflate the item layout
                View itemView = getLayoutInflater().inflate(R.layout.item_calendar, calendarContainer, false);

                View dot = itemView.findViewById(R.id.calendar_color_dot);
                TextView name = itemView.findViewById(R.id.calendar_name);
                TextView badge = itemView.findViewById(R.id.calendar_selected_badge);

                name.setText(summary);

                // Dynamically color the dot
                GradientDrawable dotDrawable = (GradientDrawable) getResources().getDrawable(R.drawable.bg_circle, null).mutate();
                try {
                    dotDrawable.setColor(Color.parseColor(bgColor));
                } catch (IllegalArgumentException e) {
                    dotDrawable.setColor(Color.GRAY); // Fallback for invalid color strings
                }
                dot.setBackground(dotDrawable);

                // Show the "Selected" badge if the IDs match
                if (id.equals(selectedCalendarId)) {
                    badge.setVisibility(View.VISIBLE);
                } else {
                    badge.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> {
                    // Disable the clicked view so it can't be clicked again
                    v.setEnabled(false);

                    Intent intent = new Intent(this, ScheduleViewer.class);
                    intent.putExtra("calendar_token", calendarToken);
                    intent.putExtra("calendar_id", id);
                    intent.putExtra("calendar_name", summary);
                    intent.putExtra("calendar_color", bgColor);

                    v.setEnabled(true);

                    startActivity(intent);
                });

                // Add to the container
                calendarContainer.addView(itemView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading calendars", Toast.LENGTH_SHORT).show();
        }
    }

    // --- The Banner UI Math & Injection Logic ---
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

                int redColor = Color.parseColor("#8B1E2D");
                int greyColor = Color.parseColor("#808080");
                int iconSizePx = (int) (16 * getResources().getDisplayMetrics().density);

                String timeStatus = NotificationTimeFormatter.getBannerTimeStatus(now, startTime, endTime);
                timeStatusView.setText(timeStatus);
                timeStatusView.setTextColor(redColor);

                android.graphics.drawable.Drawable clockIcon = androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_menu_recent_history);
                if (clockIcon != null) {
                    clockIcon = androidx.core.graphics.drawable.DrawableCompat.wrap(clockIcon).mutate();
                    androidx.core.graphics.drawable.DrawableCompat.setTint(clockIcon, redColor);
                    clockIcon.setBounds(0, 0, iconSizePx, iconSizePx);
                    timeStatusView.setCompoundDrawables(clockIcon, null, null, null);
                    timeStatusView.setCompoundDrawablePadding(16);
                }

                // Pass context to LocationParser
                String parsedLocation = LocationParser.parseSmartLocation(this, title, rawLocation, description);

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