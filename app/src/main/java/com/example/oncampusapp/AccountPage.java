package com.example.oncampusapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
    private BottomNavigationView bottomNav;
    private Button btnRefresh;
    private Button btnOpenCalendar;
    private CalendarRepository repository;
    private String email;
    private LinearLayout calendarContainer;
    private android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        email = getIntent().getStringExtra("email");

        // US-3.3 FIX: Pull from global variable instead of Intent to avoid crashes
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

        btnOpenCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleViewer.class);
            // US-3.3 FIX: We don't put eventsJson in the intent anymore!
            startActivity(intent);
        });

        btnRefresh.setOnClickListener(v -> {
            showConnectDialog();
        });

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                refreshBannerUI();
                timerHandler.postDelayed(this, 1000); // Run this exact block again in 1000ms (1 second)
            }
        };

        timerHandler.post(timerRunnable);
    }

    private void setViews() {
        bottomNav = findViewById(R.id.bottom_nav);
        calendarContainer = findViewById(R.id.calendarListContainer);
        btnOpenCalendar = findViewById(R.id.btnOpenCalendar);
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
            } else if (id == R.id.nav_account) {
                return true;
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    public void setRefreshButton() {
        btnRefresh.setOnClickListener(v -> {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            btnRefresh.setEnabled(false);

            new Thread(() -> {
                try {
                    String accessToken = GoogleAuthUtil.getToken(
                            this,
                            account.getAccount(),
                            "oauth2:https://www.googleapis.com/auth/calendar.readonly"
                    );

                    JSONArray events = repository.fetchAllEvents(accessToken);
                    eventsJson = events.toString();

                    // US-3.3 FIX: Update the global variable so the rest of the app sees the refresh
                    CalendarEventManager.globalEventsJson = eventsJson;

                    runOnUiThread(() -> {
                        btnRefresh.setEnabled(true);
                        Toast.makeText(this, "Calendar updated", Toast.LENGTH_SHORT).show();
                        // Also refresh banner after manual sync
                        refreshBannerUI();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        btnRefresh.setEnabled(true);
                        Toast.makeText(this, "Refresh failed", Toast.LENGTH_SHORT).show();
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

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            TextView txtName = dialog.findViewById(R.id.txt_account_name);
            TextView txtEmail = dialog.findViewById(R.id.txt_account_email);
            TextView txtAvatar = dialog.findViewById(R.id.txt_avatar);

            txtName.setText(account.getDisplayName());
            txtEmail.setText(account.getEmail());

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