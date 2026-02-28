package com.example.oncampusapp;

import androidx.annotation.NonNull;
import java.util.*;

public class BuildingDetails {
    private String code;
    private String name;
    private String address;
    private String image;
    private boolean accessibility;
    private String link;
    private boolean hasDirectTunnelToMetro;
    private Schedule schedule;
    private double lat;
    private double lng;
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getImage() {
        return image;
    }

    public boolean isAccessibility() {
        return accessibility;
    }

    public String getLink() {
        return link;
    }

    public boolean isHasDirectTunnelToMetro() {
        return hasDirectTunnelToMetro;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public static class Schedule {
        private boolean alwaysOpen;
        private String monday;
        private String tuesday;
        private String wednesday;
        private String thursday;
        private String friday;
        private String saturday;
        private String sunday;

        @NonNull
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (alwaysOpen) {
                sb.append("Always Open");
            } else {
                sb.append(groupSchedule());
            }
            return sb.toString();
        }


        public String groupSchedule() {

            if (this.alwaysOpen) {
                return "Always Open";
            }

            List<String> result = new ArrayList<>();

            String[] days = {
                    "monday", "tuesday", "wednesday",
                    "thursday", "friday", "saturday", "sunday"
            };

            String[] hours = {
                    this.monday,
                    this.tuesday,
                    this.wednesday,
                    this.thursday,
                    this.friday,
                    this.saturday,
                    this.sunday
            };

            String startDay = days[0];
            String prevDay = days[0];
            String prevHours = hours[0];

            for (int i = 1; i < days.length; i++) {
                String currentHours = hours[i];

                if (Objects.equals(currentHours, prevHours)) {
                    prevDay = days[i];
                } else {
                    result.add(formatRange(startDay, prevDay, prevHours));
                    startDay = days[i];
                    prevDay = days[i];
                    prevHours = currentHours;
                }
            }

            result.add(formatRange(startDay, prevDay, prevHours));
            StringBuilder sb = new StringBuilder();
            for (String s : result) {
                sb.append(s).append("\n");
            }

            return sb.toString();
        }

        private String formatRange(String start, String end, String hours) {
            String startCap = capitalize(start);
            String endCap = capitalize(end);

            if (start.equals(end)) {
                return startCap + ": " + hours;
            }

            return startCap + "–" + endCap + ": " + hours;
        }

        private String capitalize(String day) {
            return day.substring(0, 1).toUpperCase() + day.substring(1);
        }
    }

}
