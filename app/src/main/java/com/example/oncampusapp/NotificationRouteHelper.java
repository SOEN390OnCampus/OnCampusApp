package com.example.oncampusapp;

import android.util.Log;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class NotificationRouteHelper {

    public static Result resolveRoute(
            String eventsJson,
            JSONObject nextClass,
            Map<String, BuildingDetails> buildingDetailsMap
    ) {
        if (eventsJson == null || eventsJson.isEmpty() || nextClass == null || buildingDetailsMap == null) {
            return null;
        }

        try {
            String destinationBuilding =
                    EventBuildingResolver.extractBuildingNameFromEvent(nextClass, buildingDetailsMap);

            if (destinationBuilding == null || destinationBuilding.isEmpty()) {
                return null;
            }

            JSONObject previousClass = PreviousClassFinder.findPreviousClass(eventsJson, nextClass);

            String previousBuilding = null;

            if (previousClass != null && shouldUsePreviousClass(previousClass, nextClass)) {
                previousBuilding =
                        EventBuildingResolver.extractBuildingNameFromEvent(previousClass, buildingDetailsMap);
            }

            return new Result(destinationBuilding, previousBuilding);

        } catch (Exception e) {
            Log.e("NotificationRouteHelper", "Error resolving route");
            return null;
        }
    }

    private static boolean shouldUsePreviousClass(JSONObject previousClass, JSONObject nextClass) {
        try {
            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

            long previousEnd = format.parse(
                    previousClass.getJSONObject("end").getString("dateTime")
            ).getTime();

            long nextStart = format.parse(
                    nextClass.getJSONObject("start").getString("dateTime")
            ).getTime();

            long diffMinutes = (nextStart - previousEnd) / (60 * 1000);
            return diffMinutes <= 15;

        } catch (Exception e) {
            return false;
        }
    }

    public static class Result {
        private final String destinationBuilding;
        private final String previousBuilding;

        public Result(String destinationBuilding, String previousBuilding) {
            this.destinationBuilding = destinationBuilding;
            this.previousBuilding = previousBuilding;
        }

        public String getDestinationBuilding() {
            return destinationBuilding;
        }

        public String getPreviousBuilding() {
            return previousBuilding;
        }

        public boolean shouldStartFromPreviousBuilding() {
            return previousBuilding != null && !previousBuilding.isEmpty();
        }
    }
}