package com.example.myapplication.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "my_prefs";
    private static final String KEY_COINS = "coins";
    private static final String KEY_CURRENT = "current_room";

    private static final String ROOM_GYM = "gym";
    private static final String ROOM_STUDY = "study";
    private static final String ROOM_BEDROOM = "bedroom";
    private static final String ROOM_BACKGROUND = "base";

    private static final long COIN_INTERVAL = 120000;

    private ImageView bgImage,imgRoomBg,imgProfile;

    private TextView tvCoins;

    private ImageButton btnShop, btnInvite, btnInventory, btnSettings, btnHomeStatus,btnChangeRoom; //page switching

    private long coins = 0L;
    private String currentRoom = ROOM_GYM;
    private final Map<String, Long> timeSpent = new HashMap<>();
    private long lastRoomEnterTs = 0L;

    private Animation animUp, animDown;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        bindViews();
        loadState();

        lastRoomEnterTs = System.currentTimeMillis();
        initAnimations();
        setupListeners();

        applyBounceEffect(btnShop);
        applyBounceEffect(btnInvite);
        applyBounceEffect(btnInventory);
        applyBounceEffect(btnSettings);
        applyBounceEffect(btnHomeStatus);
        applyBounceEffect(btnChangeRoom);

        tvCoins.setText(String.valueOf(coins));
    }

    private void bindViews() {
        bgImage = findViewById(R.id.bgImage);
        imgRoomBg = findViewById(R.id.imgRoomBg);
        imgProfile = findViewById(R.id.imgProfile);
        tvCoins = findViewById(R.id.tvCoins);
        btnChangeRoom = findViewById(R.id.btnChangeRoom);
        btnShop = findViewById(R.id.btnShop);
        btnInvite = findViewById(R.id.btnInvite);
        btnInventory = findViewById(R.id.btnInventory);
        btnSettings = findViewById(R.id.btnSettings);
        btnHomeStatus = findViewById(R.id.btnHomeStatus);
    }

    private void loadState() {
        coins = prefs.getLong(KEY_COINS, 0L);
        currentRoom = prefs.getString(KEY_CURRENT, currentRoom);

        timeSpent.put(ROOM_GYM, prefs.getLong("time_" + ROOM_GYM, 0L));
        timeSpent.put(ROOM_STUDY, prefs.getLong("time_" + ROOM_STUDY, 0L));
        timeSpent.put(ROOM_BEDROOM, prefs.getLong("time_" + ROOM_BEDROOM, 0L));

        updateBaseBackground();
        updateRoomBackground(currentRoom);

        tvCoins.setText(String.valueOf(coins));
    }

    private void initAnimations() {
        animUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        animDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);
    }

    private void setupListeners() {
        imgProfile.setOnClickListener(v -> openProfile());
        btnChangeRoom.setOnClickListener(v -> cycleRooms());
        btnShop.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)));
        btnInvite.setOnClickListener(v -> startActivity(new Intent(this, InviteActivity.class)));
        btnInventory.setOnClickListener(v -> startActivity(new Intent(this, InventoryActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnHomeStatus.setOnClickListener(v -> {});

        tvCoins.setOnLongClickListener(v -> {
            addCoins(10);
            return true;
        });
    }

    private void applyBounceEffect(ImageButton btn) {
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) v.startAnimation(animUp);
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                v.startAnimation(animDown);
            return false;
        });
    }

    private void switchToRoom(String room) {
        if (room == null || room.equals(currentRoom)) return;

        long now = System.currentTimeMillis();
        long delta = now - lastRoomEnterTs;
        timeSpent.put(currentRoom, timeSpent.getOrDefault(currentRoom, 0L) + delta);
        convertTimeToCoins();
        currentRoom = room;
        lastRoomEnterTs = now;
        updateRoomBackground(room);
        prefs.edit().putString(KEY_CURRENT, currentRoom).apply();
    }

    private void updateBaseBackground() {
        String key = "current_bg_" + ROOM_BACKGROUND;
        String bgId = prefs.getString(key, null);

        int resId;
        if (bgId != null) {
            resId = getResources().getIdentifier(bgId, "drawable", getPackageName());
            if (resId == 0) {
                resId = R.drawable.bg_background;
            }
        } else {
            resId = R.drawable.bg_background;
        }

        bgImage.setImageResource(resId);
    }

    private void updateRoomBackground(String room) {
        String key = "current_bg_" + room;
        String bgId = prefs.getString(key, null);

        int resId;
        if (bgId != null) {
            resId = getResources().getIdentifier(bgId, "drawable", getPackageName());
            if (resId == 0) {
                resId = getDefaultRoomBg(room);
            }
        } else {
            resId = getDefaultRoomBg(room);
        }

        imgRoomBg.setImageResource(resId);
    }

    private int getDefaultRoomBg(String room) {
        if (ROOM_GYM.equals(room)) return R.drawable.bg_gym;
        else if (ROOM_STUDY.equals(room)) return R.drawable.bg_study;
        else if (ROOM_BEDROOM.equals(room)) return R.drawable.bg_bedroom;
        else return R.drawable.bg_gym;
    }

    private void cycleRooms() {
        if (ROOM_GYM.equals(currentRoom)) switchToRoom(ROOM_STUDY);
        else if (ROOM_STUDY.equals(currentRoom)) switchToRoom(ROOM_BEDROOM);
        else switchToRoom(ROOM_GYM);
    }

    private void addCoins(long amount) {
        coins += amount;
        prefs.edit().putLong(KEY_COINS, coins).apply();
        tvCoins.setText(String.valueOf(coins));
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void openStatusPage() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("room", currentRoom);
        startActivity(i);
    }

    private void convertTimeToCoins(){
        long totalTime = timeSpent.getOrDefault(currentRoom, 0L);
        long earnedCoins = totalTime / COIN_INTERVAL;
        if (earnedCoins > 0) {
            addCoins(earnedCoins);
            long remainingTime = totalTime % COIN_INTERVAL;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        long now = System.currentTimeMillis();
        long delta = now - lastRoomEnterTs;
        timeSpent.put(currentRoom, timeSpent.getOrDefault(currentRoom, 0L) + delta);
        lastRoomEnterTs = now;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_COINS, coins);
        editor.putString(KEY_CURRENT, currentRoom);
        for (Map.Entry<String, Long> e : timeSpent.entrySet()) {
            editor.putLong("time_" + e.getKey(), e.getValue());
        }
        editor.apply();
    }
}
