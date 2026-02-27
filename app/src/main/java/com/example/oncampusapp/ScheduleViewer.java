package com.example.oncampusapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScheduleViewer extends AppCompatActivity {

    private LinearLayout mainRow;
    private Map<String, LinearLayout> dayColumns = new HashMap<>();

    private Calendar currentWeek = Calendar.getInstance();
    private TextView weekTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildLayout(); // Create UI manually

        snapToMonday(currentWeek);
        updateWeekTitle();
        updateDayHeaders();

        refreshEventsForWeek();
    }

    /**
     * Builds a 7-column week layout manually (Google Calendar style)
     */
    private void buildLayout() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // ---------- TOP BAR ----------
        LinearLayout headerBar = new LinearLayout(this);
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setPadding(32,32,32,32);
        headerBar.setBackgroundColor(Color.parseColor("#8B1E2D"));

        TextView leftArrow = new TextView(this);
        leftArrow.setText("◀");
        leftArrow.setTextSize(22f);
        leftArrow.setTextColor(Color.WHITE);

        weekTitle = new TextView(this);
        weekTitle.setTextColor(Color.WHITE);
        weekTitle.setTextSize(18f);
        weekTitle.setPadding(32,0,32,0);

        TextView rightArrow = new TextView(this);
        rightArrow.setText("▶");
        rightArrow.setTextSize(22f);
        rightArrow.setTextColor(Color.WHITE);

        headerBar.addView(leftArrow);
        headerBar.addView(weekTitle);
        headerBar.addView(rightArrow);

        root.addView(headerBar);

        // ---------- WEEK GRID ----------
        ScrollView scrollView = new ScrollView(this);
        mainRow = new LinearLayout(this);
        mainRow.setOrientation(LinearLayout.HORIZONTAL);
        mainRow.setBackgroundColor(Color.parseColor("#F1F3F4"));

        String[] days = {
                "monday","tuesday","wednesday",
                "thursday","friday","saturday","sunday"
        };

        for (String day : days) {

            LinearLayout columnWrapper = new LinearLayout(this);
            columnWrapper.setOrientation(LinearLayout.VERTICAL);
            columnWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

            TextView header = new TextView(this);
            header.setGravity(Gravity.CENTER);
            header.setPadding(8,16,8,16);
            header.setBackgroundColor(Color.parseColor("#E0E0E0"));

            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setPadding(16,16,16,16);

            dayColumns.put(day, column);

            columnWrapper.addView(header);
            columnWrapper.addView(column);

            mainRow.addView(columnWrapper);
        }

        scrollView.addView(mainRow);
        root.addView(scrollView);

        setContentView(root);

        // Set current week to Monday
        snapToMonday(currentWeek);
        updateWeekTitle();
        updateDayHeaders();

        leftArrow.setOnClickListener(v -> {
            currentWeek.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeek();
        });

        rightArrow.setOnClickListener(v -> {
            currentWeek.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeek();
        });
    }

    private void snapToMonday(Calendar cal) {
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void updateWeek() {
        snapToMonday(currentWeek);
        updateWeekTitle();
        updateDayHeaders();
        refreshEventsForWeek();
    }

    private void updateWeekTitle() {

        Calendar start = (Calendar) currentWeek.clone();
        Calendar end = (Calendar) currentWeek.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);

        SimpleDateFormat monthDay = new SimpleDateFormat("MMM d", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

        String title = monthDay.format(start.getTime()) + " - " +
                monthDay.format(end.getTime()) + ", " +
                yearFormat.format(end.getTime());

        weekTitle.setText(title);
    }

    private void updateDayHeaders() {

        Calendar temp = (Calendar) currentWeek.clone();
        SimpleDateFormat dayFormat =
                new SimpleDateFormat("EEE d", Locale.getDefault());

        for (int i = 0; i < 7; i++) {

            LinearLayout columnWrapper =
                    (LinearLayout) mainRow.getChildAt(i);

            TextView header =
                    (TextView) columnWrapper.getChildAt(0);

            header.setText(dayFormat.format(temp.getTime()));

            temp.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    /**
     * Parses JSON and displays events in the correct day column
     */
    private void refreshEventsForWeek() {

        for (LinearLayout column : dayColumns.values()) {
            column.removeAllViews();
        }

        try {
            JSONArray eventsArray = new JSONArray(
                    getIntent().getStringExtra("calendar_events_json"));

            Calendar weekStart = (Calendar) currentWeek.clone();
            Calendar weekEnd = (Calendar) currentWeek.clone();
            weekEnd.add(Calendar.DAY_OF_MONTH, 6);

            for (int i = 0; i < eventsArray.length(); i++) {

                JSONObject event = eventsArray.getJSONObject(i);
                String title = event.optString("summary", "No Title");

                JSONObject startObj = event.getJSONObject("start");
                String start = startObj.optString("dateTime",
                        startObj.optString("date", ""));

                Calendar eventDate = parseIsoToCalendar(start);

                if (eventDate == null) continue;

                if (!eventDate.before(weekStart) && !eventDate.after(weekEnd)) {

                    String day = getDayOfWeek(start);
                    LinearLayout column = dayColumns.get(day);
                    if (column != null) {
                        column.addView(createEventBoxFromXml(title, start, start));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Calendar parseIsoToCalendar(String iso) {
        try {
            if (!iso.contains("T")) return null;

            String datePart = iso.split("T")[0];

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(datePart);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates styled event block
     */
    private LinearLayout createEventBoxFromXml(String title, String start, String end) {
        LinearLayout layout = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.item_schedule, null); // event_item.xml is your XML filename

        TextView titleView = layout.findViewById(R.id.event_title);
        TextView timeView = layout.findViewById(R.id.event_time);

        titleView.setText(title);
        timeView.setText(formatTime(start) + " - " + formatTime(end));

        // Optional: set background programmatically or leave XML styling
        int bgColor;
        int borderColor;

        if (title.contains("MATH")) {
            bgColor = Color.parseColor("#F4E7C5");
            borderColor = Color.parseColor("#F4B400");
        } else if (title.contains("COMP")) {
            bgColor = Color.parseColor("#DCE6F8");
            borderColor = Color.parseColor("#4285F4");
        } else if (title.contains("SOEN")) {
            bgColor = Color.parseColor("#F8D7DA");
            borderColor = Color.parseColor("#DB4437");
        } else {
            bgColor = Color.parseColor("#D4EDDA");
            borderColor = Color.parseColor("#0F9D58");
        }

        layout.setBackground(createRoundedBackground(bgColor, borderColor));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 24, 0, 0);
        layout.setLayoutParams(lp);

        return layout;
    }

    /**
     * Extract HH:mm from ISO datetime
     */
    private String formatTime(String iso) {
        try {
            if (!iso.contains("T")) return "All Day";

            String time = iso.split("T")[1];
            return time.substring(0,5);

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Converts ISO date to lowercase weekday name
     * (API 24 safe)
     */
    private String getDayOfWeek(String isoDateTime) {
        try {
            if (!isoDateTime.contains("T")) return "";

            String datePart = isoDateTime.split("T")[0];

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(datePart);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            switch (dayOfWeek) {
                case Calendar.MONDAY: return "monday";
                case Calendar.TUESDAY: return "tuesday";
                case Calendar.WEDNESDAY: return "wednesday";
                case Calendar.THURSDAY: return "thursday";
                case Calendar.FRIDAY: return "friday";
                case Calendar.SATURDAY: return "saturday";
                case Calendar.SUNDAY: return "sunday";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private android.graphics.drawable.Drawable createRoundedBackground(int bgColor, int borderColor) {

        android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();

        shape.setColor(bgColor);
        shape.setCornerRadius(25f);

        shape.setStroke(8, borderColor); // border thickness

        return shape;
    }
}