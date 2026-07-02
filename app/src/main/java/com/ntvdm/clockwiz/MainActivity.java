package com.ntvdm.clockwiz;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private SharedPreferences mPrefs;
    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler = new Handler();
    private int mLastDrawnDay = -1;

    // clock mapping
    private final int[] mNumDrawables = {
            R.drawable.dask_cradle_num_00, R.drawable.dask_cradle_num_01,
            R.drawable.dask_cradle_num_02, R.drawable.dask_cradle_num_03,
            R.drawable.dask_cradle_num_04, R.drawable.dask_cradle_num_05,
            R.drawable.dask_cradle_num_06, R.drawable.dask_cradle_num_07,
            R.drawable.dask_cradle_num_08, R.drawable.dask_cradle_num_09
    };

    // 1 second loop for clock
    private final Runnable mClockTicker = new Runnable() {
        @Override
        public void run() {
            updateClockAndCalendar();
            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                 android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_main);

        mPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

        // avoid time out duh
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "ClockWiz::KeepAwake");
            mWakeLock.acquire();
        }
        // hide bg
        findViewById(R.id.main_background_frame).setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                android.content.Intent i = new android.content.Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mHandler.post(mClockTicker);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mClockTicker);
    }

    private void updateClockAndCalendar() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        View rootFrame = findViewById(R.id.main_background_frame);

        // night/day mod bg switching
        boolean showCal = mPrefs.getBoolean("show_calendar", true);
        findViewById(R.id.calendar_table).setVisibility(showCal ? View.VISIBLE : View.GONE);

        String bgMode = mPrefs.getString("bg_mode", "standard");

        if ("wallpaper".equals(bgMode)) {
            rootFrame.setBackgroundColor(Color.TRANSPARENT);
        } else {
            // only run in bg mode
            if (hour >= 6 && hour < 19) {
                rootFrame.setBackgroundResource(R.drawable.cradle_bg_am_land);
            } else {
                rootFrame.setBackgroundResource(R.drawable.cradle_bg_pm_land);
            }
        }

        // clock digit updater
        String timeStr = new SimpleDateFormat("HHmm", Locale.US).format(cal.getTime());
        ((ImageView) findViewById(R.id.img_h1)).setImageResource(mNumDrawables[timeStr.charAt(0) - '0']);
        ((ImageView) findViewById(R.id.img_h2)).setImageResource(mNumDrawables[timeStr.charAt(1) - '0']);
        ((ImageView) findViewById(R.id.img_m1)).setImageResource(mNumDrawables[timeStr.charAt(2) - '0']);
        ((ImageView) findViewById(R.id.img_m2)).setImageResource(mNumDrawables[timeStr.charAt(3) - '0']);

        // date updater
        TextView dateTextView = (TextView) findViewById(R.id.date_text);
        dateTextView.setText(new SimpleDateFormat("E, dd MMM", Locale.GERMAN).format(cal.getTime()));

        // "RE-DRAW CALENDAR MATRIX"
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        if (currentDay != mLastDrawnDay) {
            buildCalendarGrid(cal);
            mLastDrawnDay = currentDay;
        }
    }

    private void buildCalendarGrid(Calendar currentCal) {
        TableLayout table = (TableLayout) findViewById(R.id.calendar_table);
        table.removeAllViews();

        Calendar workingCal = (Calendar) currentCal.clone();
        int targetDay = workingCal.get(Calendar.DAY_OF_MONTH);
        workingCal.set(Calendar.DAY_OF_MONTH, 1);
        int startDayOfWeek = workingCal.get(Calendar.DAY_OF_WEEK);
        int maxDays = workingCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int cellWidth = (int) (47 * getResources().getDisplayMetrics().density);

        TableRow currentRow = new TableRow(this);
        for (int i = 1; i < startDayOfWeek; i++) {
            TextView blank = new TextView(this);
            blank.setWidth(cellWidth);
            currentRow.addView(blank);
        }

        for (int day = 1; day <= maxDays; day++) {
            if (currentRow.getChildCount() == 7) {
                // row to the table
                table.addView(currentRow);

                // divider line for every row
                addDividerLine(table);

                currentRow = new TableRow(this);
            }

            TextView dayTv = new TextView(this);
            dayTv.setText(String.valueOf(day));
            dayTv.setGravity(Gravity.CENTER);
            dayTv.setWidth(cellWidth);
            dayTv.setPadding(0, 8, 0, 8);
            dayTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, spToPx(20));

            int dayOfWeek = (startDayOfWeek + day - 2) % 7 + 1;
            dayTv.setTextColor(dayOfWeek == Calendar.SUNDAY ? Color.parseColor("#FF4444") : Color.WHITE);

            if (day == targetDay) {
                dayTv.setBackgroundResource(R.drawable.calendar_focus_day);
                dayTv.setTextColor(Color.BLACK);
            }

            currentRow.addView(dayTv);
        }

        if (currentRow.getChildCount() > 0) {
            while (currentRow.getChildCount() < 7) {
                TextView blank = new TextView(this);
                blank.setWidth(cellWidth);
                currentRow.addView(blank);
            }
            table.addView(currentRow);
        }
    }

    // helper to create the divider
    private void addDividerLine(TableLayout table) {
        View divider = new View(this);
        divider.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#40FFFFFF")); // half-transparent white
        table.addView(divider);
    }

    // scaling
    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }
}