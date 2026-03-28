package com.example.oncampusapp;

import android.app.AlertDialog;
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
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

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

    private static final String CALENDAR_SCOPE =
            "https://www.googleapis.com/auth/calendar.readonly";



    private GoogleSignInClient googleSignInClient;


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

        configureSignOut();

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
                    CalendarEventManager.setGlobalEventsJson(eventsJson);

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
        if (bannerView == null) return;

        if (nextClass != null) {
            populateBanner(bannerView, nextClass);
        } else {
            bannerView.setVisibility(View.GONE);
        }
    }

    private void populateBanner(View bannerView, JSONObject nextClass) {
        TextView titleView = bannerView.findViewById(R.id.banner_event_title);
        TextView detailsView = bannerView.findViewById(R.id.banner_event_details);
        if (titleView == null || detailsView == null) {
            android.util.Log.e("BannerCrash", "Missing TextViews in XML!");
            return;
        }

        String title = nextClass.optString("summary", "Class");
        try {
            long now = System.currentTimeMillis();
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault());
            long startTime = fmt.parse(nextClass.getJSONObject("start").getString("dateTime")).getTime();
            long endTime = fmt.parse(nextClass.getJSONObject("end").getString("dateTime")).getTime();
            String rawLocation = nextClass.optString("location", "");
            String description = nextClass.optString("description", "");

            if (now < startTime && (startTime - now) > 60 * 60 * 1000L) {
                bannerView.setVisibility(View.GONE);
                return;
            }

            bannerView.setVisibility(View.VISIBLE);
            titleView.setText(title);

            int redColor = Color.parseColor("#8B1E2D");
            int greyColor = Color.parseColor("#808080");
            int iconSizePx = (int) (16 * getResources().getDisplayMetrics().density);

            TextView timeStatusView = bannerView.findViewById(R.id.banner_time_status);
            TextView onlineTagView = bannerView.findViewById(R.id.banner_online_tag);

            timeStatusView.setText(NotificationTimeFormatter.getBannerTimeStatus(now, startTime, endTime));
            timeStatusView.setTextColor(redColor);
            applyTintedIcon(timeStatusView, android.R.drawable.ic_menu_recent_history, redColor, iconSizePx);

            String parsedLocation = LocationParser.parseSmartLocation(this, title, rawLocation, description);
            applyTintedIcon(detailsView, android.R.drawable.ic_menu_mylocation, greyColor, iconSizePx);
            setLocationText(detailsView, onlineTagView, parsedLocation, rawLocation, description);

        } catch (Exception e) {
            e.printStackTrace();
            titleView.setText(title);
            detailsView.setText("Check schedule for details");
        }
    }

    private void applyTintedIcon(TextView view, int drawableRes, int color, int sizePx) {
        android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(this, drawableRes);
        if (icon != null) {
            icon = androidx.core.graphics.drawable.DrawableCompat.wrap(icon).mutate();
            androidx.core.graphics.drawable.DrawableCompat.setTint(icon, color);
            icon.setBounds(0, 0, sizePx, sizePx);
            view.setCompoundDrawables(icon, null, null, null);
            view.setCompoundDrawablePadding(16);
        }
    }

    private void setLocationText(TextView detailsView, TextView onlineTagView,
                                  String parsedLocation, String rawLocation, String description) {
        if (parsedLocation.equals("Online")) {
            onlineTagView.setVisibility(View.VISIBLE);
            detailsView.setText(resolveOnlineLabel(rawLocation, description));
        } else {
            onlineTagView.setVisibility(View.GONE);
            detailsView.setText(parsedLocation.equals("TBD") && !rawLocation.isEmpty() ? rawLocation : parsedLocation);
        }
    }

    private String resolveOnlineLabel(String rawLocation, String description) {
        String search = (rawLocation + " " + description).toLowerCase();
        if (search.contains("zoom")) return "ZOOM MEETING";
        if (search.contains("teams")) return "MICROSOFT TEAMS";
        if (search.contains("meet.google")) return "GOOGLE MEET";
        return rawLocation.isEmpty() ? "ONLINE CLASS" : rawLocation.toUpperCase();
    }

    // Alert pop up and signout functionality
    private void configureSignOut(){
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CALENDAR_SCOPE))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, options);

        MaterialButton btnSignOut = findViewById(R.id.btn_sign_out);
        btnSignOut.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm_sign_out)
                        .setMessage(R.string.confirm_sign_out_body)
                        .setPositiveButton(R.string.sign_out, (dialog, which) -> googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                startActivity(new Intent(this, GoogleCalendarAuthActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, R.string.sign_out_failed_body, Toast.LENGTH_SHORT).show();
                            }
                        }))
                        .setNegativeButton(R.string.cancel, null)
                        .show()
        );
    }
}