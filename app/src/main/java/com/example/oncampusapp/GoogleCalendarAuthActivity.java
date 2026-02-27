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
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
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

    private void onSignInSuccess(GoogleSignInAccount account) {
        showLoading(true);
        setStatusText("Signed in as " + account.getEmail() + "\nFetching calendars…", false);

        executor.execute(() -> {
            try {
                String token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                        this,
                        account.getAccount(),
                        "oauth2:" + CALENDAR_SCOPE);

                String calendarListJson = fetchCalendarList(token);
                JSONObject root = new JSONObject(calendarListJson);
                JSONArray calendars = root.optJSONArray("items");

                List<JSONObject> allEvents = new ArrayList<>();
                if (calendars != null) {
                    for (int i = 0; i < calendars.length(); i++) {
                        JSONObject cal = calendars.getJSONObject(i);
                        String calendarId = cal.getString("id");

                        String eventsJson = fetchCalendarEvents(token, calendarId);
                        JSONObject eventsRoot = new JSONObject(eventsJson);
                        JSONArray events = eventsRoot.optJSONArray("items");
                        if (events != null) {
                            for (int j = 0; j < events.length(); j++) {
                                allEvents.add(events.getJSONObject(j));
                            }
                        }
                    }
                }

                JSONArray allEventsArray = new JSONArray(allEvents);

                mainHandler.post(() -> {
                    showLoading(false);
                    Intent intent = new Intent(GoogleCalendarAuthActivity.this, ScheduleViewer.class);
                    intent.putExtra("email", account.getEmail());
                    intent.putExtra("rawEvents", allEventsArray.toString());
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
    // Post-authentication: fetch calendar list
    // -------------------------------------------------------------------------


    private String fetchCalendarList(String accessToken) throws Exception {
        return fetchUrl("https://www.googleapis.com/calendar/v3/users/me/calendarList", accessToken);
    }

    private String fetchCalendarEvents(String accessToken, String calendarId) throws Exception {
        Calendar past = Calendar.getInstance();
        past.add(Calendar.MONTH, -6);   // 6 months back

        Calendar future = Calendar.getInstance();
        future.add(Calendar.MONTH, 6);  // 6 months forward

        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        String timeMin = sdf.format(past.getTime());
        String timeMax = sdf.format(future.getTime());

        String url = "https://www.googleapis.com/calendar/v3/calendars/"
                + URLEncoder.encode(calendarId, "UTF-8")
                + "/events"
                + "?singleEvents=true"
                + "&orderBy=startTime"
                + "&timeMin=" + URLEncoder.encode(timeMin, "UTF-8")
                + "&timeMax=" + URLEncoder.encode(timeMax, "UTF-8");

        return fetchUrl(url,accessToken);
    }

    private String fetchUrl(String urlStr, String accessToken) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + responseCode + " from Google API");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
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
        mainHandler.post(() -> {
            try {
                Intent intent = new Intent(GoogleCalendarAuthActivity.this, ScheduleViewer.class);
                intent.putExtra("email", email);
                intent.putExtra("calendarEventsJson", items != null ? items.toString() : "[]");
                startActivity(intent);
                finish(); // optional, closes auth activity
            } catch (Exception e) {
                e.printStackTrace();
                setStatusText("Failed to open ScheduleViewer: " + e.getMessage(), true);
            }

            Snackbar.make(connectButton,
                    "Google Calendar connected successfully!", Snackbar.LENGTH_LONG).show();
        });
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
