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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        assertEquals(10f, resultSingle.get(0).x, 0.001);
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
        assertEquals(0f, result.get(0).x, 0.001);
        assertEquals(0f, result.get(0).y, 0.001);
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
        assertEquals(0f, result.get(0).x, 0.001);
        assertEquals(0f, result.get(0).y, 0.001);

        // First interpolated dot
        assertEquals(120f, result.get(1).x, 0.001);
        assertEquals(0f, result.get(1).y, 0.001);

        // Second interpolated dot
        assertEquals(240f, result.get(2).x, 0.001);
        assertEquals(0f, result.get(2).y, 0.001);
    }

    // ── setPath / clearPath ───────────────────────────────────────────────────

    @Test
    public void testSetPath_storesPoints() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        IndoorMapView mapView = new IndoorMapView(context, null);

        List<PointF> points = Arrays.asList(new PointF(0f, 0f), new PointF(100f, 0f));
        mapView.setPath(points);

        Field field = IndoorMapView.class.getDeclaredField("pathPoints");
        field.setAccessible(true);
        List<?> stored = (List<?>) field.get(mapView);
        assertEquals(2, stored.size());
    }

    @Test
    public void testSetPath_null_storesEmptyList() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        IndoorMapView mapView = new IndoorMapView(context, null);

        mapView.setPath(null);

        Field field = IndoorMapView.class.getDeclaredField("pathPoints");
        field.setAccessible(true);
        List<?> stored = (List<?>) field.get(mapView);
        assertTrue(stored.isEmpty());
    }

    @Test
    public void testClearPath_emptiesPathAndEvenPoints() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        IndoorMapView mapView = new IndoorMapView(context, null);

        mapView.setPath(Arrays.asList(new PointF(0f, 0f), new PointF(200f, 0f)));
        mapView.clearPath();

        Field pathField = IndoorMapView.class.getDeclaredField("pathPoints");
        pathField.setAccessible(true);
        Field evenField = IndoorMapView.class.getDeclaredField("evenPathPoints");
        evenField.setAccessible(true);

        assertTrue(((List<?>) pathField.get(mapView)).isEmpty());
        assertTrue(((List<?>) evenField.get(mapView)).isEmpty());
    }

    // ── onMeasure ─────────────────────────────────────────────────────────────

    @Test
    public void testOnMeasure_withFloorPlan_exactlyMode_usesGivenHeight() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        IndoorMapView mapView = new IndoorMapView(activity, null);

        // Inject a 200x400 floor plan so the aspect-ratio branch is reached
        Bitmap bitmap = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888);
        Field fp = IndoorMapView.class.getDeclaredField("floorPlan");
        fp.setAccessible(true);
        fp.set(mapView, bitmap);

        int widthSpec  = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY);
        mapView.measure(widthSpec, heightSpec);

        assertEquals(300, mapView.getMeasuredHeight());
    }

    @Test
    public void testOnMeasure_withFloorPlan_atMostMode_clampsToBound() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        IndoorMapView mapView = new IndoorMapView(activity, null);

        Bitmap bitmap = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888);
        Field fp = IndoorMapView.class.getDeclaredField("floorPlan");
        fp.setAccessible(true);
        fp.set(mapView, bitmap);

        int widthSpec  = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST);
        mapView.measure(widthSpec, heightSpec);

        // desired = 500 * (400/200) = 1000 — clamped to AT_MOST bound 200
        assertEquals(200, mapView.getMeasuredHeight());
    }

    @Test
    public void testOnMeasure_withFloorPlan_unspecifiedMode_usesDesiredHeight() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        IndoorMapView mapView = new IndoorMapView(activity, null);

        // 100x50 bitmap: aspect = 0.5, width=400 → desired height = 200
        Bitmap bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888);
        Field fp = IndoorMapView.class.getDeclaredField("floorPlan");
        fp.setAccessible(true);
        fp.set(mapView, bitmap);

        int widthSpec  = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        mapView.measure(widthSpec, heightSpec);

        assertEquals(200, mapView.getMeasuredHeight());
    }

    @Test
    public void testOnMeasure_withoutFloorPlan_usesDefaultMeasure() {
        Context context = ApplicationProvider.getApplicationContext();
        IndoorMapView mapView = new IndoorMapView(context, null);

        int spec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY);
        mapView.measure(spec, spec);

        assertNotNull(mapView);
    }

    // ── onDraw ────────────────────────────────────────────────────────────────

    @Test
    public void testOnDraw_withFloorPlanAndPin_doesNotThrow() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        IndoorMapView mapView = new IndoorMapView(activity, null);

        Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        mapView.setMapData(bitmap);
        ShadowLooper.idleMainLooper();

        mapView.setPinLocation(100f, 100f);

        // Draw onto a real canvas backed by a bitmap
        Bitmap canvas = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        mapView.draw(new android.graphics.Canvas(canvas));

        assertNotNull(mapView);
    }

    @Test
    public void testOnDraw_withPath_doesNotThrow() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        IndoorMapView mapView = new IndoorMapView(activity, null);
        activity.setContentView(mapView);

        int spec = View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY);
        mapView.measure(spec, spec);
        mapView.layout(0, 0, 300, 300);

        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        mapView.setMapData(bitmap);
        ShadowLooper.idleMainLooper();

        mapView.setPath(Arrays.asList(new PointF(0f, 0f), new PointF(300f, 0f)));

        Bitmap canvas = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        mapView.draw(new android.graphics.Canvas(canvas));

        assertNotNull(mapView);
    }
}