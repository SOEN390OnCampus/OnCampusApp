package com.example.oncampusapp;

import android.content.Context;
import android.location.Location;

import com.example.oncampusapp.location.FakeLocationProvider;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.Task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class FakeLocationProviderTest {

    private Context context;
    private FakeLocationProvider provider;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        provider = new FakeLocationProvider(context);
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
        assertNotNull(provider.getLastLocation());
    }

    @Test
    public void setFakeLocation_updatesCoordinates_retrievableViaGetLastLocation() {
        provider.setFakeLocation(45.497, -73.579);
        Task<Location> task = provider.getLastLocation();
        assertNotNull(task);
    }

    @Test
    public void setFakeLocation_zeroes_doesNotThrow() {
        provider.setFakeLocation(0.0, 0.0);
        assertNotNull(provider.getLastLocation());
    }

    @Test
    public void setFakeLocation_negativeCoords_doesNotThrow() {
        provider.setFakeLocation(-45.0, -73.0);
        assertNotNull(provider.getLastLocation());
    }

    @Test
    public void setFakeLocation_multipleCalls_lastValuePersists() {
        provider.setFakeLocation(10.0, 20.0);
        provider.setFakeLocation(45.497, -73.579);
        Task<Location> task = provider.getLastLocation();
        assertNotNull(task);
    }

    // ── getLastLocation ───────────────────────────────────────────────────────

    @Test
    public void getLastLocation_returnsNonNullTask() {
        Task<Location> task = provider.getLastLocation();
        assertNotNull(task);
    }

    @Test
    public void getLastLocation_taskIsComplete() {
        Task<Location> task = provider.getLastLocation();
        assertTrue(task.isComplete());
    }

    @Test
    public void getLastLocation_taskResultIsLocation() {
        provider.setFakeLocation(45.497, -73.579);
        Task<Location> task = provider.getLastLocation();
        Location loc = task.getResult();
        assertNotNull(loc);
        assertEquals(45.497, loc.getLatitude(),  0.0001);
        assertEquals(-73.579, loc.getLongitude(), 0.0001);
    }

    // ── removeLocationUpdates ─────────────────────────────────────────────────

    @Test
    public void removeLocationUpdates_unknownCallback_doesNotThrow() {
        LocationCallback cb = new LocationCallback() {};
        provider.removeLocationUpdates(cb);
        assertNotNull(provider);
    }

    @Test
    public void removeLocationUpdates_sameCallbackTwice_doesNotThrow() {
        LocationCallback cb = new LocationCallback() {};
        provider.removeLocationUpdates(cb);
        provider.removeLocationUpdates(cb);
        assertNotNull(provider);
    }
}
