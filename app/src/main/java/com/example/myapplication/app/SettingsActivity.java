package com.example.myapplication.app;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnShop, btnInvite, btnInventory, btnSettings, btnHomeStatus;
    private Animation animUp, animDown;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindViews();
        initAnimations();
        setupListeners();
        applyBounceEffect(btnShop);
        applyBounceEffect(btnInvite);
        applyBounceEffect(btnInventory);
        applyBounceEffect(btnSettings);
        applyBounceEffect(btnHomeStatus);
    }






// main bar

    private void bindViews() {
        btnShop = findViewById(R.id.btnShop);
        btnInvite = findViewById(R.id.btnInvite);
        btnInventory = findViewById(R.id.btnInventory);
        btnSettings = findViewById(R.id.btnSettings);
        btnHomeStatus = findViewById(R.id.btnHomeStatus);
    }

    private void setupListeners() {

        btnShop.setOnClickListener(v ->
                startActivity(new Intent(this, ShopActivity.class)));

        btnInvite.setOnClickListener(v ->
                startActivity(new Intent(this, InviteActivity.class)));

        btnInventory.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryActivity.class)));

        btnSettings.setOnClickListener(v ->{});

        btnHomeStatus.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
    }
    private void initAnimations() {
        animUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        animDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);
    }

    private void applyBounceEffect(ImageButton btn) {
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startAnimation(animUp);
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.startAnimation(animDown);
            }
            return false;
        });}
}
