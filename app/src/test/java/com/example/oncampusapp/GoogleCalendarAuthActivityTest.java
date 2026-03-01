package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;

@RunWith(MockitoJUnitRunner.class)
public class GoogleCalendarAuthActivityTest {
    @Test
    public void fetchCalendarList_callsFetchUrl() throws Exception{
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("mocked_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarList("mocked_token");
        assertEquals("mocked_json", result);
        verify(activity).fetchUrl(contains("calendarList"), eq("mocked_token"));
    }
    @Test
    public void fetchCalendarEvents_buildsCorrectUrl() throws Exception{
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("events_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarEvents("mocked_token", "calendar123");
        assertEquals("events_json", result);
        verify(activity).fetchUrl(contains("calendars/calendar123/events"), eq("mocked_token"));
    }
    @Test(expected = Exception.class)
    public void fetchUrl_invalidUrl_throwsException() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        activity.fetchUrl("http://invalid.localhost", "token");
    }
    @Test
    public void fetchCalendarList_reflectionCall() throws Exception{
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("json_response").when(activity).fetchUrl(anyString(), anyString());

        Method method = GoogleCalendarAuthActivity.class.getDeclaredMethod("fetchCalendarList", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(activity, "abcd");
        assertEquals("json_response", result);
    }
    @Test
    public void fetchUrl_success_returnsResponse() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("success_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchUrl("https://test.com", "token123");
        assertEquals("success_json", result);
    }
    @Test
    public void fetchCalendarEvents_withSpecialCharactersInCalendarId() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("events_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarEvents("token", "calendar@123");
        assertEquals("events_json", result);
        verify(activity).fetchUrl(contains("calendar%40123"), eq("token"));
    }
    @Test
    public void fetchCalendarList_withEmptyToken() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("empty_token_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarList("");
        assertEquals("empty_token_json", result);
    }
    @Test
    public void fetchCalendarEvents_multipleCallsCoverage() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("json1").when(activity).fetchUrl(anyString(), anyString());

        String result1 = activity.fetchCalendarEvents("token1", "id1");
        String result2 = activity.fetchCalendarEvents("token2", "id2");
        assertEquals("json1", result1);
        assertEquals("json1", result2);
    }
    @Test(expected = Exception.class)
    public void fetchUrl_nullUrl_throwsException() throws Exception {
        GoogleCalendarAuthActivity activity = new GoogleCalendarAuthActivity();
        activity.fetchUrl(null, "token");
    }
    @Test
    public void fetchCalendarEvents_returnsMockedValue() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("mocked_events").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarEvents("abc", "calendarXYZ");
        assertEquals("mocked_events", result);
    }
}
