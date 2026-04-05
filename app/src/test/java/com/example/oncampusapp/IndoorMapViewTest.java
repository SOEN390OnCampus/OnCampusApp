package com.example.oncampusapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class IndoorMapViewTest {

    @Test
    public void testSetPinLocation_updatesCoordinates() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        IndoorMapView mapView = new IndoorMapView(context, null);

        mapView.setPinLocation(500f, 600f);

        Field pinXField = IndoorMapView.class.getDeclaredField("pinX");
        pinXField.setAccessible(true);
        float actualX = (float) pinXField.get(mapView);

        Field pinYField = IndoorMapView.class.getDeclaredField("pinY");
        pinYField.setAccessible(true);
        float actualY = (float) pinYField.get(mapView);

        assertEquals(500f, actualX, 0.001);
        assertEquals(600f, actualY, 0.001);
    }

    @Test
    public void testSetMapData_initializesScaleAndMatrix() throws Exception {
        // Build a dummy Activity and attach the view so post() is allowed to run
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        IndoorMapView mapView = new IndoorMapView(activity, null);
        activity.setContentView(mapView);

        // Force the layout engine to treat the view as 500x500 pixels
        int measureSpec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY);
        mapView.measure(measureSpec, measureSpec);
        mapView.layout(0, 0, 500, 500);

        // Create a 1000x1000 dummy image
        Bitmap dummyBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);

        mapView.setMapData(dummyBitmap);

        ShadowLooper.idleMainLooper();

        // Read the private scaleFactor field
        Field scaleField = IndoorMapView.class.getDeclaredField("scaleFactor");
        scaleField.setAccessible(true);
        float actualScale = (float) scaleField.get(mapView);

        float expectedScaleX = (float) mapView.getWidth() / 1000f;
        float expectedScaleY = (float) mapView.getHeight() / 1000f;
        float expectedScale = Math.min(expectedScaleX, expectedScaleY) * 0.95f;

        assertEquals(expectedScale, actualScale, 0.001);
    }

    // ─── NEW TESTS FOR PATH INTERPOLATION MATH ──────────────────────────────

    @Test
    public void testInterpolateEvenPoints_EmptyOrSinglePoint() {
        Context context = ApplicationProvider.getApplicationContext();
        IndoorMapView mapView = new IndoorMapView(context, null);

        List<PointF> emptyList = new ArrayList<>();
        List<PointF> resultEmpty = mapView.interpolateEvenPoints(emptyList, 120f);
        assertTrue(resultEmpty.isEmpty());

        List<PointF> singlePoint = Arrays.asList(new PointF(10f, 10f));
        List<PointF> resultSingle = mapView.interpolateEvenPoints(singlePoint, 120f);
        assertEquals(1, resultSingle.size());
        float result = resultSingle.get(0).x;
        assertEquals(10f, result, 0.001);
    }

    @Test
    public void testInterpolateEvenPoints_ShortSegment() {
        Context context = ApplicationProvider.getApplicationContext();
        IndoorMapView mapView = new IndoorMapView(context, null);

        // Segment of length 50, but spacing is 120. Should only return the start point.
        List<PointF> raw = Arrays.asList(
                new PointF(0f, 0f),
                new PointF(50f, 0f)
        );

        List<PointF> result = mapView.interpolateEvenPoints(raw, 120f);

        assertEquals(1, result.size());
        float x = result.get(0).x;
        float y = result.get(0).y;
        assertEquals(0f, x, 0.001);
        assertEquals(0f, y, 0.001);
    }

    @Test
    public void testInterpolateEvenPoints_LongStraightLine() {
        Context context = ApplicationProvider.getApplicationContext();
        IndoorMapView mapView = new IndoorMapView(context, null);

        // Straight line from x=0 to x=300. Spacing 120. Expected dots: 0, 120, 240.
        List<PointF> raw = Arrays.asList(
                new PointF(0f, 0f),
                new PointF(300f, 0f)
        );

        List<PointF> result = mapView.interpolateEvenPoints(raw, 120f);

        assertEquals(3, result.size());

        // Start point
        float x0 = result.get(0).x;
        float y0 = result.get(0).y;

        assertEquals(0f, x0, 0.001);
        assertEquals(0f, y0, 0.001);

        // First interpolated dot
        float x1 = result.get(1).x;
        float y1 = result.get(1).y;

        assertEquals(120f, x1, 0.001);
        assertEquals(0f, y1, 0.001);

        // Second interpolated dot
        float x2 = result.get(2).x;
        float y2 = result.get(2).y;

        assertEquals(240f, x2, 0.001);
        assertEquals(0f, y2, 0.001);
    }
}