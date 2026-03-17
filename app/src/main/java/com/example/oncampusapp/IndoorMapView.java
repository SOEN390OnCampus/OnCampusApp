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

public class IndoorMapView extends View {
    private Bitmap floorPlan;
    private Matrix matrix = new Matrix();

    private float scaleFactor = 1.0f;
    private float minScaleFactor = 1.0f;
    private float maxScaleFactor = 5.0f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    // Pin Variables
    private float pinX = -1;
    private float pinY = -1;
    private Drawable pinDrawable;
    private Paint fallbackPaint;

    public IndoorMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        fallbackPaint = new Paint();
        fallbackPaint.setColor(Color.parseColor("#D32F2F")); // Bright Red
        fallbackPaint.setStyle(Paint.Style.FILL);
        fallbackPaint.setAntiAlias(true);

        pinDrawable = ContextCompat.getDrawable(context, R.drawable.ic_location_solid);
        if (pinDrawable != null) {
            pinDrawable = DrawableCompat.wrap(pinDrawable).mutate();
            DrawableCompat.setTint(pinDrawable, Color.parseColor("#D32F2F"));
        }
    }

    public void setMapData(Bitmap bitmap) {
        this.floorPlan = bitmap;

        post(() -> {
            if (getWidth() > 0 && getHeight() > 0 && floorPlan != null) {
                float scaleX = (float) getWidth() / floorPlan.getWidth();
                float scaleY = (float) getHeight() / floorPlan.getHeight();
                scaleFactor = Math.min(scaleX, scaleY) * 0.95f;

                minScaleFactor = scaleFactor;
                maxScaleFactor = minScaleFactor * 4.0f;

                float dx = (getWidth() - floorPlan.getWidth() * scaleFactor) / 2f;
                float dy = (getHeight() - floorPlan.getHeight() * scaleFactor) / 2f;

                matrix.setScale(scaleFactor, scaleFactor);
                matrix.postTranslate(dx, dy);
                invalidate();
            }
        });
    }

    public void setPinLocation(float x, float y) {
        this.pinX = x;
        this.pinY = y;
        invalidate();
    }

    private void constrainMatrix() {
        if (floorPlan == null) return;

        RectF bounds = new RectF(0, 0, floorPlan.getWidth(), floorPlan.getHeight());
        matrix.mapRect(bounds);

        float deltaX = 0;
        float deltaY = 0;
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (bounds.width() < viewWidth) {
            deltaX = (viewWidth - bounds.width()) / 2f - bounds.left;
        } else {
            if (bounds.left > 0) {
                deltaX = -bounds.left;
            } else if (bounds.right < viewWidth) {
                deltaX = viewWidth - bounds.right;
            }
        }

        if (bounds.height() < viewHeight) {
            deltaY = (viewHeight - bounds.height()) / 2f - bounds.top;
        } else {
            if (bounds.top > 0) {
                deltaY = -bounds.top;
            } else if (bounds.bottom < viewHeight) {
                deltaY = viewHeight - bounds.bottom;
            }
        }

        if (deltaX != 0 || deltaY != 0) {
            matrix.postTranslate(deltaX, deltaY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (floorPlan == null) return;

        canvas.save();
        canvas.concat(matrix);
        canvas.drawBitmap(floorPlan, 0, 0, null);

        if (pinX != -1 && pinY != -1) {
            float inverseScale = 1.0f / scaleFactor;

            if (pinDrawable != null) {
                int pinSize = (int) (90 * inverseScale);

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

        canvas.restore();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleMultiplier = detector.getScaleFactor();
            float proposedScale = scaleFactor * scaleMultiplier;

            if (proposedScale < minScaleFactor) {
                scaleMultiplier = minScaleFactor / scaleFactor;
                scaleFactor = minScaleFactor;
            } else if (proposedScale > maxScaleFactor) {
                scaleMultiplier = maxScaleFactor / scaleFactor;
                scaleFactor = maxScaleFactor;
            } else {
                scaleFactor = proposedScale;
            }

            matrix.postScale(scaleMultiplier, scaleMultiplier, detector.getFocusX(), detector.getFocusY());
            constrainMatrix();
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            matrix.postTranslate(-distanceX, -distanceY);
            constrainMatrix();
            invalidate();
            return true;
        }
    }
}