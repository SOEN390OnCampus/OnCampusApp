package com.example.oncampusapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntSupplier;

public class ScheduleViewer extends AppCompatActivity {

    private LinearLayout headerRow, timeColumn, daysContainer;
    private TextView weekTitle;

    private Map<String, FrameLayout> dayColumns = new HashMap<>();
    private final Calendar currentWeek = Calendar.getInstance();

    private final int START_HOUR = 7; // 7 AM
    private final int END_HOUR = 22;  // 10 PM
    private final int HOUR_HEIGHT_DP = 60; // 1 min = 1 dp height

    private String calendarJson;

    private CalendarRepository calendarRepository;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#8B1E2D"));

        calendarRepository = CalendarRepository.getInstance();

        setContentView(R.layout.activity_schedule);

        headerRow = findViewById(R.id.header_row);

        setContentView(R.layout.activity_schedule);

        headerRow = findViewById(R.id.header_row);
        timeColumn = findViewById(R.id.time_column);
        daysContainer = findViewById(R.id.days_container);
        weekTitle = findViewById(R.id.week_title);
        Button btnMainCalendar = findViewById(R.id.btn_select_main_calendar);
        TextView calendarTitle = findViewById(R.id.calendar_header_title);
        View calendarColorDot = findViewById(R.id.calendar_header_title_color);

        String calendarName = getIntent().getStringExtra("calendar_name");
        String calendarColor = getIntent().getStringExtra("calendar_color");
        String calendarId = getIntent().getStringExtra("calendar_id");
        assert calendarId != null;

        SharedPreferences prefs = getSharedPreferences("OnCampusPrefs", MODE_PRIVATE);
        String selectedCalendarId = prefs.getString("selected_calendar", "");

        if (calendarId.equals(selectedCalendarId))
            btnMainCalendar.setText("Selected ✓");

        btnMainCalendar.setOnClickListener(v -> {
            if (calendarId.equals(selectedCalendarId))
                return;

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("selected_calendar", calendarId);
            editor.apply();

            btnMainCalendar.setText("Selected ✓");
        });

        calendarTitle.setText(calendarName);
        calendarColorDot.setBackgroundColor(Color.parseColor(calendarColor));

        getCalendarEvents();
        setupGrid();

        snapToMonday(currentWeek);
        updateWeek();

        findViewById(R.id.nav_left).setOnClickListener(v -> {
            currentWeek.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeek();
        });

        findViewById(R.id.nav_right).setOnClickListener(v -> {
            currentWeek.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeek();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    public void getCalendarEvents() {
        executor.execute(() -> {
            try {
                String calendarToken = getIntent().getStringExtra("calendar_token");
                String id = getIntent().getStringExtra("calendar_id");

                String calendarEvents = calendarRepository.fetchCalendarEvents(calendarToken, id);
                JSONObject eventsRoot = new JSONObject(calendarEvents);
                JSONArray events = eventsRoot.optJSONArray("items");

                calendarJson = events.toString();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setupGrid() {
        // 1. Build Time Column (Left Side)
        for (int i = START_HOUR; i <= END_HOUR; i++) {
            TextView timeTxt = new TextView(this);
            String amPm = (i < 12 || i == 24) ? "AM" : "PM";
            int displayHour = (i % 12 == 0) ? 12 : (i % 12);

            // Text is forced vertically (Hour on top, AM/PM below)
            timeTxt.setText(displayHour + "\n" + amPm);
            timeTxt.setTextSize(9f);
            timeTxt.setTextColor(Color.parseColor("#999999"));
            timeTxt.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(HOUR_HEIGHT_DP));
            timeTxt.setLayoutParams(lp);
            timeColumn.addView(timeTxt);
        }

        // 2. Build Day Columns (7 Days - Mon to Sun)
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        for (String day : days) {
            // Header cell using layout_weight = 1
            LinearLayout headerCell = new LinearLayout(this);
            headerCell.setOrientation(LinearLayout.VERTICAL);
            headerCell.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            headerCell.setLayoutParams(headerParams);

            TextView dayName = new TextView(this);
            dayName.setTextSize(10f);
            dayName.setTextColor(Color.parseColor("#777777"));

            TextView dayNum = new TextView(this);
            dayNum.setTextSize(12f);
            dayNum.setTextColor(Color.BLACK);

            headerCell.addView(dayName);
            headerCell.addView(dayNum);
            headerCell.setTag(day);
            headerRow.addView(headerCell);

            // FrameLayout for events using layout_weight = 1
            FrameLayout frame = new FrameLayout(this);
            int totalHeight = (END_HOUR - START_HOUR + 1) * HOUR_HEIGHT_DP;
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dpToPx(totalHeight), 1.0f);

            // Faint border to visually separate the columns
            frame.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
            frame.setLayoutParams(flp);

            dayColumns.put(day, frame);
            daysContainer.addView(frame);
        }
    }

    private void updateWeek() {
        updateWeekTitle();
        updateDayHeaders();
        refreshEventsForWeek();
    }

    private void updateWeekTitle() {
        Calendar start = (Calendar) currentWeek.clone();
        Calendar end = (Calendar) currentWeek.clone();
        end.add(Calendar.DAY_OF_MONTH, 6); // Mon-Sun bounds (7 days)

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        String title = monthFormat.format(start.getTime()) + " " + start.get(Calendar.DAY_OF_MONTH) +
                " – " + end.get(Calendar.DAY_OF_MONTH) + ", " + end.get(Calendar.YEAR);
        weekTitle.setText(title);
    }

    private void updateDayHeaders() {
        Calendar temp = (Calendar) currentWeek.clone();
        SimpleDateFormat dayNameFmt = new SimpleDateFormat("EE", Locale.getDefault()); // E.g., "Mo", "Tu"
        SimpleDateFormat dayNumFmt = new SimpleDateFormat("d", Locale.getDefault());

        for (int i = 1; i < headerRow.getChildCount(); i++) { // Skip index 0 (empty spacer)
            LinearLayout cell = (LinearLayout) headerRow.getChildAt(i);
            TextView nameView = (TextView) cell.getChildAt(0);
            TextView numView = (TextView) cell.getChildAt(1);

            nameView.setText(dayNameFmt.format(temp.getTime()).toUpperCase());
            numView.setText(dayNumFmt.format(temp.getTime()));

            temp.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void refreshEventsForWeek() {
        for (FrameLayout column : dayColumns.values()) {
            column.removeAllViews();
        }

        try {
            if(calendarJson == null) return;
            JSONArray eventsArray = new JSONArray(calendarJson);

            Calendar weekStart = (Calendar) currentWeek.clone();
            Calendar weekEnd = (Calendar) currentWeek.clone();
            weekEnd.add(Calendar.DAY_OF_MONTH, 6);

            for (int i = 0; i < eventsArray.length(); i++) {
                JSONObject event = eventsArray.getJSONObject(i);
                String title = event.optString("summary", "No Title");
                String location = event.optString("location", "");
                String colorId = event.optString("colorId", "");

                JSONObject startObj = event.getJSONObject("start");
                JSONObject endObj = event.getJSONObject("end");

                String startIso = startObj.optString("dateTime", startObj.optString("date", ""));
                String endIso = endObj.optString("dateTime", endObj.optString("date", ""));

                Calendar eventDate = parseIsoToCalendar(startIso);
                if (eventDate == null) continue;

                if (!eventDate.before(weekStart) && !eventDate.after(weekEnd)) {
                    String day = getDayOfWeek(startIso);
                    FrameLayout column = dayColumns.get(day);

                    if (column != null) {
                        column.addView(createEventBox(title, location, startIso, endIso, colorId));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View createEventBox(String title, String location, String startIso, String endIso, String colorId) {
        View layout = getLayoutInflater().inflate(R.layout.item_schedule, null);

        TextView titleView = layout.findViewById(R.id.event_title);
        TextView locView = layout.findViewById(R.id.event_location);
        View strip = layout.findViewById(R.id.event_strip);
        View bg = layout.findViewById(R.id.event_bg);

        String[] locationSplitted = location.split(" - ");
        String shortLocation = locationSplitted[locationSplitted.length - 1];

        titleView.setText(title);
        locView.setText(shortLocation);

        String[] colors = getEventColors(colorId);
        String fgColorHex = colors[0];
        String bgColorHex = colors[1];

        // Apply the mapped colors
        strip.setBackgroundColor(Color.parseColor(fgColorHex));
        bg.setBackgroundColor(Color.parseColor(bgColorHex));
        titleView.setTextColor(Color.parseColor(fgColorHex));
        // ---------------------------

        int startHour = getHourFromIso(startIso);
        int startMin = getMinFromIso(startIso);
        int endHour = getHourFromIso(endIso);
        int endMin = getMinFromIso(endIso);

        int topMarginMins = ((startHour - START_HOUR) * 60) + startMin;
        int durationMins = ((endHour - startHour) * 60) + (endMin - startMin);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(durationMins));
        params.topMargin = dpToPx(topMarginMins);

        layout.setLayoutParams(params);
        return layout;
    }

    private void snapToMonday(Calendar cal) {
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    protected String[] getEventColors(String colorId) {
        String fgColorHex;
        String bgColorHex;

        int colorIdVal = ((IntSupplier) () -> {
            try {
                return Integer.parseInt(colorId);
            } catch (Exception e) {
                return 0;
            }
        }).getAsInt();

        bgColorHex = switch (colorIdVal % 11) {
            case 1 -> {
                fgColorHex = "#7986CB";
                yield "#E8EAF6";
            }
            case 2 -> {
                fgColorHex = "#33B679";
                yield "#E8F5E9";
            }
            case 3 -> {
                fgColorHex = "#8E24AA";
                yield "#F3E5F5";
            }
            case 4 -> {
                fgColorHex = "#E67C73";
                yield "#FBE9E7";
            }
            case 5 -> {
                fgColorHex = "#F6BF26";
                yield "#FFFDE7";
            }
            case 6 -> {
                fgColorHex = "#F4511E";
                yield "#FBE9E7";
            }
            case 7 -> {
                fgColorHex = "#039BE5";
                yield "#E1F5FE";
            }
            case 8 -> {
                fgColorHex = "#616161";
                yield "#F5F5F5";
            }
            case 9 -> {
                fgColorHex = "#3F51B5";
                yield "#E8EAF6";
            }
            case 10 -> {
                fgColorHex = "#0B8043";
                yield "#E8F5E9";
            }
            default -> {
                fgColorHex = "#4285F4";
                yield "#DCE6F8";
            }
        };

        return new String[]{fgColorHex, bgColorHex};
    }

    protected int getHourFromIso(String iso) {
        try {
            if(!iso.contains("T")) return 0;
            return Integer.parseInt(iso.split("T")[1].substring(0, 2));
        } catch (Exception e) { return 0; }
    }

    protected int getMinFromIso(String iso) {
        try {
            if(!iso.contains("T")) return 0;
            return Integer.parseInt(iso.split("T")[1].substring(3, 5));
        } catch (Exception e) { return 0; }
    }

    protected int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    protected Calendar parseIsoToCalendar(String iso) {
        try {
            if(!iso.contains("T")) return null;
            String datePart = iso.split("T")[0];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(datePart);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (Exception e) { return null; }
    }

    protected String getDayOfWeek(String isoDateTime) {
        try {
            if(!isoDateTime.contains("T")) return "";
            String datePart = isoDateTime.split("T")[0];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(datePart);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            switch (cal.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.MONDAY: return "monday";
                case Calendar.TUESDAY: return "tuesday";
                case Calendar.WEDNESDAY: return "wednesday";
                case Calendar.THURSDAY: return "thursday";
                case Calendar.FRIDAY: return "friday";
                case Calendar.SATURDAY: return "saturday";
                case Calendar.SUNDAY: return "sunday";
            }
        } catch (Exception e) { return ""; }
        return "";
    }
}