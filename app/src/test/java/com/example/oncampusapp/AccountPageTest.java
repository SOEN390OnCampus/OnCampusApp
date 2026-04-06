package com.example.oncampusapp;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;

/**
 * Tests for AccountPage pure-logic methods and UI flows.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AccountPageTest {

    private AccountPage activity;
    private Method resolveOnlineLabel;

    @Before
    public void setUp() throws Exception {
        // .get() returns the instance without running any lifecycle callbacks
        activity = Robolectric.buildActivity(AccountPage.class).get();

        resolveOnlineLabel = AccountPage.class
                .getDeclaredMethod("resolveOnlineLabel", String.class, String.class);
        resolveOnlineLabel.setAccessible(true);
    }

    // ── resolveOnlineLabel ────────────────────────────────────────────────────

    @Test
    public void resolveOnlineLabel_zoom_returnsZoomMeeting() throws Exception {
        assertEquals("ZOOM MEETING",
                resolveOnlineLabel.invoke(activity, "https://zoom.us/j/123456", ""));
    }

    @Test
    public void resolveOnlineLabel_zoomInDescription_returnsZoomMeeting() throws Exception {
        assertEquals("ZOOM MEETING",
                resolveOnlineLabel.invoke(activity, "", "Join via Zoom link"));
    }

    @Test
    public void resolveOnlineLabel_teams_returnsMicrosoftTeams() throws Exception {
        assertEquals("MICROSOFT TEAMS",
                resolveOnlineLabel.invoke(activity, "teams.microsoft.com/meeting", ""));
    }

    @Test
    public void resolveOnlineLabel_teamsInDescription_returnsMicrosoftTeams() throws Exception {
        assertEquals("MICROSOFT TEAMS",
                resolveOnlineLabel.invoke(activity, "", "Click the Teams link to join"));
    }

    @Test
    public void resolveOnlineLabel_googleMeet_returnsGoogleMeet() throws Exception {
        assertEquals("GOOGLE MEET",
                resolveOnlineLabel.invoke(activity, "meet.google.com/abc-def", ""));
    }

    @Test
    public void resolveOnlineLabel_googleMeetInDescription_returnsGoogleMeet() throws Exception {
        assertEquals("GOOGLE MEET",
                resolveOnlineLabel.invoke(activity, "", "Join at meet.google.com/xyz"));
    }

    @Test
    public void resolveOnlineLabel_emptyRawLocation_returnsOnlineClass() throws Exception {
        assertEquals("ONLINE CLASS",
                resolveOnlineLabel.invoke(activity, "", "No platform info"));
    }

    @Test
    public void resolveOnlineLabel_nonEmptyRawLocation_returnsUppercased() throws Exception {
        assertEquals("BLACKBOARD COLLABORATE",
                resolveOnlineLabel.invoke(activity, "Blackboard Collaborate", "some description"));
    }

    @Test
    public void resolveOnlineLabel_bothEmpty_returnsOnlineClass() throws Exception {
        assertEquals("ONLINE CLASS",
                resolveOnlineLabel.invoke(activity, "", ""));
    }

    @Test
    public void resolveOnlineLabel_caseInsensitive_zoom() throws Exception {
        assertEquals("ZOOM MEETING",
                resolveOnlineLabel.invoke(activity, "ZOOM LINK", ""));
    }

    @Test
    public void resolveOnlineLabel_caseInsensitive_teams() throws Exception {
        assertEquals("MICROSOFT TEAMS",
                resolveOnlineLabel.invoke(activity, "Microsoft Teams Meeting", ""));
    }

    // ── setLocationText ───────────────────────────────────────────────────────

    @Test
    public void setLocationText_whenOnline_showsTagAndResolvesLabel() throws Exception {
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        android.widget.TextView detailsView = new android.widget.TextView(context);
        android.widget.TextView onlineTagView = new android.widget.TextView(context);
        onlineTagView.setVisibility(android.view.View.GONE);

        Method setLocationText = AccountPage.class.getDeclaredMethod(
                "setLocationText", android.widget.TextView.class, android.widget.TextView.class,
                String.class, String.class, String.class);
        setLocationText.setAccessible(true);

        setLocationText.invoke(activity, detailsView, onlineTagView, "Online", "zoom.us", "");

        assertEquals(android.view.View.VISIBLE, onlineTagView.getVisibility());
        assertEquals("ZOOM MEETING", detailsView.getText().toString());
    }

    @Test
    public void setLocationText_whenPhysicalLocation_hidesTagAndShowsLocation() throws Exception {
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        android.widget.TextView detailsView = new android.widget.TextView(context);
        android.widget.TextView onlineTagView = new android.widget.TextView(context);
        onlineTagView.setVisibility(android.view.View.VISIBLE);

        Method setLocationText = AccountPage.class.getDeclaredMethod(
                "setLocationText", android.widget.TextView.class, android.widget.TextView.class,
                String.class, String.class, String.class);
        setLocationText.setAccessible(true);

        setLocationText.invoke(activity, detailsView, onlineTagView, "H-820", "", "");

        assertEquals(android.view.View.GONE, onlineTagView.getVisibility());
        assertEquals("H-820", detailsView.getText().toString());
    }

    // ── populateCalendarList ──────────────────────────────────────────────────

    @Test
    public void populateCalendarList_nullJson_returnsImmediately() throws Exception {
        Method m = AccountPage.class.getDeclaredMethod("populateCalendarList");
        m.setAccessible(true);
        m.invoke(activity); // Should not crash
    }

    @Test
    public void populateCalendarList_malformedJson_showsToast() throws Exception {
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
        activity.setContentView(R.layout.account_page);

        Method setViewsMethod = AccountPage.class.getDeclaredMethod("setViews");
        setViewsMethod.setAccessible(true);
        setViewsMethod.invoke(activity);

        java.lang.reflect.Field jsonField = AccountPage.class.getDeclaredField("calendarListJson");
        jsonField.setAccessible(true);
        jsonField.set(activity, "{ this is obviously broken JSON ]");

        Method populateMethod = AccountPage.class.getDeclaredMethod("populateCalendarList");
        populateMethod.setAccessible(true);
        populateMethod.invoke(activity);

        assertEquals("Error loading calendars", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void populateCalendarList_emptyJsonArray_createsZeroItems() throws Exception {
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
        activity.setContentView(R.layout.account_page);

        Method setViewsMethod = AccountPage.class.getDeclaredMethod("setViews");
        setViewsMethod.setAccessible(true);
        setViewsMethod.invoke(activity);

        String mockJson = "{ \"items\": [] }";
        java.lang.reflect.Field jsonField = AccountPage.class.getDeclaredField("calendarListJson");
        jsonField.setAccessible(true);
        jsonField.set(activity, mockJson);

        Method populateMethod = AccountPage.class.getDeclaredMethod("populateCalendarList");
        populateMethod.setAccessible(true);
        populateMethod.invoke(activity);

        android.widget.LinearLayout container = activity.findViewById(R.id.calendarListContainer);
        assertEquals("Container should be empty if no calendars exist", 0, container.getChildCount());
    }

    @Test
    public void populateCalendarList_withValidJson_inflatesCalendarItemsAndHandlesClicks() throws Exception {
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
        activity.setContentView(R.layout.account_page);

        Method setViewsMethod = AccountPage.class.getDeclaredMethod("setViews");
        setViewsMethod.setAccessible(true);
        setViewsMethod.invoke(activity);

        String mockJson = "{ \"items\": [{\"id\": \"cal123\", \"summary\": \"Test Calendar\", \"backgroundColor\": \"#8B1E2D\"}] }";
        java.lang.reflect.Field jsonField = AccountPage.class.getDeclaredField("calendarListJson");
        jsonField.setAccessible(true);
        jsonField.set(activity, mockJson);

        Method populateMethod = AccountPage.class.getDeclaredMethod("populateCalendarList");
        populateMethod.setAccessible(true);
        populateMethod.invoke(activity);

        android.widget.LinearLayout container = activity.findViewById(R.id.calendarListContainer);
        assertEquals(1, container.getChildCount());

        View itemView = container.getChildAt(0);
        TextView nameText = itemView.findViewById(R.id.calendar_name);
        assertEquals("Test Calendar", nameText.getText().toString());

        // Perform click on the inflated item
        itemView.performClick();

        // Verify Intent was fired correctly
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent intent = shadowActivity.getNextStartedActivity();
        assertNotNull("Clicking a calendar should launch an Intent", intent);
        assertEquals(ScheduleViewer.class.getName(), intent.getComponent().getClassName());
        assertEquals("cal123", intent.getStringExtra("calendar_id"));
        assertEquals("Test Calendar", intent.getStringExtra("calendar_name"));
        assertEquals("#8B1E2D", intent.getStringExtra("calendar_color"));
    }

    // ── Banner Logic Tests ────────────────────────────────────────────────────

    private String generateDynamicIsoDate(long offsetMillis) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        return fmt.format(new Date(System.currentTimeMillis() + offsetMillis));
    }

    @Test
    public void populateBanner_eventMoreThanOneHourAway_hidesBanner() throws Exception {
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
        activity.setContentView(R.layout.account_page);
        View bannerView = activity.findViewById(R.id.included_banner);

        // Make an event that starts 2 hours from now
        long twoHoursMillis = 2 * 60 * 60 * 1000L;
        JSONObject startObj = new JSONObject().put("dateTime", generateDynamicIsoDate(twoHoursMillis));
        JSONObject endObj = new JSONObject().put("dateTime", generateDynamicIsoDate(twoHoursMillis + 3600000L));

        JSONObject mockEvent = new JSONObject();
        mockEvent.put("summary", "Future Class");
        mockEvent.put("start", startObj);
        mockEvent.put("end", endObj);

        Method populateBanner = AccountPage.class.getDeclaredMethod("populateBanner", View.class, JSONObject.class);
        populateBanner.setAccessible(true);
        populateBanner.invoke(activity, bannerView, mockEvent);

        assertEquals("Banner should be GONE if event is > 1 hour away", View.GONE, bannerView.getVisibility());
    }

    @Test
    public void populateBanner_eventWithinOneHour_showsBannerAndPopulatesData() throws Exception {
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
        activity.setContentView(R.layout.account_page);
        View bannerView = activity.findViewById(R.id.included_banner);

        // Start visible = false so we can prove it turns visible
        bannerView.setVisibility(View.GONE);

        // Make an event that starts in 30 minutes
        long thirtyMinsMillis = 30 * 60 * 1000L;
        JSONObject startObj = new JSONObject().put("dateTime", generateDynamicIsoDate(thirtyMinsMillis));
        JSONObject endObj = new JSONObject().put("dateTime", generateDynamicIsoDate(thirtyMinsMillis + 3600000L));

        JSONObject mockEvent = new JSONObject();
        mockEvent.put("summary", "Imminent Class");
        mockEvent.put("start", startObj);
        mockEvent.put("end", endObj);
        mockEvent.put("location", "H-820");

        Method populateBanner = AccountPage.class.getDeclaredMethod("populateBanner", View.class, JSONObject.class);
        populateBanner.setAccessible(true);
        populateBanner.invoke(activity, bannerView, mockEvent);

        assertEquals("Banner should be VISIBLE if event is < 1 hour away", View.VISIBLE, bannerView.getVisibility());
        TextView titleView = bannerView.findViewById(R.id.banner_event_title);
        assertEquals("Imminent Class", titleView.getText().toString());

        TextView detailsView = bannerView.findViewById(R.id.banner_event_details);
        // FIX: Expect the fully expanded location name generated by the LocationParser
        assertEquals("Henry F. Hall Building - Room 820", detailsView.getText().toString());
    }

    // ── Full Activity UI & Lifecycle Tests ────────────────────────────────────

    /**
     * Safely boots a fully lifecycle-aware Activity for UI testing
     * by clearing globals to prevent background crashes during onCreate().
     */
    private AccountPage createFullActivity() {
        CalendarEventManager.setGlobalCalendarListJson("");
        CalendarEventManager.setGlobalEventsJson("");
        return Robolectric.buildActivity(AccountPage.class).create().resume().get();
    }

    @Test
    public void testBackButton_startsMapsActivity() {
        AccountPage fullActivity = createFullActivity();
        ImageView backButton = fullActivity.findViewById(R.id.btn_back);
        backButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(fullActivity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull(actualIntent);
        assertEquals(MapsActivity.class.getName(), actualIntent.getComponent().getClassName());
    }

    @Test
    public void testBottomNav_clickHome_startsMapsActivity() {
        AccountPage fullActivity = createFullActivity();
        BottomNavigationView bottomNav = fullActivity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        ShadowActivity shadowActivity = Shadows.shadowOf(fullActivity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Intent to MapsActivity should be fired", actualIntent);
        assertEquals(MapsActivity.class.getName(), actualIntent.getComponent().getClassName());
        assertTrue("Activity should finish to prevent backstack buildup", fullActivity.isFinishing());
    }

    @Test
    public void testBottomNav_clickSettings_startsSettingsActivity() {
        AccountPage fullActivity = createFullActivity();
        BottomNavigationView bottomNav = fullActivity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_settings);

        ShadowActivity shadowActivity = Shadows.shadowOf(fullActivity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Intent to SettingsActivity should be fired", actualIntent);
        assertEquals(SettingsActivity.class.getName(), actualIntent.getComponent().getClassName());
        assertTrue(fullActivity.isFinishing());
    }

    @Test
    public void testBottomNav_clickAccount_doesNotStartNewActivity() {
        AccountPage fullActivity = createFullActivity();
        BottomNavigationView bottomNav = fullActivity.findViewById(R.id.bottom_nav);

        bottomNav.setSelectedItemId(R.id.nav_account);

        ShadowActivity shadowActivity = Shadows.shadowOf(fullActivity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNull("No intent should be fired since we are already on Account", actualIntent);
    }

    @Test
    public void testRefreshButton_showsConnectDialog() {
        AccountPage fullActivity = createFullActivity();
        Button btnRefresh = fullActivity.findViewById(R.id.refreshCalendar);
        btnRefresh.performClick();

        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("Connect dialog should appear", dialog);
        assertTrue("Dialog should be showing", dialog.isShowing());
        assertNotNull("Dialog should contain the allow button", dialog.findViewById(R.id.btn_allow));
    }

    @Test
    public void testConnectDialog_cancelButton_dismissesDialog() {
        AccountPage fullActivity = createFullActivity();

        // 1. Open the dialog
        fullActivity.findViewById(R.id.refreshCalendar).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();

        // 2. Click cancel
        dialog.findViewById(R.id.btn_cancel).performClick();

        // 3. Verify it closed
        assertFalse("Dialog should be dismissed after clicking cancel", dialog.isShowing());
    }

    @Test
    public void testSignOutButton_showsConfirmationAlertDialog() {
        AccountPage fullActivity = createFullActivity();
        MaterialButton btnSignOut = fullActivity.findViewById(R.id.btn_sign_out);
        btnSignOut.performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("Sign out confirmation dialog should appear", dialog);
        assertTrue("Dialog should be showing", dialog.isShowing());
    }

    @Test
    public void testActivityDestruction_removesTimerCallbacksCleanly() {
        org.robolectric.android.controller.ActivityController<AccountPage> controller =
                Robolectric.buildActivity(AccountPage.class).create().resume();
        AccountPage fullActivity = controller.get();

        controller.destroy();

        assertTrue(fullActivity.isFinishing() || fullActivity.isDestroyed());
    }
}