package com.example.oncampusapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

public final class TextSizePreferences {

    private static final String PREFS_NAME = "OnCampusAccessibilityPrefs";
    private static final String KEY_TEXT_SIZE_ENABLED = "text_size_enabled";
    private static final String KEY_TEXT_SIZE_PERCENT = "text_size_percent";

    private static final int DEFAULT_PERCENT = 100;

    private TextSizePreferences() {
        // Utility class
    }

    public static boolean apply(Activity activity) {
        int percent = isTextSizeEnabled(activity) ? getTextSizePercent(activity) : DEFAULT_PERCENT;
        float targetScale = percent / 100f;

        Resources resources = activity.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        if (Math.abs(configuration.fontScale - targetScale) < 0.001f) {
            return false;
        }

        configuration.fontScale = targetScale;
        activity.applyOverrideConfiguration(configuration);
        return true;
    }

    public static boolean isTextSizeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_TEXT_SIZE_ENABLED, false);
    }

    public static void setTextSizeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_TEXT_SIZE_ENABLED, enabled).apply();
    }

    public static int getTextSizePercent(Context context) {
        int saved = prefs(context).getInt(KEY_TEXT_SIZE_PERCENT, DEFAULT_PERCENT);
        return Math.max(50, Math.min(200, saved));
    }

    public static void setTextSizePercent(Context context, int percent) {
        int clamped = Math.max(50, Math.min(200, percent));
        prefs(context).edit().putInt(KEY_TEXT_SIZE_PERCENT, clamped).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}