package com.example.oncampusapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity that handles Google Calendar OAuth authentication.
 *
 * <p>User Story: As a user, I want to connect to my Google Calendar,
 * so I can easily see when my next event at university is.
 *
 * <p>Flow:
 * 1. User taps "Connect Google Calendar"
 * 2. Google OAuth screen appears
 * 3. User logs in
 * 4. App retrieves the user's calendar list
 * 5. Success/failure feedback is displayed
 */
public class GoogleCalendarAuthActivity extends AppCompatActivity {

    private CalendarRepository calendarRepository;
    private static final String TAG = "GoogleCalendarAuth";
    private static final String CALENDAR_SCOPE =
            "https://www.googleapis.com/auth/calendar.readonly";

    private GoogleSignInClient googleSignInClient;
    private MaterialButton connectButton;
    private ProgressBar progressBar;

    private BottomNavigationView bottomNav;
    private TextView statusText;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Launcher for the Google Sign-In intent, replacing deprecated onActivityResult. */
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::handleSignInResult);

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        calendarRepository = CalendarRepository.getInstance();

        setContentView(R.layout.calendar_login_page);

        bindViews();
        setUpBottomNav();

        GoogleSignInAccount account =
                GoogleSignIn.getLastSignedInAccount(this);

        if (account != null) {
            onSignInSuccess(account);
            return;
        }

        setupGoogleSignIn();

        connectButton.setOnClickListener(v -> startSignIn());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private void bindViews() {
        bottomNav = findViewById(R.id.bottom_nav);
        connectButton = findViewById(R.id.btn_calendar_signin);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
    }

    private void setUpBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_account); // highlight account tab

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(GoogleCalendarAuthActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
                return true;
            }

            else if (id == R.id.nav_account) {
                // Already on account page
                return true;
            }

            else if (id == R.id.nav_settings) {
                Intent intent = new Intent(GoogleCalendarAuthActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CALENDAR_SCOPE))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, options);
    }

    // -------------------------------------------------------------------------
    // Sign-In flow
    // -------------------------------------------------------------------------

    /** Kick off the Google Sign-In intent. */
    private void startSignIn() {
        // Sign out any previous session so the account-picker always appears.
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });
    }

    /** Called when the Google Sign-In activity returns a result. */
    private void handleSignInResult(ActivityResult activityResult) {
        int resultCode = activityResult.getResultCode();
        Intent data = activityResult.getData();

        if (resultCode == RESULT_CANCELED) {
            showAuthCancelled();
            return;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            onSignInSuccess(account);
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in failed, code=" + e.getStatusCode(), e);
            showAuthError(e.getStatusCode());
        }
    }

    private void onSignInSuccess(GoogleSignInAccount account) {
        showLoading(true);
        setStatusText("Signed in as " + account.getEmail() + "\nFetching calendars…", false);

        executor.execute(() -> {
            try {
                String token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                        this,
                        account.getAccount(),
                        "oauth2:" + CALENDAR_SCOPE);

                JSONArray allEventsArray = calendarRepository.fetchAllEvents(token);
                String allCalendars = calendarRepository.fetchCalendarList(token);

                mainHandler.post(() -> {
                    showLoading(false);

                    // Save the massive JSON strings to global variables to avoid TransactionTooLargeException
                    CalendarEventManager.globalEventsJson = allEventsArray.toString();
                    CalendarEventManager.globalCalendarListJson = allCalendars;

                    Intent intent = new Intent(GoogleCalendarAuthActivity.this, AccountPage.class);

                    // Pass only small pieces of data through the Intent
                    intent.putExtra("email", account.getEmail());
                    intent.putExtra("calendar_token", token);
                    startActivity(intent);
                    finish();
                });

            } catch (com.google.android.gms.auth.UserRecoverableAuthException e) {
                Log.w(TAG, "UserRecoverableAuthException", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    startActivity(e.getIntent());
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch calendar events", e);
                mainHandler.post(() -> showCalendarFetchError(e.getMessage()));
            }
        });
    }

    // -------------------------------------------------------------------------
    // UI feedback helpers
    // -------------------------------------------------------------------------

    /**
     * Displays a user-friendly error when authentication fails.
     * AC: User is informed if authentication fails.
     */
    private void showAuthError(int statusCode) {
        showLoading(false);
        String message = "Authentication failed (code " + statusCode + ").\n"
                + "Please try again or check your Google account settings.";
        setStatusText("✗ " + message, true);

        new AlertDialog.Builder(this)
                .setTitle("Authentication Failed")
                .setMessage(message)
                .setPositiveButton("Retry", (d, w) -> startSignIn())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Displays feedback when the user cancels the sign-in flow.
     * AC: User is informed if authentication is cancelled.
     */
    private void showAuthCancelled() {
        showLoading(false);
        setStatusText("Sign-in was cancelled. Tap the button to try again.", true);
        Snackbar.make(connectButton,
                "Sign-in cancelled.", Snackbar.LENGTH_LONG).show();
    }

    private void showCalendarFetchError(String detail) {
        showLoading(false);
        setStatusText("✗ Signed in, but failed to load calendars.\n" + detail, true);
    }

    private void setStatusText(String message, boolean isError) {
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(message);
        statusText.setTextColor(ContextCompat.getColor(this,
                isError ? android.R.color.holo_red_dark : android.R.color.holo_green_dark));
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        connectButton.setEnabled(!loading);
    }
}
