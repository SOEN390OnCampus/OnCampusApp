package com.example.oncampusapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    private static final String TAG = "GoogleCalendarAuth";
    private static final String CALENDAR_SCOPE =
            "https://www.googleapis.com/auth/calendar.readonly";
    private static final String CALENDAR_LIST_URL =
            "https://www.googleapis.com/calendar/v3/users/me/calendarList";

    private GoogleSignInClient googleSignInClient;
    private MaterialButton connectButton;
    private ProgressBar progressBar;
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
        setContentView(R.layout.activity_google_calendar_auth);

        bindViews();
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
        connectButton = findViewById(R.id.connectButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
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

    // -------------------------------------------------------------------------
    // Post-authentication: fetch calendar list
    // -------------------------------------------------------------------------

    /**
     * Called when Google Sign-In succeeds.
     * Starts a background fetch of the user's Google Calendar list.
     */
    private void onSignInSuccess(GoogleSignInAccount account) {
        showLoading(true);
        setStatusText("Signed in as " + account.getEmail() + "\nFetching calendars…", false);

        executor.execute(() -> {
            try {
                // Obtain an OAuth2 bearer token for the calendar scope.
                String token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                        this,
                        account.getAccount(),
                        "oauth2:" + CALENDAR_SCOPE);

                // Fetch the calendar list from the Google Calendar API.
                String jsonResponse = fetchCalendarList(token);
                JSONObject root = new JSONObject(jsonResponse);
                JSONArray items = root.optJSONArray("items");
                int count = (items != null) ? items.length() : 0;

                mainHandler.post(() -> onCalendarListFetched(account.getEmail(), count, items));

            } catch (com.google.android.gms.auth.UserRecoverableAuthException e) {
                // The user must grant additional permissions through a recovery intent.
                Log.w(TAG, "UserRecoverableAuthException", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    startActivity(e.getIntent());
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch calendar list", e);
                mainHandler.post(() -> showCalendarFetchError(e.getMessage()));
            }
        });
    }

    /** Performs an authenticated GET request to the Calendar List endpoint. */
    private String fetchCalendarList(String accessToken) throws Exception {
        URL url = new URL(CALENDAR_LIST_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + responseCode + " from calendar API");
        }

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    // -------------------------------------------------------------------------
    // UI feedback helpers
    // -------------------------------------------------------------------------

    private void onCalendarListFetched(String email, int calendarCount, JSONArray items) {
        showLoading(false);
        StringBuilder msg = new StringBuilder();
        msg.append("✓ Connected as ").append(email).append("\n\n");
        if (calendarCount == 0) {
            msg.append("No calendars found.");
        } else {
            msg.append(calendarCount).append(" calendar(s) found:\n");
            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject cal = items.getJSONObject(i);
                    String summary = cal.optString("summary", "Unnamed");
                    msg.append("• ").append(summary).append("\n");
                } catch (Exception ignored) { }
            }
        }
        setStatusText(msg.toString().trim(), false);
        Snackbar.make(connectButton,
                "Google Calendar connected successfully!", Snackbar.LENGTH_LONG).show();
    }

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
