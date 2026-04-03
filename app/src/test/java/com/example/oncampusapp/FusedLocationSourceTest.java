package com.example.oncampusapp;

import android.content.Context;

import com.example.oncampusapp.location.FusedLocationSource;
import com.example.oncampusapp.location.ILocationProvider;
import com.google.android.gms.maps.LocationSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FusedLocationSourceTest {

    @Mock ILocationProvider mockClient;
    @Mock Context mockContext;
    @Mock LocationSource.OnLocationChangedListener mockListener;

    private FusedLocationSource source;

    @Before
    public void setUp() {
        // unitTests.returnDefaultValues = true lets LocationCallback be subclassed on JVM
        source = new FusedLocationSource(mockContext, mockClient);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(source);
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    public void deactivate_callsRemoveLocationUpdates() {
        source.deactivate();
        // verify the ILocationProvider was told to remove updates
        verify(mockClient).removeLocationUpdates(any());
    }

    @Test
    public void deactivate_multipleTimes_doesNotThrow() {
        source.deactivate();
        source.deactivate();
        assertNotNull(source);
    }

    // ── activate ──────────────────────────────────────────────────────────────

    @Test
    public void activate_withPermissionGranted_callsRequestLocationUpdates() {
        // returnDefaultValues: checkSelfPermission returns 0 == PERMISSION_GRANTED
        source.activate(mockListener);
        verify(mockClient).requestLocationUpdates(any(), any(), any());
    }

    @Test
    public void activate_thenDeactivate_doesNotThrow() {
        source.activate(mockListener);
        source.deactivate();
        assertNotNull(source);
    }
}
