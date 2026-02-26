package com.example.oncampusapp;

import androidx.annotation.NonNull;
import java.util.*;

public class BuildingDetails {
    public String code;
    public String name;
    public String address;
    public String image;
    public boolean accessibility;
    public String link;
    public boolean hasDirectTunnelToMetro;
    public Schedule schedule;
    public static class Schedule {
        public boolean alwaysOpen;
        public String monday;
        public String tuesday;
        public String wednesday;
        public String thursday;
        public String friday;
        public String saturday;
        public String sunday;

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
