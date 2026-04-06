package com.example.oncampusapp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarRepository {

    private static final String UTF8 = "UTF-8";
    private static final String CALENDAR_LIST_URL =
            "https://www.googleapis.com/calendar/v3/users/me/calendarList";

    /**
     * Fetch all events from all calendars.
     */

    private static CalendarRepository instance;

    private CalendarRepository() {}

    public static CalendarRepository getInstance() {
        if (instance == null) {
            instance = new CalendarRepository();
        }
        return instance;
    }

    public JSONArray fetchAllEvents(String accessToken) throws Exception {

        String calendarListJson = fetchCalendarList(accessToken);
        JSONObject root = new JSONObject(calendarListJson);

        JSONArray calendars = root.optJSONArray("items");
        List<JSONObject> allEvents = new ArrayList<>();

        if (calendars != null) {

            for (int i = 0; i < calendars.length(); i++) {

                JSONObject cal = calendars.getJSONObject(i);
                String calendarId = cal.getString("id");

                String eventsJson = fetchCalendarEvents(accessToken, calendarId);

                JSONObject eventsRoot = new JSONObject(eventsJson);
                JSONArray events = eventsRoot.optJSONArray("items");

                if (events != null) {
                    for (int j = 0; j < events.length(); j++) {
                        allEvents.add(events.getJSONObject(j));
                    }
                }
            }
        }

        return new JSONArray(allEvents);
    }

    /**
     * Fetch user's calendar list
     */
    protected String fetchCalendarList(String accessToken) throws Exception {

        return fetchUrl(
                CALENDAR_LIST_URL,
                accessToken
        );
    }

    public JSONArray refreshEvents(String accessToken) throws Exception {
        return fetchAllEvents(accessToken);
    }

    /**
     * Fetch events for a specific calendar
     */
    protected String fetchCalendarEvents(String accessToken, String calendarId) throws Exception {

        Calendar past = Calendar.getInstance();
        past.add(Calendar.MONTH, -6);

        Calendar future = Calendar.getInstance();
        future.add(Calendar.MONTH, 6);

        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        String timeMin = sdf.format(past.getTime());
        String timeMax = sdf.format(future.getTime());

        String url =
                "https://www.googleapis.com/calendar/v3/calendars/"
                        + URLEncoder.encode(calendarId, UTF8)
                        + "/events"
                        + "?singleEvents=true"
                        + "&orderBy=startTime"
                        + "&timeMin=" + URLEncoder.encode(timeMin, UTF8)
                        + "&timeMax=" + URLEncoder.encode(timeMax, UTF8);

        return fetchUrl(url, accessToken);
    }

    /**
     * Generic GET request
     */
    protected String fetchUrl(String urlStr, String accessToken) throws Exception {

        URL url = new URL(urlStr);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + responseCode + " from Google API");
        }

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(conn.getInputStream()))) {

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}