package com.example.oncampusapp;

import android.content.Context;
import android.content.SharedPreferences;

public final class HighContrastPreferences {

    private static final String PREFS_NAME = "OnCampusAccessibilityPrefs";
    private static final String KEY_HIGH_CONTRAST_ENABLED = "high_contrast_enabled";

    private HighContrastPreferences() {
    }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_HIGH_CONTRAST_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST_ENABLED, enabled).apply();
    }
}
