package com.example.oncampusapp;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Robolectric tests for ShuttleHelper.init() and post-init getShuttleRoute() branches.
 * Requires a real Android context so the raw JSON resource can be opened.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ShuttleHelperRobolectricTest {

    private static final LatLng NEAR_SGW = new LatLng(45.497163, -73.578535);
    private static final LatLng NEAR_LOY = new LatLng(45.458424, -73.638369);

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        ShuttleHelper.init(context);
    }

    // ── init ─────────────────────────────────────────────────────────────────

    @Test
    public void init_doesNotThrow() {
        // Covered by @Before; assert context is sane
        assertNotNull(context);
    }

    @Test
    public void init_calledTwice_earlyReturnDoesNotThrow() {
        // Second call hits the "already initialised" early-return guard
        ShuttleHelper.init(context);
        // Verify that shuttle route retrieval still works after second init call
        List<LatLng> route = ShuttleHelper.getShuttleRoute(NEAR_SGW);
        assertNotNull(route);
        assertFalse(route.isEmpty());
    }

    // ── getShuttleRoute — after init ──────────────────────────────────────────

    @Test
    public void getShuttleRoute_afterInit_startNearSGW_returnsNonEmptyList() {
        List<LatLng> route = ShuttleHelper.getShuttleRoute(NEAR_SGW);
        assertNotNull(route);
        assertFalse(route.isEmpty());
    }

    @Test
    public void getShuttleRoute_afterInit_startNearLOY_returnsNonEmptyList() {
        List<LatLng> route = ShuttleHelper.getShuttleRoute(NEAR_LOY);
        assertNotNull(route);
        assertFalse(route.isEmpty());
    }

    @Test
    public void getShuttleRoute_afterInit_nullStart_returnsSGWToLOYRoute() {
        // null start → defaults to SGW→LOY direction
        List<LatLng> route = ShuttleHelper.getShuttleRoute(null);
        assertNotNull(route);
        assertFalse(route.isEmpty());
    }

    @Test
    public void getShuttleRoute_sgwStart_returnsDifferentRouteToLOY() {
        List<LatLng> sgwRoute = ShuttleHelper.getShuttleRoute(NEAR_SGW);
        List<LatLng> loyRoute = ShuttleHelper.getShuttleRoute(NEAR_LOY);
        assertNotNull(sgwRoute);
        assertNotNull(loyRoute);
        // The two directions should produce different polylines
        assertNotEquals(sgwRoute.get(0), loyRoute.get(0));
    }

    @Test
    public void getShuttleRoute_afterInit_returnsNewListEachCall() {
        List<LatLng> r1 = ShuttleHelper.getShuttleRoute(NEAR_SGW);
        List<LatLng> r2 = ShuttleHelper.getShuttleRoute(NEAR_SGW);
        assertNotSame(r1, r2); // defensive copy via new ArrayList<>()
        assertEquals(r1, r2);
    }
}
