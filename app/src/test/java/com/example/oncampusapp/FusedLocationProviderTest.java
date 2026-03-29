package com.example.oncampusapp;

import android.content.Context;

import com.example.oncampusapp.location.FusedLocationProvider;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class FusedLocationProviderTest {

    private Context context;
    private FusedLocationProvider provider;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        provider = new FusedLocationProvider(context);
    }

    // ── constructor ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(provider);
    }

    // ── setFakeLocation ───────────────────────────────────────────────────────

    @Test
    public void setFakeLocation_doesNotThrow() {
        provider.setFakeLocation(45.497, -73.579);
    }

    @Test
    public void setFakeLocation_zeroes_doesNotThrow() {
        provider.setFakeLocation(0.0, 0.0);
    }

    @Test
    public void setFakeLocation_negativeCoords_doesNotThrow() {
        provider.setFakeLocation(-90.0, -180.0);
    }

    @Test
    public void setFakeLocation_multipleCalls_doesNotThrow() {
        provider.setFakeLocation(45.497, -73.579);
        provider.setFakeLocation(45.458, -73.640);
        provider.setFakeLocation(0.0, 0.0);
    }

    // ── removeLocationUpdates ─────────────────────────────────────────────────

    @Test
    public void removeLocationUpdates_withUnknownCallback_doesNotThrow() {
        LocationCallback cb = new LocationCallback() {};
        provider.removeLocationUpdates(cb);
    }
}
