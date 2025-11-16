package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "my_prefs";
    private static final String KEY_COINS = "coins";
    private static final String KEY_CURRENT = "current_room";

    private static final String ROOM_GYM = "gym";
    private static final String ROOM_STUDY = "study";
    private static final String ROOM_BEDROOM = "bedroom";

    private ImageView imgRoomBg, imgProfile;
    private TextView tvCoins;
    private ImageButton btnChangeRoom;

    private ImageButton btnShop, btnInvite, btnInventory, btnSettings, btnHomeStatus;

    private long coins = 0L; // أو القيمة اللي تحبها
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
    }

    private void bindViews() {
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
        coins = prefs.getLong(KEY_COINS, coins);
        currentRoom = prefs.getString(KEY_CURRENT, currentRoom);
        tvCoins.setText(String.valueOf(coins));

        timeSpent.put(ROOM_GYM, prefs.getLong("time_" + ROOM_GYM, 0L));
        timeSpent.put(ROOM_STUDY, prefs.getLong("time_" + ROOM_STUDY, 0L));
        timeSpent.put(ROOM_BEDROOM, prefs.getLong("time_" + ROOM_BEDROOM, 0L));

        updateRoomBackground(currentRoom);
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
        btnHomeStatus.setOnClickListener(v -> openStatusPage());

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

        currentRoom = room;
        lastRoomEnterTs = now;
        updateRoomBackground(room);
        prefs.edit().putString(KEY_CURRENT, currentRoom).apply();
    }

    private void updateRoomBackground(String room) {
        if (ROOM_GYM.equals(room)) imgRoomBg.setImageResource(R.drawable.bg_gym);
        else if (ROOM_STUDY.equals(room)) imgRoomBg.setImageResource(R.drawable.bg_study);
        else imgRoomBg.setImageResource(R.drawable.bg_bedroom);
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
        Intent i = new Intent(this, RoomStatusActivity.class);
        i.putExtra("room", currentRoom);
        startActivity(i);
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
