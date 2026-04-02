package com.example.oncampusapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class NotificationTimeFormatterTest {

    private static final long MINUTE = 60_000L;
    private static final long HOUR   = 60 * MINUTE;

    // ── Ongoing class ─────────────────────────────────────────────────────────

    @Test
    public void ongoing_atExactStart_returnsOngoing() {
        long start = 1_000_000L;
        long end   = start + HOUR;
        assertEquals("Class is ongoing",
                NotificationTimeFormatter.getBannerTimeStatus(start, start, end));
    }

    @Test
    public void ongoing_midClass_returnsOngoing() {
        long start = 1_000_000L;
        long end   = start + HOUR;
        assertEquals("Class is ongoing",
                NotificationTimeFormatter.getBannerTimeStatus(start + 30 * MINUTE, start, end));
    }

    @Test
    public void ongoing_atExactEnd_returnsOngoing() {
        long start = 1_000_000L;
        long end   = start + HOUR;
        assertEquals("Class is ongoing",
                NotificationTimeFormatter.getBannerTimeStatus(end, start, end));
    }

    // ── Class has ended ───────────────────────────────────────────────────────

    @Test
    public void ended_afterEndTime_returnsEnded() {
        long start = 1_000_000L;
        long end   = start + HOUR;
        assertEquals("Class has ended",
                NotificationTimeFormatter.getBannerTimeStatus(end + 1, start, end));
    }

    @Test
    public void ended_longAfterEnd_returnsEnded() {
        long start = 1_000_000L;
        long end   = start + HOUR;
        assertEquals("Class has ended",
                NotificationTimeFormatter.getBannerTimeStatus(end + 2 * HOUR, start, end));
    }

    // ── Upcoming: minutes countdown ───────────────────────────────────────────

    @Test
    public void upcoming_exactly30MinsBefore_showsMinutes() {
        long start = 1_000_000L + 30 * MINUTE;
        long end   = start + HOUR;
        String result = NotificationTimeFormatter.getBannerTimeStatus(1_000_000L, start, end);
        assertEquals("Next class starting in 30 mins", result);
    }

    @Test
    public void upcoming_1MinBefore_showsMinutes() {
        long start = 1_000_000L + MINUTE;
        long end   = start + HOUR;
        String result = NotificationTimeFormatter.getBannerTimeStatus(1_000_000L, start, end);
        assertEquals("Next class starting in 1 mins", result);
    }

    // ── Upcoming: seconds countdown ───────────────────────────────────────────

    @Test
    public void upcoming_under1Min_showsSeconds() {
        long now   = 1_000_000L;
        long start = now + 45_000L; // 45 seconds away
        long end   = start + HOUR;
        String result = NotificationTimeFormatter.getBannerTimeStatus(now, start, end);
        assertEquals("Next class starting in 45 secs", result);
    }

    @Test
    public void upcoming_1SecondBefore_showsSeconds() {
        long now   = 1_000_000L;
        long start = now + 1_000L;
        long end   = start + HOUR;
        String result = NotificationTimeFormatter.getBannerTimeStatus(now, start, end);
        assertEquals("Next class starting in 1 secs", result);
    }
}
