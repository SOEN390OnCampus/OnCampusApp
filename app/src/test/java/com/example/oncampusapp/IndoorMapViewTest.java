package com.example.oncampusapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;


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
}