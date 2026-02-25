package com.example.oncampusapp;

import androidx.annotation.NonNull;

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
                sb.append("Mon: ").append(monday).append("\n");
                sb.append("Tue: ").append(tuesday).append("\n");
                sb.append("Wed: ").append(wednesday).append("\n");
                sb.append("Thu: ").append(thursday).append("\n");
                sb.append("Fri: ").append(friday).append("\n");
                sb.append("Sat: ").append(saturday).append("\n");
                sb.append("Sun: ").append(sunday).append("\n");
            }
            return sb.toString();
        }
    }
}
