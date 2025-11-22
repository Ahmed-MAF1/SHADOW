package com.example.myapplication.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShopActivity extends AppCompatActivity {
    private static final String PREFS = "my_prefs";
    private static final String KEY_COINS = "coins";
    private static final String ROOM_BACKGROUND = "base";
    private static final String ROOM_GYM = "gym";
    private static final String ROOM_STUDY = "study";
    private static final String ROOM_BEDROOM = "bedroom";
    private static final String KEY_OWNED_PREFIX = "owned_";

    private ImageButton btnShop, btnInvite, btnInventory, btnSettings, btnHomeStatus;  //page switching
    private Animation animUp, animDown; //button effect
    private LinearLayout rowBackground, rowGym, rowStudy, rowBedroom; //page layout
    private SharedPreferences prefs;

    // item description
    static class BgItem {
        String id;
        String room;
        int resId;
        int price;
        BgItem(String id, String room, int resId, int price) {
            this.id = id;
            this.room = room;
            this.resId = resId;
            this.price = price;
        }
    }
    //List that stores all background items
    private final List<BgItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE); // Load app's SharedPreferences file for reading/writing data

        bindViews();
        initAnimations(); // Load animation
        setupBottomNavListeners(); //Switch between pages
        applyBounceEffect(btnShop);
        applyBounceEffect(btnInvite);
        applyBounceEffect(btnInventory);
        applyBounceEffect(btnSettings);
        applyBounceEffect(btnHomeStatus);

        initItems(); //Load all inventory items
        buildShopUi();// Build inventory UI based on owned items
    }

    private void bindViews() {
        // bottom bar
        btnShop = findViewById(R.id.btnShop);
        btnInvite = findViewById(R.id.btnInvite);
        btnInventory = findViewById(R.id.btnInventory);
        btnSettings = findViewById(R.id.btnSettings);
        btnHomeStatus = findViewById(R.id.btnHomeStatus);
        // rows
        rowBackground = findViewById(R.id.rowBackground);
        rowGym = findViewById(R.id.rowGym);
        rowStudy = findViewById(R.id.rowStudy);
        rowBedroom = findViewById(R.id.rowBedroom);
    }
    private void initAnimations() {
        animUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        animDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);
    }
    private void setupBottomNavListeners() {
        btnShop.setOnClickListener(v -> {});

        btnInvite.setOnClickListener(v ->
                startActivity(new Intent(this, InviteActivity.class)));

        btnInventory.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryActivity.class)));

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        btnHomeStatus.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
    }
    // Adds bounce (scale up/down) animation
    private void applyBounceEffect(ImageButton btn) {
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startAnimation(animUp);
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.startAnimation(animDown);
            }
            return false;
        });
    }
    // Add shop items here: new BgItem(id, roomType, drawableResource, price)
    private void initItems() {
        // Background
        items.add(new BgItem("bg_background", ROOM_BACKGROUND, R.drawable.bg_background, 0));
        // Gym
        items.add(new BgItem("bg_gym", ROOM_GYM, R.drawable.bg_gym, 0));
        items.add(new BgItem("im_gym2", ROOM_GYM, R.drawable.im_gym2, 50));
        // Study
        items.add(new BgItem("bg_study", ROOM_STUDY, R.drawable.bg_study, 0));
        items.add(new BgItem("im_study2", ROOM_STUDY, R.drawable.im_study2, 50));
        // Bedroom
        items.add(new BgItem("bg_bedroom", ROOM_BEDROOM, R.drawable.bg_bedroom, 0));
        items.add(new BgItem("im_bedroom2", ROOM_BEDROOM, R.drawable.im_bedroom2, 50));

    }

    // Returns the coin new or 0
    private long getCoins() {
        return prefs.getLong(KEY_COINS, 0L);
    }

    // Saves the updated coin
    private void setCoins(long value) {
        prefs.edit().putLong(KEY_COINS, value).apply();
    }

    private Set<String> getOwnedSet(String room) {
        String key = KEY_OWNED_PREFIX + room;  //key = "owned_gym"
        // HashSet stores unique values only (no duplicates)
        Set<String> def = new HashSet<>();  // Return an empty Set (def) instead of null
        return new HashSet<>(prefs.getStringSet(key, def));
    }

    // Saves the owned item
    private void saveOwnedSet(String room, Set<String> set) {
        String key = KEY_OWNED_PREFIX + room; //key = "owned_gym"
        prefs.edit().putStringSet(key, set).apply();
    }

    private boolean isOwned(BgItem item) {
        Set<String> owned = getOwnedSet(item.room);
        return owned.contains(item.id);
    }
    //Marks the item as owned
    private void markOwned(BgItem item) {
        Set<String> owned = getOwnedSet(item.room);
        owned.add(item.id);
        saveOwnedSet(item.room, owned);
    }

    // Builds the inventory UI by clearing rows and adding only owned items
    private void buildShopUi() {
        rowBackground.removeAllViews();
        rowGym.removeAllViews();
        rowStudy.removeAllViews();
        rowBedroom.removeAllViews();

        for (BgItem item : items) {
            if (isOwned(item)) {
                continue;
            }
            addItemToRow(item);
        }
    }
    // Create and style the item ImageView, then add it to the correct room row
    private void addItemToRow(BgItem item) {
        LinearLayout parent;
        switch (item.room) {
            case ROOM_BACKGROUND:
                parent = rowBackground;
                break;
            case ROOM_GYM:
                parent = rowGym;
                break;
            case ROOM_STUDY:
                parent = rowStudy;
                break;
            case ROOM_BEDROOM:
                parent = rowBedroom;
                break;
            default:
                return;
        }

        ImageView img = new ImageView(this);
        int size = dpToPx(80);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        img.setLayoutParams(lp);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageResource(item.resId);
        img.setBackgroundResource(R.drawable.background_bottom_icon_bg);
        img.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        img.setOnClickListener(v -> onItemClicked(item, v));

        parent.addView(img);
    }

    private void onItemClicked(BgItem item, View view) {
        long coins = getCoins();
        if (coins < item.price) {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
            return;
        }
        // price discount
        coins -= item.price;
        setCoins(coins);
        //it's yours
        markOwned(item);
        // Remove from shop
        ViewGroup parent = (ViewGroup) view.getParent();
        parent.removeView(view);

        Toast.makeText(this, "Purchased!", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }
}
