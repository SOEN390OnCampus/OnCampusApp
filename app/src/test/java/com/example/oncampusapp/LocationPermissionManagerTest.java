package com.example.oncampusapp;

import androidx.activity.result.ActivityResultLauncher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class LocationPermissionManagerTest {

    @Mock MapsActivity mockActivity;
    @Mock ActivityResultLauncher<String[]> mockLauncher;

    private LocationPermissionManager manager;

    @Before
    public void setUp() {
        manager = new LocationPermissionManager(mockActivity, mockLauncher);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(manager);
    }

    @Test
    public void setMap_nullMap_doesNotThrow() {
        // setMap only assigns the field — null assignment is safe
        manager.setMap(null);
    }

    // ── logPermissionResult (static) ──────────────────────────────────────────

    @Test
    public void logPermissionResult_granted_doesNotThrow() {
        LocationPermissionManager.logPermissionResult("ACCESS_FINE_LOCATION", true);
    }

    @Test
    public void logPermissionResult_denied_doesNotThrow() {
        LocationPermissionManager.logPermissionResult("ACCESS_FINE_LOCATION", false);
    }

    @Test
    public void logPermissionResult_nullPermission_doesNotThrow() {
        LocationPermissionManager.logPermissionResult(null, false);
    }

    @Test
    public void logPermissionResult_emptyPermission_doesNotThrow() {
        LocationPermissionManager.logPermissionResult("", true);
    }

    @Test
    public void logPermissionResult_postNotifications_doesNotThrow() {
        LocationPermissionManager.logPermissionResult("POST_NOTIFICATIONS", true);
    }

    @Test
    public void logPermissionResult_coarseLocation_doesNotThrow() {
        LocationPermissionManager.logPermissionResult("ACCESS_COARSE_LOCATION", false);
    }
}
