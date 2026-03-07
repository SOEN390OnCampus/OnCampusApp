package com.example.oncampusapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.nullable;

public class NotificationSchedulerTest {

    @Mock Context mockContext;
    @Mock AlarmManager mockAlarmManager;

    private final SimpleDateFormat exactTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager);
    }

    @Test
    public void testScheduleNotification_SetsAlarmManagerCorrectly() throws Exception {
        // Event starting in 2 hours
        long futureTimeMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000);
        String futureDateStr = exactTimeFormat.format(new Date(futureTimeMillis));

        JSONObject mockEvent = new JSONObject();
        mockEvent.put("summary", "COMP 346");
        mockEvent.put("location", "H-110");

        JSONObject startObj = new JSONObject();
        startObj.put("dateTime", futureDateStr);
        mockEvent.put("start", startObj);

        NotificationScheduler.scheduleClassNotification(mockContext, mockEvent);

        verify(mockAlarmManager).setExactAndAllowWhileIdle(
                eq(AlarmManager.RTC_WAKEUP),
                anyLong(),
                nullable(PendingIntent.class)
        );
    }

    @Test
    public void testScheduleNotification_NullEvent_DoesNothing() {
        // Execute with null
        NotificationScheduler.scheduleClassNotification(mockContext, null);
        // Verify AlarmManager was NEVER called
        verify(mockContext, org.mockito.Mockito.never()).getSystemService(Context.ALARM_SERVICE);
    }
}