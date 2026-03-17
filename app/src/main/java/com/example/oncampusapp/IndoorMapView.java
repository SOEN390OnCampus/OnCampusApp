package com.example.oncampusapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Custom View responsible for rendering the indoor floor plan.
 * It handles pinch-to-zoom, dragging, and drawing a dynamic location pin.
 */
public class IndoorMapView extends View {

    // The actual image of the floor plan
    private Bitmap floorPlan;

    // The matrix controls the translation (panning) and scaling (zooming) of the canvas
    private Matrix matrix = new Matrix();

    // Zoom constraints to prevent the user from zooming into infinity or out into the void
    private float scaleFactor = 1.0f;
    private float minScaleFactor = 1.0f;
    private float maxScaleFactor = 5.0f;

    // Android framework detectors for pinch and swipe gestures
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    // Pin Variables
    private float pinX = -1;
    private float pinY = -1;
    private Drawable pinDrawable;
    private Paint fallbackPaint;

    public IndoorMapView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize the gesture listeners
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        // Set up a simple red circle just in case the proper pin icon fails to load
        fallbackPaint = new Paint();
        fallbackPaint.setColor(Color.parseColor("#D32F2F")); // Concordia-ish Red
        fallbackPaint.setStyle(Paint.Style.FILL);
        fallbackPaint.setAntiAlias(true);

        // Load the vector pin icon and tint it red
        pinDrawable = ContextCompat.getDrawable(context, R.drawable.ic_location_solid);
        if (pinDrawable != null) {
            pinDrawable = DrawableCompat.wrap(pinDrawable).mutate();
            DrawableCompat.setTint(pinDrawable, Color.parseColor("#D32F2F"));
        }
    }

    /**
     * Injects the floor plan image into the view and calculates the initial zoom
     * so the map fits perfectly inside the screen margins.
     */
    public void setMapData(Bitmap bitmap) {
        this.floorPlan = bitmap;

        // post() ensures we only calculate math AFTER the view has been physically measured on screen
        post(() -> {
            if (getWidth() > 0 && getHeight() > 0 && floorPlan != null) {
                // Calculate the ratio needed to fit the entire image inside the view
                float scaleX = (float) getWidth() / floorPlan.getWidth();
                float scaleY = (float) getHeight() / floorPlan.getHeight();

                // Multiply by 0.95 to leave a 5% margin around the edges
                scaleFactor = Math.min(scaleX, scaleY) * 0.95f;

                // Lock the zooming limits based on the initial screen fit
                minScaleFactor = scaleFactor;
                maxScaleFactor = minScaleFactor * 4.0f;

                // Center the image exactly in the middle of the screen
                float dx = (getWidth() - floorPlan.getWidth() * scaleFactor) / 2f;
                float dy = (getHeight() - floorPlan.getHeight() * scaleFactor) / 2f;

                // Apply the math to the matrix
                matrix.setScale(scaleFactor, scaleFactor);
                matrix.postTranslate(dx, dy);

                // Force the view to redraw itself
                invalidate();
            }
        });
    }

    /**
     * Updates the target destination for the location pin and redraws the map.
     */
    public void setPinLocation(float x, float y) {
        this.pinX = x;
        this.pinY = y;
        invalidate(); // Triggers onDraw()
    }

    /**
     * Prevents the user from dragging the map off the screen.
     * Calculates the bounding box of the image and snaps it back if it crosses the edge.
     */
    private void constrainMatrix() {
        if (floorPlan == null) return;

        RectF bounds = new RectF(0, 0, floorPlan.getWidth(), floorPlan.getHeight());
        matrix.mapRect(bounds);

        float deltaX = 0;
        float deltaY = 0;
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // Horizontal constraints
        if (bounds.width() < viewWidth) {
            deltaX = (viewWidth - bounds.width()) / 2f - bounds.left;
        } else {
            if (bounds.left > 0) deltaX = -bounds.left;
            else if (bounds.right < viewWidth) deltaX = viewWidth - bounds.right;
        }

        // Vertical constraints
        if (bounds.height() < viewHeight) {
            deltaY = (viewHeight - bounds.height()) / 2f - bounds.top;
        } else {
            if (bounds.top > 0) deltaY = -bounds.top;
            else if (bounds.bottom < viewHeight) deltaY = viewHeight - bounds.bottom;
        }

        // Apply the snap-back correction if necessary
        if (deltaX != 0 || deltaY != 0) {
            matrix.postTranslate(deltaX, deltaY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass raw touch data to our dedicated gesture handlers
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true; // Consume the event
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (floorPlan == null) return;

        // Save the raw canvas state before applying transformations
        canvas.save();

        // Apply our zooming and panning matrix
        canvas.concat(matrix);

        // Draw the base floor plan
        canvas.drawBitmap(floorPlan, 0, 0, null);

        // If a search was executed, draw the pin on top of the floor plan
        if (pinX != -1 && pinY != -1) {
            // As the user zooms in, we scale the pin DOWN so it stays the exact same visual size
            float inverseScale = 1.0f / scaleFactor;

            if (pinDrawable != null) {
                int pinSize = (int) (90 * inverseScale); // Base size 90

                // Center the bottom point of the pin exactly on the (x,y) coordinates
                pinDrawable.setBounds(
                        (int) (pinX - (pinSize / 2f)),
                        (int) (pinY - pinSize),
                        (int) (pinX + (pinSize / 2f)),
                        (int) pinY
                );
                pinDrawable.draw(canvas);
            } else {
                canvas.drawCircle(pinX, pinY, 40f * inverseScale, fallbackPaint);
            }
        }

        // Restore the canvas so UI elements outside this view aren't affected
        canvas.restore();
    }

    /**
     * Handles pinch-to-zoom gestures.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleMultiplier = detector.getScaleFactor();
            float proposedScale = scaleFactor * scaleMultiplier;

            // Clamp the zoom between our min and max boundaries
            if (proposedScale < minScaleFactor) {
                scaleMultiplier = minScaleFactor / scaleFactor;
                scaleFactor = minScaleFactor;
            } else if (proposedScale > maxScaleFactor) {
                scaleMultiplier = maxScaleFactor / scaleFactor;
                scaleFactor = maxScaleFactor;
            } else {
                scaleFactor = proposedScale;
            }

            // Apply zoom centered precisely between the user's two fingers
            matrix.postScale(scaleMultiplier, scaleMultiplier, detector.getFocusX(), detector.getFocusY());
            constrainMatrix();
            invalidate();
            return true;
        }
    }

    /**
     * Handles panning (dragging) gestures.
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // distanceX and distanceY are how far the finger moved. We reverse them to drag the map.
            matrix.postTranslate(-distanceX, -distanceY);
            constrainMatrix();
            invalidate();
            return true;
        }
    }
}