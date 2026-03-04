package com.example.oncampusapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NotificationScheduler {

    // A map to store SGW Building prefixes and their full names
    // A master map to store all building prefixes and their full names across both campuses
    private static final Map<String, String> ALL_BUILDINGS = new HashMap<String, String>() {{
        // --- SGW CAMPUS ---
        put("B", "Bishop Annex");
        put("CI", "CI Annex");
        put("CL", "CL Annex");
        put("D", "D Annex");
        put("EN", "EN Annex");
        put("ER", "ER Building");
        put("EV", "Engineering & Visual Arts (EV)");
        put("FA", "FA Annex");
        put("FB", "Faubourg Building (FB)");
        put("FG", "Faubourg Ste-Catherine (FG)");
        put("GA", "Grey Nuns Annex");
        put("GM", "Guy-De Maisonneuve Building");
        put("GN", "Grey Nuns Building");
        put("GS", "GS Building");
        put("H", "Henry F. Hall Building (H)");
        put("K", "K Annex");
        put("LB", "J.W. McConnell Building (LB)");
        put("LD", "LD Building");
        put("LS", "Learning Square Building");
        put("M", "M Annex");
        put("MB", "John Molson Building (MB)");
        put("MI", "MI Annex");
        put("MU", "MU Annex");
        put("P", "P Annex");
        put("PR", "PR Annex");
        put("Q", "Q Annex");
        put("R", "R Annex");
        put("RR", "RR Annex");
        put("S", "S Annex");
        put("SB", "Samuel Bronfman Building");
        put("T", "T Annex");
        put("TD", "Toronto-Dominion Building");
        put("V", "V Annex");
        put("VA", "Visual Arts Building");
        put("X", "X Annex");
        put("Z", "Z Annex");

        // --- LOYOLA CAMPUS ---
        put("AD", "Administration Building");
        put("BB", "BB Annex");
        put("BH", "BH Annex");
        put("CC", "Central Building");
        put("CJ", "Communication Studies and Journalism Building");
        put("DO", "Stinger Dome");
        put("FC", "F.C. Smith Building");
        put("GE", "Centre for Structural and Functional Genomics");
        put("HA", "Hingston Hall, wing HA");
        put("HB", "Hingston Hall, wing HB");
        put("HC", "Hingston Hall, wing HC");
        put("HU", "Applied Science Hub");
        put("JR", "Jesuit Residence");
        put("PC", "PERFORM Centre");
        put("PS", "Physical Services Building");
        put("PT", "Oscar Peterson Concert Hall");
        put("PY", "Psychology Building");
        put("QA", "Quadrangle");
        put("RA", "Recreation and Athletics Complex");
        put("RF", "Loyola Jesuit Hall and Conference Centre");
        put("SC", "Student Centre");
        put("SH", "Future Buildings Laboratory");
        put("SI", "St. Ignatius of Loyola Church");
        put("SP", "Richard J. Renaud Science Complex");
        put("TA", "Terrebonne Building");
        put("VE", "Vanier Extension");
        put("VL", "Vanier Library Building");
    }};

    public static void scheduleClassNotification(Context context, JSONObject nextEvent) {
        if (nextEvent == null) return;

        try {
            String title = nextEvent.optString("summary", "Class");
            String rawLocation = nextEvent.optString("location", "");
            String description = nextEvent.optString("description", "");

            // 1. SMART LOCATION PARSING
            String searchText = (title + " " + rawLocation + " " + description).toUpperCase();
            String displayLocation = "TBD";
            String searchLower = searchText.toLowerCase();

            // Check for Online
            if (searchLower.contains("zoom.us") || searchLower.contains("teams.microsoft") ||
                    searchLower.contains("meet.google") || searchLower.contains("online")) {
                displayLocation = "Online";
            }
            else {
                boolean found = false;
                // Loop through our SGW map to find a match
                for (Map.Entry<String, String> entry : ALL_BUILDINGS.entrySet()) {
                    String prefix = entry.getKey();
                    // Regex: Look for the prefix at a word boundary, optional dash, then digits
                    if (searchText.matches(".*\\b" + prefix + "[-\\s]?\\d+.*")) {
                        displayLocation = entry.getValue();

                        // Try to append room number (Grabs the digits following the prefix)
                        String[] parts = searchText.split("\\b" + prefix + "[-\\s]?");
                        if (parts.length > 1) {
                            String roomNum = parts[1].replaceAll("[^0-9]", "");
                            if (!roomNum.isEmpty()) {
                                displayLocation += " - Room " + roomNum.substring(0, Math.min(roomNum.length(), 4));
                            }
                        }
                        found = true;
                        break;
                    }
                }

                if (!found && !rawLocation.isEmpty()) {
                    displayLocation = rawLocation;
                }
            }

            // 2. ALARM TIMING
            String dateTimeStr = nextEvent.getJSONObject("start").getString("dateTime");
            SimpleDateFormat exactTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            Date eventDate = exactTimeFormat.parse(dateTimeStr);
            if (eventDate == null) return;

            long alarmTimeMillis = eventDate.getTime() - (15 * 60 * 1000);

            if (alarmTimeMillis > System.currentTimeMillis()) {
                Intent intent = new Intent(context, NotificationReceiver.class);
                intent.putExtra("event_title", title);
                intent.putExtra("event_location", displayLocation);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
                    Log.d("AlarmSetup", "Notification scheduled for: " + displayLocation);
                }
            }

        } catch (Exception e) {
            Log.e("NotificationScheduler", "Error scheduling notification", e);
        }
    }
}