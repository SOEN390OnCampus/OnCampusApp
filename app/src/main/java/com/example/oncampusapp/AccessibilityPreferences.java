package com.example.oncampusapp;

import android.content.Context;
import android.content.SharedPreferences;

public final class AccessibilityPreferences {
    private static final String PREFS_NAME = "OnCampusAccessibilityPrefs";
    private static final String KEY_REDUCED_MOBILITY = "reduced_mobility_enabled";

    private AccessibilityPreferences() {
    }

    public static boolean isReducedMobilityEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_REDUCED_MOBILITY, false);
    }

    public static void setReducedMobilityEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_REDUCED_MOBILITY, enabled)
                .apply();
    }
}