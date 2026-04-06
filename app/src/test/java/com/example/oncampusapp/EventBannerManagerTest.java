package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        CalendarEventManager.setGlobalEventsJson(null);
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
        assertNotNull(manager);
    }

    @Test
    public void stop_doesNotThrow() {
        manager.stop();
        assertNotNull(manager);
    }

    @Test
    public void stopAfterStart_doesNotThrow() {
        manager.start();
        manager.stop();
        assertNotNull(manager);
    }

    @Test
    public void startCalledTwice_doesNotThrow() {
        manager.start();
        manager.start();
        assertNotNull(manager);
    }

    @Test
    public void stopWithoutStart_doesNotThrow() {
        // removeCallbacks on an idle handler is a no-op
        manager.stop();
        manager.stop();
        assertNotNull(manager);
    }

    // ── setRoutePickerOpen ────────────────────────────────────────────────────

    @Test
    public void setRoutePickerOpen_true_doesNotThrow() {
        manager.setRoutePickerOpen(true);
        assertNotNull(manager);
    }

    @Test
    public void setRoutePickerOpen_false_doesNotThrow() {
        manager.setRoutePickerOpen(false);
        assertNotNull(manager);
    }

    @Test
    public void setRoutePickerOpen_toggleMultipleTimes_doesNotThrow() {
        manager.setRoutePickerOpen(true);
        manager.setRoutePickerOpen(false);
        manager.setRoutePickerOpen(true);
        assertNotNull(manager);
    }

    // ── checkAndDisplayNextEventBanner – early-exit paths ─────────────────────

    @Test
    public void checkAndDisplayNextEventBanner_nullEventsJson_doesNotTouchActivity() {
        CalendarEventManager.setGlobalEventsJson(null);
        manager.checkAndDisplayNextEventBanner();
        verify(mockActivity, never()).findViewById(anyInt());
    }

    @Test
    public void checkAndDisplayNextEventBanner_emptyEventsJson_doesNotTouchActivity() {
        CalendarEventManager.setGlobalEventsJson("");
        manager.checkAndDisplayNextEventBanner();
        verify(mockActivity, never()).findViewById(anyInt());
    }

    @Test
    public void checkAndDisplayNextEventBanner_routePickerOpen_doesNotTouchActivity() {
        // Route picker open → immediate return before any activity interaction
        CalendarEventManager.setGlobalEventsJson("{\"items\":[]}");
        manager.setRoutePickerOpen(true);
        manager.checkAndDisplayNextEventBanner();
        verify(mockActivity, never()).findViewById(anyInt());
    }

    @Test
    public void checkAndDisplayNextEventBanner_routePickerClosed_nullJson_doesNotTouchActivity() {
        CalendarEventManager.setGlobalEventsJson(null);
        manager.setRoutePickerOpen(false);
        manager.checkAndDisplayNextEventBanner();
        verify(mockActivity, never()).findViewById(anyInt());
    }

    @Test
    public void checkAndDisplayNextEventBanner_calledMultipleTimes_doesNotThrow() {
        CalendarEventManager.setGlobalEventsJson(null);
        manager.checkAndDisplayNextEventBanner();
        manager.checkAndDisplayNextEventBanner();
        manager.checkAndDisplayNextEventBanner();
        assertNotNull(manager);
    }

    // ── checkAndDisplayNextEventBanner – bannerView interaction ───────────────

    /** Empty event array → nextClass = null, bannerView != null → hides banner. */
    @Test
    public void checkAndDisplayNextEventBanner_noEvents_hidesBannerView() {
        View mockBannerView = mock(View.class);
        doReturn(mockBannerView).when(mockActivity).findViewById(R.id.included_banner);
        CalendarEventManager.setGlobalEventsJson("[]"); // valid JSON array, no events
        manager.checkAndDisplayNextEventBanner();
        verify(mockBannerView).setVisibility(View.GONE);
    }

    /** Upcoming event, but bannerView.findViewById returns null → early return at null guard. */
    @Test
    public void checkAndDisplayNextEventBanner_upcomingEvent_nullTitleView_returnsEarly() {
        CalendarEventManager.setGlobalEventsJson(buildEventJson(30, 90));
        View mockBannerView = mock(View.class);
        doReturn(mockBannerView).when(mockActivity).findViewById(R.id.included_banner);
        // bannerView.findViewById returns null by default → titleView == null → return
        manager.checkAndDisplayNextEventBanner();
        // Must not crash; bannerView.setVisibility should NOT have been called
        verify(mockBannerView, never()).setVisibility(anyInt());
    }

    /** Event starting in 30 min (within the 60-min window) → banner shown. */
    @Test
    public void checkAndDisplayNextEventBanner_eventWithin60Min_showsBanner() {
        CalendarEventManager.setGlobalEventsJson(buildEventJson(30, 90));
        View mockBannerView    = mock(View.class);
        TextView mockTitleView  = mock(TextView.class);
        TextView mockDetailsView = mock(TextView.class);
        doReturn(mockBannerView).when(mockActivity).findViewById(R.id.included_banner);
        doReturn(mockTitleView).when(mockBannerView).findViewById(R.id.banner_event_title);
        doReturn(mockDetailsView).when(mockBannerView).findViewById(R.id.banner_event_details);

        manager.checkAndDisplayNextEventBanner();

        verify(mockBannerView).setVisibility(View.VISIBLE);
        verify(mockTitleView).setText(anyString());
    }

    /** Event starting in 90 min (beyond the 60-min window) → banner hidden. */
    @Test
    public void checkAndDisplayNextEventBanner_eventBeyond60Min_hidesBanner() {
        CalendarEventManager.setGlobalEventsJson(buildEventJson(90, 150));
        View mockBannerView    = mock(View.class);
        TextView mockTitleView  = mock(TextView.class);
        TextView mockDetailsView = mock(TextView.class);
        doReturn(mockBannerView).when(mockActivity).findViewById(R.id.included_banner);
        doReturn(mockTitleView).when(mockBannerView).findViewById(R.id.banner_event_title);
        doReturn(mockDetailsView).when(mockBannerView).findViewById(R.id.banner_event_details);

        manager.checkAndDisplayNextEventBanner();

        verify(mockBannerView).setVisibility(View.GONE);
    }

    /** Currently-ongoing event (start in past, end in future) → banner shown. */
    @Test
    public void checkAndDisplayNextEventBanner_ongoingEvent_showsBanner() {
        CalendarEventManager.setGlobalEventsJson(buildEventJson(-30, 30));
        View mockBannerView    = mock(View.class);
        TextView mockTitleView  = mock(TextView.class);
        TextView mockDetailsView = mock(TextView.class);
        doReturn(mockBannerView).when(mockActivity).findViewById(R.id.included_banner);
        doReturn(mockTitleView).when(mockBannerView).findViewById(R.id.banner_event_title);
        doReturn(mockDetailsView).when(mockBannerView).findViewById(R.id.banner_event_details);

        manager.checkAndDisplayNextEventBanner();

        verify(mockBannerView).setVisibility(View.VISIBLE);
    }

    // ── resolveOnlineLabel (private static) ──────────────────────────────────

    @Test
    public void resolveOnlineLabel_zoom_returnsZoomMeeting() throws Exception {
        assertEquals("ZOOM MEETING", invokeResolveOnlineLabel("zoom.us/meeting", ""));
    }

    @Test
    public void resolveOnlineLabel_teams_returnsMicrosoftTeams() throws Exception {
        assertEquals("MICROSOFT TEAMS", invokeResolveOnlineLabel("teams.microsoft.com", ""));
    }

    @Test
    public void resolveOnlineLabel_googleMeet_returnsGoogleMeet() throws Exception {
        assertEquals("GOOGLE MEET", invokeResolveOnlineLabel("", "join at meet.google.com/abc"));
    }

    @Test
    public void resolveOnlineLabel_emptyLocation_returnsOnlineClass() throws Exception {
        assertEquals("ONLINE CLASS", invokeResolveOnlineLabel("", ""));
    }

    @Test
    public void resolveOnlineLabel_nonEmptyLocation_returnsUppercase() throws Exception {
        assertEquals("ONLINE ROOM", invokeResolveOnlineLabel("Online Room", "no platform hint"));
    }

    private static String invokeResolveOnlineLabel(String rawLocation, String description)
            throws Exception {
        Method method = EventBannerManager.class.getDeclaredMethod(
                "resolveOnlineLabel", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, rawLocation, description);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Builds a one-element JSON array with an event offset by the given minutes. */
    private static String buildEventJson(int startOffsetMinutes, int endOffsetMinutes) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        long now = System.currentTimeMillis();
        String start = fmt.format(new Date(now + (long) startOffsetMinutes * 60_000));
        String end   = fmt.format(new Date(now + (long) endOffsetMinutes   * 60_000));
        return "[{\"summary\":\"SOEN 390\","
                + "\"start\":{\"dateTime\":\"" + start + "\"},"
                + "\"end\":{\"dateTime\":\"" + end + "\"},"
                + "\"location\":\"H-110\"}]";
    }
}
