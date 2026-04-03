package com.example.oncampusapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

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
}
