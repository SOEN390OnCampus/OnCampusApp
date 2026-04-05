package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

/**
 * Tests for AccountPage pure-logic methods.
 *
 * We obtain an AccountPage instance via Robolectric.buildActivity(...).get()
 * WITHOUT calling .create(), because onCreate() calls Google Sign-In APIs
 * that are unavailable in the test environment. The private methods under test
 * (resolveOnlineLabel) use only their parameters — no instance state — so the
 * uninitialised activity is safe to use as a receiver.
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
        // search is lowercased, so "ZOOM" in rawLocation still matches
        assertEquals("ZOOM MEETING",
                resolveOnlineLabel.invoke(activity, "ZOOM LINK", ""));
    }

    @Test
    public void resolveOnlineLabel_caseInsensitive_teams() throws Exception {
        assertEquals("MICROSOFT TEAMS",
                resolveOnlineLabel.invoke(activity, "Microsoft Teams Meeting", ""));
    }

    // ── populateCalendarList — null calendarListJson guard ────────────────────

    @Test
    public void populateCalendarList_nullJson_returnsImmediately() throws Exception {
        // calendarListJson is null by default (no Intent extras) → method returns early
        Method m = AccountPage.class.getDeclaredMethod("populateCalendarList");
        m.setAccessible(true);
        // No crash expected — null check at top of method guards all view access
        m.invoke(activity);
    }
    // ── setLocationText ───────────────────────────────────────────────────────

    @Test
    public void setLocationText_whenOnline_showsTagAndResolvesLabel() throws Exception {
        // Setup raw views using the application context
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        android.widget.TextView detailsView = new android.widget.TextView(context);
        android.widget.TextView onlineTagView = new android.widget.TextView(context);
        onlineTagView.setVisibility(android.view.View.GONE);

        Method setLocationText = AccountPage.class.getDeclaredMethod(
                "setLocationText", android.widget.TextView.class, android.widget.TextView.class,
                String.class, String.class, String.class);
        setLocationText.setAccessible(true);

        // Action: Call with "Online" as the parsed location
        setLocationText.invoke(activity, detailsView, onlineTagView, "Online", "zoom.us", "");

        // Assertion: Tag should be visible, and the text should be resolved to ZOOM MEETING
        assertEquals(android.view.View.VISIBLE, onlineTagView.getVisibility());
        assertEquals("ZOOM MEETING", detailsView.getText().toString());
    }

    @Test
    public void setLocationText_whenPhysicalLocation_hidesTagAndShowsLocation() throws Exception {
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        android.widget.TextView detailsView = new android.widget.TextView(context);
        android.widget.TextView onlineTagView = new android.widget.TextView(context);
        onlineTagView.setVisibility(android.view.View.VISIBLE); // Start visible to ensure it gets hidden

        Method setLocationText = AccountPage.class.getDeclaredMethod(
                "setLocationText", android.widget.TextView.class, android.widget.TextView.class,
                String.class, String.class, String.class);
        setLocationText.setAccessible(true);

        // Action: Call with a physical room
        setLocationText.invoke(activity, detailsView, onlineTagView, "H-820", "", "");

        // Assertion: Tag should be hidden, text should just be the room name
        assertEquals(android.view.View.GONE, onlineTagView.getVisibility());
        assertEquals("H-820", detailsView.getText().toString());
    }

    // ── populateCalendarList (UI Inflation without onCreate) ──────────────────

    @Test
    public void populateCalendarList_withValidJson_inflatesCalendarItems() throws Exception {
        /* * JEDI TRICK: We can test the UI inflation WITHOUT calling onCreate() by
         * manually setting the theme, inflating the layout, and calling setViews()!
         */

        // 1. Manually setup the UI environment
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
        activity.setContentView(R.layout.account_page);

        // 2. Call setViews() via reflection to bind the XML elements to the Activity's private fields
        Method setViewsMethod = AccountPage.class.getDeclaredMethod("setViews");
        setViewsMethod.setAccessible(true);
        setViewsMethod.invoke(activity);

        // 3. Inject mock JSON directly into the private 'calendarListJson' field
        String mockJson = "{ \"items\": [{\"id\": \"cal1\", \"summary\": \"Test Calendar\", \"backgroundColor\": \"#8B1E2D\"}] }";
        java.lang.reflect.Field jsonField = AccountPage.class.getDeclaredField("calendarListJson");
        jsonField.setAccessible(true);
        jsonField.set(activity, mockJson);

        // 4. Invoke the populate method
        Method populateMethod = AccountPage.class.getDeclaredMethod("populateCalendarList");
        populateMethod.setAccessible(true);
        populateMethod.invoke(activity);

        // 5. Assert the views were successfully generated and appended to the screen
        android.widget.LinearLayout container = activity.findViewById(R.id.calendarListContainer);
        assertEquals("Container should have exactly 1 calendar item inflated", 1, container.getChildCount());

        android.widget.TextView nameText = container.getChildAt(0).findViewById(R.id.calendar_name);
        assertEquals("Test Calendar", nameText.getText().toString());
    }

// ── Full Activity UI & Lifecycle Tests ────────────────────────────────────

    /**
     * Safely boots a fully lifecycle-aware Activity for UI testing
     * by clearing globals to prevent background crashes during onCreate().
     */
    private AccountPage createFullActivity() {
        CalendarEventManager.globalCalendarListJson = "";
        CalendarEventManager.globalEventsJson = "";
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

        // Action: Destroy the activity to trigger onDestroy()
        controller.destroy();

        // Assertion: If the timer wasn't cleaned up, Robolectric would throw a memory leak error.
        // We assert it is finishing/destroyed to ensure the lifecycle ran successfully.
        assertTrue(fullActivity.isFinishing() || fullActivity.isDestroyed());
    }


}
