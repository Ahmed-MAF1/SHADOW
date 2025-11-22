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

public class InventoryActivity extends AppCompatActivity {

    private static final String PREFS = "my_prefs";

    private static final String ROOM_BACKGROUND = "base";
    private static final String ROOM_GYM = "gym";
    private static final String ROOM_STUDY = "study";
    private static final String ROOM_BEDROOM = "bedroom";

    private static final String KEY_OWNED_PREFIX = "owned_";
    private static final String KEY_CURRENT_BG_PREFIX = "current_bg_";

    private ImageButton btnShop, btnInvite, btnInventory, btnSettings, btnHomeStatus; // page switching
    private Animation animUp, animDown; // button effect
    private LinearLayout rowInvBase, rowInvGym, rowInvStudy, rowInvBedroom; // page layout

    private SharedPreferences prefs;

    // item description
    static class BgItem {
        String id;
        String room;
        int resId;
        BgItem(String id, String room, int resId) {
            this.id = id;
            this.room = room;
            this.resId = resId;
        }
    }

    //List that stores all background items
    private final List<BgItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE); // Load app's SharedPreferences file for reading/writing data

        bindViews();
        initAnimations(); // Load animation
        setupBottomNavListeners();  //Switch between pages
        applyBounceEffect(btnShop);
        applyBounceEffect(btnInvite);
        applyBounceEffect(btnInventory);
        applyBounceEffect(btnSettings);
        applyBounceEffect(btnHomeStatus);

        initItems();       // Load all inventory items
        buildInventoryUi();// Build inventory UI based on owned items
    }

    private void bindViews() {
        // bottom bar
        btnShop = findViewById(R.id.btnShop);
        btnInvite = findViewById(R.id.btnInvite);
        btnInventory = findViewById(R.id.btnInventory);
        btnSettings = findViewById(R.id.btnSettings);
        btnHomeStatus = findViewById(R.id.btnHomeStatus);
        // rows
        rowInvBase = findViewById(R.id.rowInvBase);
        rowInvGym = findViewById(R.id.rowInvGym);
        rowInvStudy = findViewById(R.id.rowInvStudy);
        rowInvBedroom = findViewById(R.id.rowInvBedroom);
    }

    private void initAnimations() {
        animUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        animDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);
    }

    private void setupBottomNavListeners() {
        btnShop.setOnClickListener(v ->
                startActivity(new Intent(this, ShopActivity.class)));

        btnInvite.setOnClickListener(v ->
                startActivity(new Intent(this, InviteActivity.class)));

        btnInventory.setOnClickListener(v -> {});

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

    // Add inventory items here: new BgItem(id, roomType, drawableResource)
    private void initItems() {
        // Background
        items.add(new BgItem("bg_background", ROOM_BACKGROUND, R.drawable.bg_background));
        // Gym
        items.add(new BgItem("bg_gym", ROOM_GYM, R.drawable.bg_gym));
        items.add(new BgItem("im_gym2", ROOM_GYM, R.drawable.im_gym2));
        // Study
        items.add(new BgItem("bg_study", ROOM_STUDY, R.drawable.bg_study));
        items.add(new BgItem("im_study2", ROOM_STUDY, R.drawable.im_study2));
        // Bedroom
        items.add(new BgItem("bg_bedroom", ROOM_BEDROOM, R.drawable.bg_bedroom));
        items.add(new BgItem("im_bedroom2", ROOM_BEDROOM, R.drawable.im_bedroom2));
    }
    private Set<String> getOwnedSet(String room) {
        String key = KEY_OWNED_PREFIX + room; // key = "owned_gym"
        // HashSet stores unique values only (no duplicates)
        Set<String> def = new HashSet<>(); // Return an empty Set instead of null
        return new HashSet<>(prefs.getStringSet(key, def));
    }

    // Builds the inventory UI by clearing rows and adding only owned items
    private void buildInventoryUi() {
        rowInvBase.removeAllViews();
        rowInvGym.removeAllViews();
        rowInvStudy.removeAllViews();
        rowInvBedroom.removeAllViews();

        for (BgItem item : items) {
            Set<String> owned = getOwnedSet(item.room);
            if (!owned.contains(item.id)) {
                // Not owned don't display in inventory
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
                parent = rowInvBase;
                break;
            case ROOM_GYM:
                parent = rowInvGym;
                break;
            case ROOM_STUDY:
                parent = rowInvStudy;
                break;
            case ROOM_BEDROOM:
                parent = rowInvBedroom;
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

        img.setOnClickListener(v -> onItemClicked(item));

        parent.addView(img);
    }

    private void onItemClicked(BgItem item) {
        String key = KEY_CURRENT_BG_PREFIX + item.room;
        prefs.edit().putString(key, item.id).apply();
        Toast.makeText(this, "Background applied!", Toast.LENGTH_SHORT).show();
    }
    private int dpToPx(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }
}
