package com.example.oncampusapp;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Tests for IndoorGraphCache using Robolectric so real Android resources are available.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class IndoorGraphCacheTest {

    // ── Unknown resource → null ───────────────────────────────────────────────

    @Test
    public void getGraph_unknownBuildingId_returnsNull() {
        Context ctx = RuntimeEnvironment.getApplication();
        IndoorGraph result = IndoorGraphCache.getGraph(ctx, "zzz_no_such_building_xyz");
        assertNull(result);
    }

    @Test
    public void getGraph_emptyBuildingId_returnsNull() {
        Context ctx = RuntimeEnvironment.getApplication();
        IndoorGraph result = IndoorGraphCache.getGraph(ctx, "");
        assertNull(result);
    }

    // ── Known resource → non-null and cached ─────────────────────────────────

    @Test
    public void getGraph_knownBuilding_returnsNonNull() {
        Context ctx = RuntimeEnvironment.getApplication();
        // "h" matches res/raw/h.json (Hall building indoor graph)
        IndoorGraph graph = IndoorGraphCache.getGraph(ctx, "h");
        assertNotNull(graph);
    }

    @Test
    public void getGraph_sameId_returnsCachedInstance() {
        Context ctx = RuntimeEnvironment.getApplication();
        IndoorGraph first  = IndoorGraphCache.getGraph(ctx, "h");
        IndoorGraph second = IndoorGraphCache.getGraph(ctx, "h");
        assertNotNull(first);
        assertSame(first, second);
    }
}
