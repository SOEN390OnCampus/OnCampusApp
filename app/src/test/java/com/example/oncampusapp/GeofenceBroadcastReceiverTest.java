package com.example.oncampusapp;

import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

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
}
