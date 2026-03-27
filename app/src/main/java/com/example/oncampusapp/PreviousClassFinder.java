package com.example.oncampusapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class PreviousClassFinder {

    public static JSONObject findPreviousClass(String eventsJson, JSONObject targetClass) {
        try {
            JSONArray events = new JSONArray(eventsJson);

            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

            long targetStart = format.parse(
                    targetClass.getJSONObject("start").getString("dateTime")
            ).getTime();

            JSONObject bestPrevious = null;
            long latestEnd = Long.MIN_VALUE;

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);

                if (!event.has("start") || !event.has("end")) continue;
                if (!event.getJSONObject("start").has("dateTime")) continue;
                if (!event.getJSONObject("end").has("dateTime")) continue;

                long eventEnd = format.parse(
                        event.getJSONObject("end").getString("dateTime")
                ).getTime();

                if (eventEnd <= targetStart && eventEnd > latestEnd) {
                    bestPrevious = event;
                    latestEnd = eventEnd;
                }
            }

            return bestPrevious;

        } catch (Exception e) {
            Log.e("PreviousClassFinder", "Error finding previous class");
            return null;
        }
    }
}
