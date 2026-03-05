package com.example.oncampusapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationParser {

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

    // We keep a sorted list so longer prefixes (like "SP") are always checked before shorter ones (like "S")
    private static final List<Map.Entry<String, String>> SORTED_BUILDINGS = new ArrayList<>(ALL_BUILDINGS.entrySet());

    static {
        SORTED_BUILDINGS.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
    }

    public static String parseSmartLocation(String title, String rawLocation, String description) {
        if (title == null) title = "";
        if (rawLocation == null) rawLocation = "";
        if (description == null) description = "";

        String searchText = (title + " " + rawLocation + " " + description).toUpperCase();
        String searchLower = searchText.toLowerCase();
        String displayLocation = "TBD";

        // Check for Online
        if (searchLower.contains("zoom.us") || searchLower.contains("teams.microsoft") ||
                searchLower.contains("meet.google") || searchLower.contains("online") || searchLower.contains("zoom")) {
            return "Online";
        }

        boolean found = false;
        for (Map.Entry<String, String> entry : SORTED_BUILDINGS) {
            String prefix = entry.getKey();

            // Smart Regex:
            // \b         : Word boundary
            // prefix     : The building prefix (e.g., SP)
            // [-\s]?     : Optional hyphen or space
            // ( ... )    : Capturing group for the exact room number
            // [A-Z]?     : Optional leading letter (e.g., the 'S' in S110)
            // \d+        : One or more digits
            // [A-Z0-9.]* : Optional trailing characters (handles 2.210 or 110A)
            Pattern pattern = Pattern.compile("\\b" + prefix + "[-\\s]?([A-Z]?\\d+[A-Z0-9.]*)\\b");
            Matcher matcher = pattern.matcher(searchText);

            if (matcher.find()) {
                displayLocation = entry.getValue();
                String roomNum = matcher.group(1);

                if (roomNum != null && !roomNum.isEmpty()) {
                    displayLocation += " - Room " + roomNum;
                }
                found = true;
                break;
            } else if (searchText.matches(".*\\b" + prefix + "\\b.*")) {
                // Fallback: The building is mentioned, but without a specific room number
                displayLocation = entry.getValue();
                found = true;
                break;
            }
        }

        if (!found && !rawLocation.isEmpty()) {
            displayLocation = rawLocation;
        }

        return displayLocation;
    }
}