package com.example.oncampusapp;

import androidx.activity.result.ActivityResultLauncher;
import com.google.android.gms.maps.GoogleMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

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

    // ── enableMyLocation ──────────────────────────────────────────────────────
    // returnDefaultValues=true → checkSelfPermission returns 0 == PERMISSION_GRANTED
    // so the "if granted" branch is always taken in unit tests

    @Mock GoogleMap mockMap;

    @Test
    public void enableMyLocation_permissionGranted_callsSetMyLocationEnabled() {
        manager.setMap(mockMap);
        manager.enableMyLocation();
        verify(mockMap).setMyLocationEnabled(true);
    }

    @Test
    public void enableMyLocation_calledTwice_doesNotThrow() {
        manager.setMap(mockMap);
        manager.enableMyLocation();
        manager.enableMyLocation();
    }

    // ── launchPermissionRequest ───────────────────────────────────────────────
    // returnDefaultValues=true → checkSelfPermission returns 0 (GRANTED) for every permission
    // → permissionsToRequest stays empty → launcher.launch() is never called

    @Test
    public void launchPermissionRequest_allPermissionsGranted_doesNotCallLaunch() {
        manager.launchPermissionRequest();
        verify(mockLauncher, never()).launch(any());
    }

    @Test
    public void launchPermissionRequest_multipleTimes_doesNotThrow() {
        manager.launchPermissionRequest();
        manager.launchPermissionRequest();
    }

}
