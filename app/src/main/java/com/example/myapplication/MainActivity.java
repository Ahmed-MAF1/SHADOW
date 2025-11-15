package com.example.myapplication; // عدّل للبكج بتاعك

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private ImageView imgRoomBg, imgProfile;
    private TextView tvCoins, tvUsername;
    private Button  btnChangeRoom;
    private ImageButton btnShop, btnInvite, btnInventory, btnSettings, btnHomeStatus;


    private long coins = 99999;
    private String currentRoom = "gym";
    private HashMap<String, Long> timeSpent = new HashMap<>();
    private long lastRoomEnterTs = 0L;


    private SharedPreferences prefs;
    private static final String PREFS = "my_prefs";
    private static final String KEY_COINS = "coins";
    private static final String KEY_CURRENT = "current_room";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);


        imgRoomBg = findViewById(R.id.imgRoomBg);
        imgProfile = findViewById(R.id.imgProfile);
        tvCoins = findViewById(R.id.tvCoins);
        tvUsername = findViewById(R.id.tvUsername);
        btnChangeRoom = findViewById(R.id.btnChangeRoom);
        btnShop = findViewById(R.id.btnShop);
        btnInvite = findViewById(R.id.btnInvite);
        btnInventory = findViewById(R.id.btnInventory);
        btnSettings = findViewById(R.id.btnSettings);
        btnHomeStatus = findViewById(R.id.btnHomeStatus);


        coins = prefs.getLong(KEY_COINS, coins);
        currentRoom = prefs.getString(KEY_CURRENT, currentRoom);
        updateCoinsUI();
        switchToRoom(currentRoom);


        imgProfile.setOnClickListener(v -> openProfile());
        btnChangeRoom.setOnClickListener(v -> cycleRooms());
        btnShop.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ShopActivity.class)));
        btnInvite.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, InviteActivity.class)));
        btnInventory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, InventoryActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        btnHomeStatus.setOnClickListener(v -> openStatusPage());

        // For testing: long press on coins to increase
        tvCoins.setOnLongClickListener(v -> {
            addCoins(10);
            return true;
        });

        // init timeSpent keys
        timeSpent.put("gym", 0L);
        timeSpent.put("study", 0L);
        timeSpent.put("bedroom", 0L);
        lastRoomEnterTs = System.currentTimeMillis();
    }

    private void switchToRoom(String room) {
        if (room.equals(currentRoom)) return;

        // save spent time of previous room
        long now = System.currentTimeMillis();
        long delta = now - lastRoomEnterTs;
        timeSpent.put(currentRoom, timeSpent.getOrDefault(currentRoom, 0L) + delta);

        // set new
        currentRoom = room;
        lastRoomEnterTs = now;

        // change background image and selected button style
        if (room.equals("gym")) {
            imgRoomBg.setImageResource(R.drawable.bg_gym);

        } else if (room.equals("study")) {
            imgRoomBg.setImageResource(R.drawable.bg_study);

        } else {
            imgRoomBg.setImageResource(R.drawable.bg_bedroom);
        }

        // save current room and coins
        prefs.edit().putString(KEY_CURRENT, currentRoom).apply();
    }

    private void cycleRooms() {
        if (currentRoom.equals("gym")) switchToRoom("study");
        else if (currentRoom.equals("study")) switchToRoom("bedroom");
        else switchToRoom("gym");
    }

    private void addCoins(long amount) {
        coins += amount;
        prefs.edit().putLong(KEY_COINS, coins).apply();
        updateCoinsUI();
    }

    private void updateCoinsUI() {
        tvCoins.setText(String.valueOf(coins));
    }

    private void openProfile() {
        Intent i = new Intent(this, ProfileActivity.class);
        // pass data (timeSpent) as simple extras if needed, or read shared prefs in ProfileActivity
        startActivity(i);
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

        for (String key : timeSpent.keySet()) {
            prefs.edit().putLong("time_" + key, timeSpent.get(key)).apply();
        }
    }
}
