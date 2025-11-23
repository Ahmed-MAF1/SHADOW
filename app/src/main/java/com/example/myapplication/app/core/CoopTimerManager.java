package com.example.myapplication.app.core;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class CoopTimerManager {

    public interface Callback {
        void updateStatus(String newStatus);
        void onSessionFinished(long initialTimeInMillis);
    }

    private static final String PREF_COOP = "coop_prefs";
    private static final String KEY_RUNNING = "timer_running";
    private static final String KEY_PAUSED  = "timer_paused";
    private static final String KEY_INITIAL = "timer_initial_duration";
    private static final String KEY_LEFT    = "timer_time_left";
    private static final String KEY_END     = "timer_end_time";

    private final Activity activity;
    private final SharedPreferences prefs;
    private final MaterialButton btnCoopTimer;
    private final Callback callback;

    private android.os.CountDownTimer countDownTimer;
    private boolean isTimerRunning = false, isTimerPaused = false;
    private long timeLeftInMillis = 0L, initialTimeInMillis = 0L;

    public CoopTimerManager(Activity activity,
                            SharedPreferences prefs,
                            MaterialButton btnCoopTimer,
                            Callback callback) {
        this.activity = activity;
        this.prefs = prefs != null
                ? prefs
                : activity.getSharedPreferences(PREF_COOP, Context.MODE_PRIVATE);
        this.btnCoopTimer = btnCoopTimer;
        this.callback = callback;
    }

    public void attachButtonListener() {
        btnCoopTimer.setOnClickListener(v -> handleTimerClick());
    }

    public void checkTimerState() {
        boolean wasRunning = prefs.getBoolean(KEY_RUNNING, false);
        boolean wasPaused  = prefs.getBoolean(KEY_PAUSED,  false);
        initialTimeInMillis = prefs.getLong(KEY_INITIAL, 0);
        timeLeftInMillis    = prefs.getLong(KEY_LEFT,    0);
        long endTime        = prefs.getLong(KEY_END,     0);

        if (wasRunning) {
            long now = System.currentTimeMillis();
            if (now < endTime) {
                timeLeftInMillis = endTime - now;
                isTimerRunning = true;
                isTimerPaused  = false;
                createAndStartCountDown();
            } else {
                isTimerRunning = false;
                btnCoopTimer.setText("Done!");
                if (callback != null) callback.onSessionFinished(initialTimeInMillis);
                clearTimerState();
            }
        } else if (wasPaused && timeLeftInMillis > 0) {
            isTimerPaused = true;
            updateTimerButtonText();
        } else {
            btnCoopTimer.setText("Start Timer");
        }
    }

    public void onLeaveRoom() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        isTimerPaused  = false;
        clearTimerState();
        btnCoopTimer.setText("Start Timer");
    }

    public void cleanup() {
        if (countDownTimer != null) countDownTimer.cancel();
    }

    // -------------------- Internal logic --------------------

    private void handleTimerClick() {
        SharedPreferences soloPrefs =
                activity.getSharedPreferences("solo_timer_prefs", Context.MODE_PRIVATE);
        if (soloPrefs.getBoolean("running", false)) {
            Toast.makeText(activity,
                    "You have an active Solo timer in Main menu!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!isTimerRunning && !isTimerPaused) {
            showTimerSetupDialog();
        } else {
            showTimerControlDialog();
        }
    }

    private void showTimerSetupDialog() {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_timer_setup);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // أزرار الوقت
        MaterialButton btn10  = dialog.findViewById(R.id.btnTime10);
        MaterialButton btn25  = dialog.findViewById(R.id.btnTime25);
        MaterialButton btn45  = dialog.findViewById(R.id.btnTime45);
        MaterialButton btn60  = dialog.findViewById(R.id.btnTime60);
        MaterialButton btnStart = dialog.findViewById(R.id.btnStartCustom);

        MaterialButton[] allButtons = { btn10, btn25, btn45, btn60 };

        // القيمة المختارة (افتراضيًا 25)
        final int[] selectedMinutes = {25};

        // helper لتظبيط الـ outline
        View.OnClickListener presetListener = v -> {
            // شيل الـ outline من الكل
            for (MaterialButton b : allButtons) {
                b.setStrokeWidth(0);
            }

            // حط outline على الزرار المضغوط
            MaterialButton pressed = (MaterialButton) v;
            pressed.setStrokeWidth(4);
            int color = androidx.core.content.ContextCompat.getColor(
                    activity,
                    R.color.accent_green
            );
            pressed.setStrokeColor(android.content.res.ColorStateList.valueOf(color));

            int id = v.getId();
            if      (id == R.id.btnTime10) selectedMinutes[0] = 10;
            else if (id == R.id.btnTime25) selectedMinutes[0] = 25;
            else if (id == R.id.btnTime45) selectedMinutes[0] = 45;
            else if (id == R.id.btnTime60) selectedMinutes[0] = 60;
        };

        btn10.setOnClickListener(presetListener);
        btn25.setOnClickListener(presetListener);
        btn45.setOnClickListener(presetListener);
        btn60.setOnClickListener(presetListener);

        // هنا بس يبدأ التيمر بعد ما تختار وتضغط Start
        btnStart.setOnClickListener(v -> {
            startTimer(selectedMinutes[0]);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showTimerControlDialog() {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_timer_control);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle              = dialog.findViewById(R.id.tvControlTitle);
        MaterialButton btnPauseResume = dialog.findViewById(R.id.btnPauseResume);
        MaterialButton btnGiveUp      = dialog.findViewById(R.id.btnGiveUp);

        if (isTimerPaused) {
            tvTitle.setText("Paused");
            btnPauseResume.setText("Resume ▶️");
        } else {
            tvTitle.setText("Focusing");
            btnPauseResume.setText("Pause ⏸️");
        }

        btnPauseResume.setOnClickListener(v -> {
            if (isTimerPaused) resumeTimer(); else pauseTimer();
            dialog.dismiss();
        });

        btnGiveUp.setOnClickListener(v -> {
            stopTimerAndGiveUp();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void startTimer(int minutes) {
        initialTimeInMillis = minutes * 60_000L;
        timeLeftInMillis    = initialTimeInMillis;
        isTimerRunning = true;
        isTimerPaused  = false;

        if (callback != null) {
            callback.updateStatus("Focusing (" + minutes + "m)");
        }
        saveTimerState(true, false);
        createAndStartCountDown();
    }

    private void createAndStartCountDown() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new android.os.CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerButtonText();
                saveTimerState(true, false);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnCoopTimer.setText("Done!");
                if (callback != null) callback.onSessionFinished(initialTimeInMillis);
                clearTimerState();
            }
        }.start();
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerPaused  = true;
        isTimerRunning = false;
        btnCoopTimer.setText("Paused");
        if (callback != null) callback.updateStatus("Paused");
        saveTimerState(false, true);
    }

    private void resumeTimer() {
        isTimerPaused  = false;
        isTimerRunning = true;
        saveTimerState(true, false);
        createAndStartCountDown();

        long minsLeft = timeLeftInMillis / 60_000;
        if (callback != null) {
            callback.updateStatus("Focusing (" + (minsLeft + 1) + "m)");
        }
    }

    private void stopTimerAndGiveUp() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        isTimerPaused  = false;
        btnCoopTimer.setText("Start Timer");
        if (callback != null) callback.updateStatus("Active");
        clearTimerState();
    }

    private void updateTimerButtonText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(),
                "%02d:%02d", minutes, seconds);
        btnCoopTimer.setText(timeFormatted);
    }

    private void saveTimerState(boolean running, boolean paused) {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(KEY_RUNNING, running);
        ed.putBoolean(KEY_PAUSED,  paused);
        ed.putLong(KEY_INITIAL, initialTimeInMillis);
        ed.putLong(KEY_LEFT,    timeLeftInMillis);
        if (running) {
            long endTime = System.currentTimeMillis() + timeLeftInMillis;
            ed.putLong(KEY_END, endTime);
        } else {
            ed.remove(KEY_END);
        }
        ed.apply();
    }

    private void clearTimerState() {
        prefs.edit()
                .putBoolean(KEY_RUNNING, false)
                .putBoolean(KEY_PAUSED,  false)
                .remove(KEY_INITIAL)
                .remove(KEY_LEFT)
                .remove(KEY_END)
                .apply();
    }
}
