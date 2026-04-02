package com.example.oncampusapp;

import android.app.Activity;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

public final class GrayscaleModeManager {

    private static final Paint GRAYSCALE_PAINT;

    static {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        GRAYSCALE_PAINT = paint;
    }

    private GrayscaleModeManager() {
    }

    public static void applyToActivity(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        boolean enabled = HighContrastPreferences.isEnabled(activity);

        if (enabled) {
            decorView.setLayerType(View.LAYER_TYPE_HARDWARE, GRAYSCALE_PAINT);
        } else {
            decorView.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }
}
