package com.example.oncampusapp;
public class RouteDecisionHelper {

    private RouteDecisionHelper() {
        // To prevent instantiation since it's an utility class
    }
    public static boolean shouldUsePreviousClass(long previousEndMillis, long nextStartMillis) {
        long diffMinutes = (nextStartMillis - previousEndMillis) / (60 * 1000);
        return diffMinutes <= 15;
    }
}
