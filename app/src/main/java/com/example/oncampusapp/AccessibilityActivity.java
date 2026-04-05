package com.example.oncampusapp;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class AccessibilityActivity extends AppCompatActivity {

    private int currentTextSizePercent = 100;

    private static final int TEXT_SIZE_STEP = 10;
    private static final int TEXT_SIZE_MIN = 50;
    private static final int TEXT_SIZE_MAX = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextSizePreferences.apply(this);
        setContentView(R.layout.activity_accessibility);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        window.getDecorView().setBackgroundColor(Color.parseColor("#7A1C1C"));

        ImageView backButton = findViewById(R.id.btn_back_accessibility);
        backButton.setOnClickListener(v -> finish());

        setupInteractiveControls();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MapsActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_account) {
                startActivity(new Intent(this, GoogleCalendarAuthActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void setupInteractiveControls() {
        Spinner languageSpinner = findViewById(R.id.spinner_language);
        String[] languages = new String[]{"English-US", "French-CA", "Spanish", "Arabic"};
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                this,
            R.layout.item_accessibility_spinner_selected,
                languages
        );
        languageAdapter.setDropDownViewResource(R.layout.item_accessibility_spinner_dropdown);
        languageSpinner.setAdapter(languageAdapter);
        languageSpinner.setSelection(0);
        languageSpinner.setEnabled(true);
        languageSpinner.setClickable(true);
        languageSpinner.setAlpha(1f);

        View reducedMobilityButton = findViewById(R.id.btn_reduced_mobility);

        boolean isReducedMobilityEnabled = AccessibilityPreferences.isReducedMobilityEnabled(this);
        reducedMobilityButton.setSelected(isReducedMobilityEnabled);

        reducedMobilityButton.setOnClickListener(v -> {
            boolean currentValue = AccessibilityPreferences.isReducedMobilityEnabled(this);
            boolean newValue = !currentValue;
            reducedMobilityButton.setSelected(newValue);
            AccessibilityPreferences.setReducedMobilityEnabled(this, newValue);
            Log.d("ACCESSIBILITY", "Toggle clicked. New value = " + newValue);
        });

        SwitchMaterial textSizeSwitch = findViewById(R.id.switch_text_size_control);
        SwitchMaterial highContrastSwitch = findViewById(R.id.switch_high_contrast);
        TextView textSizeValue = findViewById(R.id.txt_text_size_value);
        View zoomOut = findViewById(R.id.btn_text_zoom_out);
        View zoomIn = findViewById(R.id.btn_text_zoom_in);

        highContrastSwitch.setChecked(HighContrastPreferences.isEnabled(this));
        highContrastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            HighContrastPreferences.setEnabled(this, isChecked);
            GrayscaleModeManager.applyToActivity(this);
        });

        currentTextSizePercent = TextSizePreferences.getTextSizePercent(this);
        boolean isTextSizeEnabled = TextSizePreferences.isTextSizeEnabled(this);
        textSizeSwitch.setChecked(isTextSizeEnabled);

        updateTextSizeLabel(textSizeValue);
        updateTextSizeControlsState(zoomOut, zoomIn, textSizeValue, isTextSizeEnabled);

        textSizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TextSizePreferences.setTextSizeEnabled(this, isChecked);
            if (!isChecked) {
                currentTextSizePercent = 100;
                TextSizePreferences.setTextSizePercent(this, currentTextSizePercent);
                updateTextSizeLabel(textSizeValue);
            }

            updateTextSizeControlsState(zoomOut, zoomIn, textSizeValue, isChecked);
            TextSizePreferences.apply(this);
            recreate();
        });

        zoomOut.setOnClickListener(v -> {
            if (!textSizeSwitch.isChecked()) {
                return;
            }
            currentTextSizePercent = Math.max(TEXT_SIZE_MIN, currentTextSizePercent - TEXT_SIZE_STEP);
            TextSizePreferences.setTextSizePercent(this, currentTextSizePercent);
            updateTextSizeLabel(textSizeValue);
            TextSizePreferences.apply(this);
            recreate();
        });

        zoomIn.setOnClickListener(v -> {
            if (!textSizeSwitch.isChecked()) {
                return;
            }
            currentTextSizePercent = Math.min(TEXT_SIZE_MAX, currentTextSizePercent + TEXT_SIZE_STEP);
            TextSizePreferences.setTextSizePercent(this, currentTextSizePercent);
            updateTextSizeLabel(textSizeValue);
            TextSizePreferences.apply(this);
            recreate();
        });
    }

    private void updateTextSizeLabel(TextView label) {
        label.setText(currentTextSizePercent + "%");
    }

    private void updateTextSizeControlsState(View zoomOut, View zoomIn, TextView textSizeValue, boolean enabled) {
        zoomOut.setEnabled(enabled);
        zoomIn.setEnabled(enabled);
        float alpha = enabled ? 1f : 0.45f;
        zoomOut.setAlpha(alpha);
        zoomIn.setAlpha(alpha);
        textSizeValue.setAlpha(alpha);
    }
}
