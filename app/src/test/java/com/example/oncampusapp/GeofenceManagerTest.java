package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class GeofenceManagerTest {

    private Context context;
    private GeofenceManager manager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        manager = new GeofenceManager(context);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(manager);
    }

    @Test
    public void constructor_withApplicationContext_doesNotThrow() {
        assertNotNull(new GeofenceManager(context.getApplicationContext()));
    }

    // ── getPolygonCenter ──────────────────────────────────────────────────────

    @Test
    public void getPolygonCenter_twoPoints_returnsAverageLatLng() {
        List<LatLng> points = Arrays.asList(
                new LatLng(44.0, -72.0),
                new LatLng(46.0, -74.0));
        LatLng center = GeofenceManager.getPolygonCenter(points);
        assertEquals(45.0, center.latitude,  0.0001);
        assertEquals(-73.0, center.longitude, 0.0001);
    }

    @Test
    public void getPolygonCenter_singlePoint_returnsThatPoint() {
        List<LatLng> points = Collections.singletonList(new LatLng(45.497, -73.579));
        LatLng center = GeofenceManager.getPolygonCenter(points);
        assertEquals(45.497,  center.latitude,  0.0001);
        assertEquals(-73.579, center.longitude, 0.0001);
    }

    @Test
    public void getPolygonCenter_fourEqualPoints_returnsSamePoint() {
        LatLng pt = new LatLng(45.0, -73.0);
        List<LatLng> points = Arrays.asList(pt, pt, pt, pt);
        LatLng center = GeofenceManager.getPolygonCenter(points);
        assertEquals(45.0,  center.latitude,  0.0001);
        assertEquals(-73.0, center.longitude, 0.0001);
    }

    @Test
    public void getPolygonCenter_fourSymmetricPoints_returnsOrigin() {
        List<LatLng> points = Arrays.asList(
                new LatLng( 10.0,  10.0),
                new LatLng( 10.0, -10.0),
                new LatLng(-10.0,  10.0),
                new LatLng(-10.0, -10.0));
        LatLng center = GeofenceManager.getPolygonCenter(points);
        assertEquals(0.0, center.latitude,  0.0001);
        assertEquals(0.0, center.longitude, 0.0001);
    }

    @Test
    public void getPolygonCenter_threePoints_computedCorrectly() {
        List<LatLng> points = Arrays.asList(
                new LatLng(45.0, -73.0),
                new LatLng(46.0, -74.0),
                new LatLng(47.0, -75.0));
        LatLng center = GeofenceManager.getPolygonCenter(points);
        assertEquals(46.0,  center.latitude,  0.0001);
        assertEquals(-74.0, center.longitude, 0.0001);
    }

    // ── getPolygonRadius ──────────────────────────────────────────────────────

    @Test
    public void getPolygonRadius_centerInList_returnsAtLeastBuffer() {
        LatLng center = new LatLng(45.5, -73.5);
        List<LatLng> points = Collections.singletonList(center); // distance = 0
        float radius = GeofenceManager.getPolygonRadius(center, points);
        // 0 metres to itself + 10 m buffer = 10
        assertEquals(10f, radius, 0.5f);
    }

    @Test
    public void getPolygonRadius_distantPoint_greaterThanBuffer() {
        LatLng center = new LatLng(45.5, -73.5);
        List<LatLng> points = Arrays.asList(
                center,
                new LatLng(45.501, -73.5)); // ~111 m north
        float radius = GeofenceManager.getPolygonRadius(center, points);
        assertTrue("Radius should exceed the 10 m buffer", radius > 10f);
    }

    @Test
    public void getPolygonRadius_returnsMaxDistancePlusBuffer() {
        LatLng center = new LatLng(0.0, 0.0);
        List<LatLng> points = Arrays.asList(
                new LatLng(0.001, 0.0),  // small offset (~111 m)
                new LatLng(0.01,  0.0)); // larger offset (~1111 m)
        float radius = GeofenceManager.getPolygonRadius(center, points);
        // The radius must reflect the farthest point
        assertTrue("Radius must be based on the farthest point", radius > 100f);
    }

    // ── drawGeofenceCircle ────────────────────────────────────────────────────

    @Test
    public void drawGeofenceCircle_returnsNonNull() {
        CircleOptions opts = GeofenceManager.drawGeofenceCircle(
                new LatLng(45.5, -73.5), 100f);
        assertNotNull(opts);
    }

    @Test
    public void drawGeofenceCircle_differentRadii_doNotThrow() {
        LatLng center = new LatLng(45.5, -73.5);
        assertNotNull(GeofenceManager.drawGeofenceCircle(center, 0f));
        assertNotNull(GeofenceManager.drawGeofenceCircle(center, 50f));
        assertNotNull(GeofenceManager.drawGeofenceCircle(center, 500f));
    }

    // ── addGeofence – permission denied path (early return) ───────────────────

    @Test
    public void addGeofence_permissionDeniedByDefault_doesNotThrow() {
        // Robolectric does not grant ACCESS_FINE_LOCATION by default,
        // so addGeofence exits at the permission check without calling GMS.
        manager.addGeofence("way/123", 45.497, -73.579, 50f);
    }

    @Test
    public void addGeofence_smallRadius_doesNotThrow() {
        manager.addGeofence("way/456", 45.458, -73.640, 10f);
    }

    @Test
    public void addGeofence_calledMultipleTimes_doesNotThrow() {
        manager.addGeofence("way/001", 45.497, -73.579, 30f);
        manager.addGeofence("way/002", 45.458, -73.640, 30f);
        manager.addGeofence("way/003", 45.495, -73.578, 30f);
    }
}
