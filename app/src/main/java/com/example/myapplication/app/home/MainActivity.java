package com.example.myapplication.app.home;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.example.myapplication.app.profile.ProfileActivity;
import com.example.myapplication.app.auth.LoginActivity;
import com.example.myapplication.app.core.AppUser;
import com.example.myapplication.app.core.BaseActivity;
import com.example.myapplication.app.core.FirestoreHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends BaseActivity {

    private static final String ROOM_GYM = "gym", ROOM_STUDY = "study", ROOM_BEDROOM = "bedroom";
    private static final String PREF_SOLO_TIMER = "solo_timer_prefs";
    private static final String KEY_RUNNING = "running", KEY_PAUSED = "paused",
            KEY_REMAINING = "remaining_ms", KEY_INITIAL = "initial_ms";

    private DocumentReference userRef;
    private ListenerRegistration userListener;

    private String currentRoom = ROOM_GYM;
    private boolean isTimerRunning = false, isTimerPaused = false;
    private long remainingTimeInMillis = 0L, initialTimeInMillis = 0L;

    private CountDownTimer countDownTimer;
    private SharedPreferences soloPrefs, coopPrefs;

    private ImageView imgRoomBg, imgProfile;

    private TextView tvCoins, tvUsername, tvTimer;
    private MaterialButton btnMainTimer;
    private View timerOverlay, btnChangeRoom;

    private final FirestoreHelper.SimpleCallback noopCallback = new FirestoreHelper.SimpleCallback() {
        @Override public void onSuccess() { }
        @Override public void onError(String message) { }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soloPrefs = getSharedPreferences(PREF_SOLO_TIMER, MODE_PRIVATE);
        coopPrefs = getSharedPreferences("coop_prefs", MODE_PRIVATE);

        if (FirestoreHelper.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userRef = FirestoreHelper.getUserDoc();

        bindViews();
        initBaseNavigation();
        setupListeners();
        restoreTimerStateFromPrefs();
        attachUserListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    @Override protected int getCurrentNavItemId() { return R.id.btnHomeStatus; }
    @Override protected boolean shouldBlockNavigation() { return isTimerRunning; }
    @Override protected String getNavigationBlockedMessage() {
        return isTimerRunning ? "Give up or finish timer first!" : null;
    }

    private void bindViews() {
        imgProfile = findViewById(R.id.imgProfile);
        imgRoomBg    = findViewById(R.id.imgRoomBg);
        tvCoins      = findViewById(R.id.tvCoins);
        tvUsername   = findViewById(R.id.tvUsername);
        tvTimer      = findViewById(R.id.tvTimer);
        btnMainTimer = findViewById(R.id.btnMainTimer);
        timerOverlay = findViewById(R.id.timerOverlay);
        btnChangeRoom = findViewById(R.id.btnChangeRoom);
    }

    private void setupListeners() {
        imgProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class))
        );

        btnChangeRoom.setOnClickListener(v -> {
            if (isTimerRunning)
                Toast.makeText(this, "End timer before changing room", Toast.LENGTH_SHORT).show();
            else
                showRoomSelectionDialog();
        });

        btnMainTimer.setOnClickListener(v -> handleTimerClick());


        btnMainTimer.setOnLongClickListener(v -> {
            if (isTimerRunning || isTimerPaused) {
                showTimerControlDialog();
                return true;
            }
            return false;
        });
    }

    private void attachUserListener() {
        if (userRef == null) return;
        userListener = userRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;
            AppUser user = AppUser.fromSnapshot(snapshot);
            if (user.username != null) tvUsername.setText(user.username);
            tvCoins.setText(String.valueOf(user.coins));
            if (user.currentRoom != null) {
                currentRoom = user.currentRoom;
                updateRoomBackground();
            }
        });
    }

    // -------------------- Solo timer --------------------

    private void handleTimerClick() {
        if (coopPrefs.getBoolean("timer_running", false)) {
            Toast.makeText(this, "You have an active Co-op timer. Check Invite page.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!isTimerRunning && !isTimerPaused) showTimerSetupDialog();
        else showTimerControlDialog();
    }

    private void startNewTimer(long minutes) {
        if (countDownTimer != null) countDownTimer.cancel();
        initialTimeInMillis = minutes * 60_000L;
        remainingTimeInMillis = initialTimeInMillis;
        isTimerRunning = true;
        isTimerPaused = false;
        saveTimerStateToPrefs();
        showTimerUi("Focusing...");
        startCountDown();
    }

    private void startCountDown() {
        countDownTimer = new CountDownTimer(remainingTimeInMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeInMillis = millisUntilFinished;
                updateTimerText();
                saveTimerStateToPrefs();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                isTimerPaused = false;
                remainingTimeInMillis = 0L;
                saveTimerStateToPrefs();
                hideTimerUi();
                finishSessionSuccess();
            }
        }.start();
    }

    private void pauseTimer() {
        if (!isTimerRunning || countDownTimer == null) return;
        countDownTimer.cancel();
        isTimerRunning = false;
        isTimerPaused = true;
        saveTimerStateToPrefs();
        showTimerUi("Paused");
    }

    private void resumeTimer() {
        if (!isTimerPaused || remainingTimeInMillis <= 0) return;
        isTimerPaused = false;
        isTimerRunning = true;
        saveTimerStateToPrefs();
        showTimerUi("Focusing...");
        startCountDown();
    }

    private void stopTimerWithoutReward() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        isTimerPaused = false;
        remainingTimeInMillis = 0L;
        initialTimeInMillis = 0L;
        clearTimerPrefs();
        hideTimerUi();
    }

    private void finishSessionSuccess() {
        long minutes = initialTimeInMillis / 60_000L;
        long coinsEarned = minutes / 2L;
        if (coinsEarned < 1L) coinsEarned = 1L;

        FirestoreHelper.incrementUserField("coins", coinsEarned, noopCallback);
        FirestoreHelper.incrementUserField("time_" + currentRoom, initialTimeInMillis, noopCallback);

        Toast.makeText(this, "Great job! +" + coinsEarned + " coins", Toast.LENGTH_LONG).show();
    }

    private void updateTimerText() {
        long totalSeconds = remainingTimeInMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void showTimerUi(String label) {
        timerOverlay.setVisibility(View.VISIBLE);
        tvTimer.setVisibility(View.VISIBLE);
        updateTimerText();
        btnMainTimer.setText(label);
    }

    private void hideTimerUi() {
        timerOverlay.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);
        btnMainTimer.setText("Timer");
    }

    // -------------------- Dialog helpers --------------------

    private Dialog createDialog(int layoutRes) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(layoutRes);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    private void showTimerSetupDialog() {
        Dialog dialog = createDialog(R.layout.dialog_timer_setup);

        // نخليهم MaterialButton عشان نقدر نستخدم stroke (outline)
        MaterialButton btn10  = dialog.findViewById(R.id.btnTime10);
        MaterialButton btn25  = dialog.findViewById(R.id.btnTime25);
        MaterialButton btn45  = dialog.findViewById(R.id.btnTime45);
        MaterialButton btn60  = dialog.findViewById(R.id.btnTime60);
        MaterialButton btnStart = dialog.findViewById(R.id.btnStartCustom);

        MaterialButton[] allButtons = { btn10, btn25, btn45, btn60 };

        final long[] selectedMinutes = {25};

        View.OnClickListener presetListener = v -> {
            // 1) شيل الـ outline من كل الأزرار
            for (MaterialButton b : allButtons) {
                b.setStrokeWidth(0);
            }

            // 2) حط outline للزرار اللي اتضغط
            MaterialButton pressed = (MaterialButton) v;
            pressed.setStrokeWidth(4); // سمك الإطار
            pressed.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.accent_green) // لون الإطار
                    )
            );

            // 3) حدّد القيمة المختارة
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

        btnStart.setOnClickListener(v -> {
            dialog.dismiss();
            startNewTimer(selectedMinutes[0]);
        });

        dialog.show();
    }


    private void showTimerControlDialog() {
        Dialog dialog = createDialog(R.layout.dialog_timer_control);

        TextView tvTitle = dialog.findViewById(R.id.tvControlTitle);
        MaterialButton btnPauseResume = dialog.findViewById(R.id.btnPauseResume),
                btnGiveUp = dialog.findViewById(R.id.btnGiveUp);

        if (isTimerPaused) {
            tvTitle.setText("Paused");
            btnPauseResume.setText("Resume ▶️");
        } else {
            tvTitle.setText("Session Active");
            btnPauseResume.setText("Pause ⏸️");
        }

        btnPauseResume.setOnClickListener(v -> {
            if (isTimerPaused) resumeTimer(); else pauseTimer();
            dialog.dismiss();
        });

        btnGiveUp.setOnClickListener(v -> {
            stopTimerWithoutReward();
            Toast.makeText(this, "Session cancelled", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // -------------------- SharedPreferences --------------------

    private void saveTimerStateToPrefs() {
        soloPrefs.edit()
                .putBoolean(KEY_RUNNING, isTimerRunning)
                .putBoolean(KEY_PAUSED, isTimerPaused)
                .putLong(KEY_REMAINING, remainingTimeInMillis)
                .putLong(KEY_INITIAL, initialTimeInMillis)
                .apply();
    }

    private void restoreTimerStateFromPrefs() {
        isTimerRunning = soloPrefs.getBoolean(KEY_RUNNING, false);
        isTimerPaused = soloPrefs.getBoolean(KEY_PAUSED, false);
        remainingTimeInMillis = soloPrefs.getLong(KEY_REMAINING, 0L);
        initialTimeInMillis = soloPrefs.getLong(KEY_INITIAL, 0L);

        updateRoomBackground();

        if (isTimerRunning && remainingTimeInMillis > 0) {
            showTimerUi("Focusing...");
            startCountDown();
        } else if (isTimerPaused && remainingTimeInMillis > 0) {
            showTimerUi("Paused");
        } else {
            clearTimerPrefs();
            hideTimerUi();
        }
    }

    private void clearTimerPrefs() { soloPrefs.edit().clear().apply(); }

    // -------------------- Room background --------------------

    private void showRoomSelectionDialog() {
        Dialog dialog = createDialog(R.layout.dialog_change_room);

        View btnGym = dialog.findViewById(R.id.btnGoGym),
                btnStudy = dialog.findViewById(R.id.btnGoStudy),
                btnBedroom = dialog.findViewById(R.id.btnGoBedroom);

        View.OnClickListener roomClick = v -> {
            int id = v.getId();
            if (id == R.id.btnGoGym) currentRoom = ROOM_GYM;
            else if (id == R.id.btnGoStudy) currentRoom = ROOM_STUDY;
            else if (id == R.id.btnGoBedroom) currentRoom = ROOM_BEDROOM;

            FirestoreHelper.updateUserField("currentRoom", currentRoom, noopCallback);
            updateRoomBackground();
            dialog.dismiss();
        };

        btnGym.setOnClickListener(roomClick);
        btnStudy.setOnClickListener(roomClick);
        btnBedroom.setOnClickListener(roomClick);

        dialog.show();
    }

    private void updateRoomBackground() {
        int resId = R.drawable.bg_gym;
        if (ROOM_STUDY.equals(currentRoom)) resId = R.drawable.bg_study;
        else if (ROOM_BEDROOM.equals(currentRoom)) resId = R.drawable.bg_bedroom;
        imgRoomBg.setImageResource(resId);
    }
}
