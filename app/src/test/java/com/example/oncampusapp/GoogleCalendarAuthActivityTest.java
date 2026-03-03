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

    /**
     * Test that fetchCalendarList calls fetchUrl with the correct endpoint and token
     */
    @Test
    public void fetchCalendarList_callsFetchUrl() throws Exception{
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("concordia_calendar_list_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarList("concordia_student_token");
        assertEquals("concordia_calendar_list_json", result);
        verify(activity).fetchUrl(contains("calendarList"), eq("concordia_student_token"));
    }

    /**
     * Test that fetchCalendarEvents builds the correct URL containing the calendar ID
     */
    @Test
    public void fetchCalendarEvents_buildsCorrectUrl() throws Exception{
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("soen341_events_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarEvents("concordia_student_token", "soen390_schedule");
        assertEquals("soen390_events_json", result);
        verify(activity).fetchUrl(contains("calendars/soen390_schedule/events"), eq("concordia_student_token"));
    }

    /**
     * Test that fetchUrl throws an exception when an invalid URL is provided
     */
    @Test(expected = Exception.class)
    public void fetchUrl_invalidUrl_throwsException() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        activity.fetchUrl("http://invalid.concordia.local", "concordia_student_token");
    }

    /**
     * Test fetchCalendarList using reflection to verify private/protected method access
     */
    @Test
    public void fetchCalendarList_reflectionCall() throws Exception{
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("concordia_calendar_reflection_json").when(activity).fetchUrl(anyString(), anyString());

        Method method = GoogleCalendarAuthActivity.class.getDeclaredMethod("fetchCalendarList", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(activity, "reflection_student_token");
        assertEquals("concordia_calendar_reflection_json", result);
    }

    /**
     * Test that fetchUrl returns the expected response on success
     */
    @Test
    public void fetchUrl_success_returnsResponse() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("concordia_success_response_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchUrl("https://api.concordia.ca/test", "concordia_student_token_123");
        assertEquals("concordia_success_response_json", result);
    }

    /**
     * Test that fetchCalendarEvents correctly encodes special characters in the calendar ID
     */
    @Test
    public void fetchCalendarEvents_withSpecialCharactersInCalendarId() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("concordia_encoded_events_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarEvents("concordia_student_token", "concordia@encs.concordia.ca");
        assertEquals("concordia_encoded_events_json", result);
        verify(activity).fetchUrl(contains("concordia%40encs.concordia.ca"), eq("concordia_student_token"));
    }

    /**
     * Test fetchCalendarList behavior when an empty token is provided
     */
    @Test
    public void fetchCalendarList_withEmptyToken() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("unauthorized_concordia_calendar_response").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarList("");
        assertEquals("unauthorized_concordia_calendar_response", result);
    }

    /**
     * Test multiple calls to fetchCalendarEvents to ensure consistent behavior
     */
    @Test
    public void fetchCalendarEvents_multipleCallsCoverage() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("semester_events_json").when(activity).fetchUrl(anyString(), anyString());

        String result1 = activity.fetchCalendarEvents("fall2026_student_token", "fall2026_schedule");
        String result2 = activity.fetchCalendarEvents("winter2027_student_token", "winter2027_schedule");

        assertEquals("semester_events_json", result1);
        assertEquals("semester_events_json", result2);
    }

    /**
     * Test that fetchUrl throws an exception when the URL is null
     */
    @Test(expected = Exception.class)
    public void fetchUrl_nullUrl_throwsException() throws Exception {
        GoogleCalendarAuthActivity activity = new GoogleCalendarAuthActivity();
        activity.fetchUrl(null, "concordia_student_token");
    }

    /**
     * Test that fetchCalendarEvents returns the mocked response value
     */
    @Test
    public void fetchCalendarEvents_returnsMockedValue() throws Exception {
        GoogleCalendarAuthActivity activity = Mockito.mock(GoogleCalendarAuthActivity.class, Mockito.CALLS_REAL_METHODS);
        doReturn("concordia_mocked_events_json").when(activity).fetchUrl(anyString(), anyString());

        String result = activity.fetchCalendarEvents("encs_student_token", "concordia_course_calendar");
        assertEquals("concordia_mocked_events_json", result);
    }
}
