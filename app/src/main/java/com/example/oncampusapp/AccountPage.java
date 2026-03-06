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

public class AccountPage extends AppCompatActivity {

    private ImageView backButton;

    private String eventsJson;

    private BottomNavigationView bottomNav;

    private Button btnRefresh;

    private Button btnOpenCalendar;

    private CalendarRepository repository;

    private String email;


    private LinearLayout calendarContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        email = getIntent().getStringExtra("email");

        eventsJson = getIntent().getStringExtra("calendar_events_json");

        repository = CalendarRepository.getInstance();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_page);
        setViews();
        setUpBottomNav();

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountPage.this, MapsActivity.class);
            startActivity(intent);
        });

        TextView txtUserEmail = findViewById(R.id.txtUserEmail);

        GoogleSignInAccount account =
                GoogleSignIn.getLastSignedInAccount(this);

        if (account != null) {
            txtUserEmail.setText("Logged in as:\n" + account.getEmail());
        } else {
            txtUserEmail.setText("Not signed in");
        }

        btnOpenCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleViewer.class);
            intent.putExtra("calendar_events_json", eventsJson);
            startActivity(intent);
        });

        btnRefresh.setOnClickListener(v -> {
            showConnectDialog();
        });
    }

    private void setViews() {
        bottomNav = findViewById(R.id.bottom_nav);
        calendarContainer = findViewById(R.id.calendarListContainer);
        bottomNav = findViewById(R.id.bottom_nav);
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
}