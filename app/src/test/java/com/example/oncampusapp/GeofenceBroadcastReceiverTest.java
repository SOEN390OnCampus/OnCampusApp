package com.example.oncampusapp;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for GeofenceBroadcastReceiver.
 * Uses Robolectric so that Android context and Intent are real.
 * GeofencingEvent.fromIntent(null) returns null → exercises the null-guard early-return.
 * GeofencingEvent.fromIntent(emptyIntent) returns an event with hasError()=true → same guard.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class GeofenceBroadcastReceiverTest {

    private GeofenceBroadcastReceiver receiver;
    private Context context;

    @Before
    public void setUp() {
        receiver = new GeofenceBroadcastReceiver();
        context  = RuntimeEnvironment.getApplication();
    }

    // ── constructor ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(receiver);
    }

    // ── onReceive — null / error-event paths ──────────────────────────────────

    @Test
    public void onReceive_nullIntent_doesNotThrow() {
        receiver.onReceive(context, null);
        assertNotNull(receiver);
    }

    @Test
    public void onReceive_emptyIntent_doesNotThrow() {
        receiver.onReceive(context, new Intent());
        assertNotNull(receiver);
    }

    @Test
    public void onReceive_intentWithNoExtras_doesNotThrow() {
        Intent intent = new Intent("com.google.android.gms.location.Geofence.ACTION_GEOFENCE_TRANSITION");
        receiver.onReceive(context, intent);
        assertNotNull(receiver);
    }

    // ── onReceive — ENTER / EXIT transition paths ─────────────────────────────

    @Test
    public void onReceive_enterTransition_startsLocationTrackingService() {
        try (MockedStatic<GeofencingEvent> mocked = mockStatic(GeofencingEvent.class)) {
            GeofencingEvent mockEvent = mock(GeofencingEvent.class);
            when(mockEvent.hasError()).thenReturn(false);
            when(mockEvent.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_ENTER);
            mocked.when(() -> GeofencingEvent.fromIntent(any())).thenReturn(mockEvent);

            receiver.onReceive(context, new Intent());

            Intent started = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService();
            assertNotNull("Expected a service to be started on ENTER transition", started);
            assertEquals(LocationTrackingService.class.getName(),
                    started.getComponent().getClassName());
        }
    }

    @Test
    public void onReceive_exitTransition_doesNotStartService() {
        try (MockedStatic<GeofencingEvent> mocked = mockStatic(GeofencingEvent.class)) {
            GeofencingEvent mockEvent = mock(GeofencingEvent.class);
            when(mockEvent.hasError()).thenReturn(false);
            when(mockEvent.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT);
            mocked.when(() -> GeofencingEvent.fromIntent(any())).thenReturn(mockEvent);

            receiver.onReceive(context, new Intent());

            // EXIT calls stopService, not startForegroundService
            assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).peekNextStartedService());
        }
    }

    @Test
    public void onReceive_unknownTransition_doesNotStartOrStopService() {
        try (MockedStatic<GeofencingEvent> mocked = mockStatic(GeofencingEvent.class)) {
            GeofencingEvent mockEvent = mock(GeofencingEvent.class);
            when(mockEvent.hasError()).thenReturn(false);
            when(mockEvent.getGeofenceTransition()).thenReturn(-1); // unknown transition
            mocked.when(() -> GeofencingEvent.fromIntent(any())).thenReturn(mockEvent);

            receiver.onReceive(context, new Intent());

            assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).peekNextStartedService());
        }
    }
}
