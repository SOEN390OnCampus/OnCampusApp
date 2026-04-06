package com.example.oncampusapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private int lastAppliedTextPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextSizePreferences.apply(this);
        lastAppliedTextPercent = getEffectiveTextPercent();
        setContentView(R.layout.activity_settings);

        setupStatusBarColor();

        ImageView backButton = findViewById(R.id.btn_back_settings);
        backButton.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            disableActivityTransitions();
        });

        findViewById(R.id.btn_accessibility_action).setOnClickListener(v ->
                startActivity(new Intent(this, AccessibilityActivity.class)));

        findViewById(R.id.btn_about_action).setOnClickListener(v ->
                Toast.makeText(this, "About content coming soon", Toast.LENGTH_SHORT).show());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MapsActivity.class));
                finish();
                disableActivityTransitions();
                return true;
            }
            if (id == R.id.nav_account) {
                startActivity(new Intent(this, GoogleCalendarAuthActivity.class));
                finish();
                disableActivityTransitions();
                return true;
            }
            return id == R.id.nav_settings;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentPercent = getEffectiveTextPercent();
        if (currentPercent != lastAppliedTextPercent) {
            lastAppliedTextPercent = currentPercent;
            recreate();
            return;
        }

        if (TextSizePreferences.apply(this)) {
            recreate();
        }
    }

    private int getEffectiveTextPercent() {
        return TextSizePreferences.isTextSizeEnabled(this)
                ? TextSizePreferences.getTextSizePercent(this)
                : 100;
    }

    /**
     * Safely disables transition animations, supporting both modern (API 34+) and legacy Android versions.
     */
    @SuppressWarnings("deprecation")
    private void disableActivityTransitions() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0);
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0);
        } else {
            overridePendingTransition(0, 0);
        }
    }

    /**
     * Sets the status bar color. Suppressed because API 35 deprecates this in favor of Edge-to-Edge.
     */
    @SuppressWarnings("deprecation")
    private void setupStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#7A1C1C"));
    }
}