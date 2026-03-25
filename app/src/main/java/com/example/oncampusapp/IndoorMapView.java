package com.example.oncampusapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.annotation.VisibleForTesting;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // Path Variables
    private List<PointF> pathPoints     = Collections.emptyList(); // raw node coords
    private List<PointF> evenPathPoints = Collections.emptyList(); // evenly-spaced dots
    private static final float DOT_SPACING = 120f; // map-pixel gap between dots
    private Paint pathDotPaint;

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

        // Dot paint for path waypoints (colour set per-dot at draw time)
        pathDotPaint = new Paint();
        pathDotPaint.setColor(Color.parseColor("#1565C0"));
        pathDotPaint.setStyle(Paint.Style.FILL);
        pathDotPaint.setAntiAlias(true);

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
     * Draws a route polyline on the floor plan.
     * Points are in floor-plan pixel coordinates (matching node x/y values).
     */
    public void setPath(List<PointF> points) {
        this.pathPoints     = points != null ? points : Collections.emptyList();
        this.evenPathPoints = interpolateEvenPoints(this.pathPoints, DOT_SPACING);
        invalidate();
    }

    /** Removes any drawn path without changing the pin. */
    public void clearPath() {
        this.pathPoints     = Collections.emptyList();
        this.evenPathPoints = Collections.emptyList();
        invalidate();
    }

    /**
     * Resamples {@code raw} into points spaced exactly {@code spacing} map-pixels
     * apart by walking segment-by-segment and interpolating linearly.
     */
    @VisibleForTesting
    List<PointF> interpolateEvenPoints(List<PointF> raw, float spacing) {
        List<PointF> result = new ArrayList<>();
        if (raw.size() < 2) return new ArrayList<>(raw);

        result.add(raw.get(0));          // always keep the start
        float carry = 0f;                // leftover distance from previous segment

        for (int i = 0; i < raw.size() - 1; i++) {
            PointF a = raw.get(i);
            PointF b = raw.get(i + 1);

            float dx     = b.x - a.x;
            float dy     = b.y - a.y;
            float segLen = (float) Math.sqrt(dx * dx + dy * dy);
            if (segLen < 0.001f) continue;

            float ux = dx / segLen;   // unit vector along segment
            float uy = dy / segLen;

            float needed = spacing - carry;  // distance until next dot
            float pos    = 0f;

            while (pos + needed <= segLen) {
                pos += needed;
                result.add(new PointF(a.x + ux * pos, a.y + uy * pos));
                needed = spacing;
                carry  = 0f;
            }
            carry = segLen - pos;   // distance past the last dot on this segment
        }
        return result;
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

        // Draw evenly-spaced path dots (grey start, blue rest)
        if (!evenPathPoints.isEmpty()) {
            float dotRadius = 9f / scaleFactor; // ~9 screen-px regardless of zoom

            for (int i = 0; i < evenPathPoints.size(); i++) {
                PointF pt = evenPathPoints.get(i);
                pathDotPaint.setColor(i == 0
                        ? Color.parseColor("#9E9E9E")   // start: grey
                        : Color.parseColor("#1565C0")); // rest: blue
                canvas.drawCircle(pt.x, pt.y, dotRadius, pathDotPaint);
            }
        }

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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (floorPlan != null) {
            // Get the width that the layout gave us
            int width = MeasureSpec.getSize(widthMeasureSpec);

            // Calculate the exact height needed to keep the image's aspect ratio
            float aspectRatio = (float) floorPlan.getHeight() / (float) floorPlan.getWidth();
            int desiredHeight = Math.round(width * aspectRatio);

            // Respect any height constraints from the XML (like constrainedHeight="true")
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            int finalHeight;
            if (heightMode == MeasureSpec.EXACTLY) {
                finalHeight = heightSize;
            } else if (heightMode == MeasureSpec.AT_MOST) {
                finalHeight = Math.min(desiredHeight, heightSize);
            } else {
                finalHeight = desiredHeight;
            }

            // Lock in the new dimensions
            setMeasuredDimension(width, finalHeight);
        }
    }
}