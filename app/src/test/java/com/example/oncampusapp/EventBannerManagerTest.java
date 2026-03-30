package com.example.oncampusapp;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for EventBannerManager.
 * Robolectric is required because the field initialiser creates a real Handler,
 * which needs a Looper backed by the Android runtime.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class EventBannerManagerTest {

    private MapsActivity mockActivity;
    private EventBannerManager manager;

    @Before
    public void setUp() {
        mockActivity = mock(MapsActivity.class);
        manager = new EventBannerManager(mockActivity);
    }

    @After
    public void tearDown() {
        // Prevent static state leaking between tests
        CalendarEventManager.globalEventsJson = null;
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(manager);
    }

    @Test
    public void constructor_withNullActivity_doesNotThrow() {
        // The constructor only stores the reference; no NPE at construction time
        assertNotNull(new EventBannerManager(null));
    }

    // ── start / stop ──────────────────────────────────────────────────────────

    @Test
    public void start_doesNotThrow() {
        manager.start();
    }

    @Test
    public void stop_doesNotThrow() {
        manager.stop();
    }

    @Test
    public void stopAfterStart_doesNotThrow() {
        manager.start();
        manager.stop();
    }

    @Test
    public void startCalledTwice_doesNotThrow() {
        manager.start();
        manager.start();
    }

    @Test
    public void stopWithoutStart_doesNotThrow() {
        // removeCallbacks on an idle handler is a no-op
        manager.stop();
        manager.stop();
    }

    // ── setRoutePickerOpen ────────────────────────────────────────────────────

    @Test
    public void setRoutePickerOpen_true_doesNotThrow() {
        manager.setRoutePickerOpen(true);
    }

    @Test
    public void setRoutePickerOpen_false_doesNotThrow() {
        manager.setRoutePickerOpen(false);
    }

    @Test
    public void setRoutePickerOpen_toggleMultipleTimes_doesNotThrow() {
        manager.setRoutePickerOpen(true);
        manager.setRoutePickerOpen(false);
        manager.setRoutePickerOpen(true);
    }

    // ── checkAndDisplayNextEventBanner – early-exit paths ─────────────────────

    @Test
    public void checkAndDisplayNextEventBanner_nullEventsJson_doesNotTouchActivity() {
        CalendarEventManager.globalEventsJson = null;
        manager.checkAndDisplayNextEventBanner();
        verify(mockActivity, never()).findViewById(anyInt());
    }

    @Test
    public void checkAndDisplayNextEventBanner_emptyEventsJson_doesNotTouchActivity() {
        CalendarEventManager.globalEventsJson = "";
        manager.checkAndDisplayNextEventBanner();
        verify(mockActivity, never()).findViewById(anyInt());
    }

    @Test
    public void checkAndDisplayNextEventBanner_routePickerOpen_doesNotTouchActivity() {
        // Route picker open → immediate return before any activity interaction
        CalendarEventManager.globalEventsJson = "{\"items\":[]}";
        manager.setRoutePickerOpen(true);
        manager.checkAndDisplayNextEventBanner();
        verify(mockActivity, never()).findViewById(anyInt());
    }

    @Test
    public void checkAndDisplayNextEventBanner_routePickerClosed_nullJson_doesNotTouchActivity() {
        CalendarEventManager.globalEventsJson = null;
        manager.setRoutePickerOpen(false);
        manager.checkAndDisplayNextEventBanner();
        verify(mockActivity, never()).findViewById(anyInt());
    }

    @Test
    public void checkAndDisplayNextEventBanner_calledMultipleTimes_doesNotThrow() {
        CalendarEventManager.globalEventsJson = null;
        manager.checkAndDisplayNextEventBanner();
        manager.checkAndDisplayNextEventBanner();
        manager.checkAndDisplayNextEventBanner();
    }
}
