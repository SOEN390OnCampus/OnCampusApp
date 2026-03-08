package com.example.oncampusapp;

import java.util.concurrent.TimeUnit;

public class NotificationTimeFormatter {

    public static String getBannerTimeStatus(long currentTimeMillis, long startTimeMillis, long endTimeMillis) {
        if (currentTimeMillis >= startTimeMillis && currentTimeMillis <= endTimeMillis) {
            return "Class is ongoing";
        } else if (currentTimeMillis < startTimeMillis) {
            long diffInMillis = startTimeMillis - currentTimeMillis;
            long diffInMins = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
            long diffInSecs = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60;

            if (diffInMins < 1) {
                return "Next class starting in " + diffInSecs + " secs";
            } else {
                return "Next class starting in " + diffInMins + " mins";
            }
        }
        return "Class has ended";
    }
}