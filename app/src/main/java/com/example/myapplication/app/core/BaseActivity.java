package com.example.myapplication.app.core;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.app.coop.InventoryActivity;
import com.example.myapplication.app.shop.InviteActivity;
import com.example.myapplication.app.home.MainActivity;
import com.example.myapplication.app.profile.SettingsActivity;
import com.example.myapplication.app.shop.ShopActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected ImageButton btnShop, btnInvite, btnInventory, btnSettings, btnHomeStatus;
    protected Animation animUp, animDown;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /** Call this after setContentView() */
    protected void initBaseNavigation() {
        btnShop       = findViewById(R.id.btnShop);
        btnInvite     = findViewById(R.id.btnInvite);
        btnInventory  = findViewById(R.id.btnInventory);
        btnSettings   = findViewById(R.id.btnSettings);
        btnHomeStatus = findViewById(R.id.btnHomeStatus);

        animUp   = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        animDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);

        setupBottomNav();
        applyBounce(btnShop);
        applyBounce(btnInvite);
        applyBounce(btnInventory);
        applyBounce(btnSettings);
        applyBounce(btnHomeStatus);
    }

    /** Navigation logic */
    private void setupBottomNav() {
        View.OnClickListener nav = v -> {
            if (shouldBlockNavigation()) {
                String msg = getNavigationBlockedMessage();
                if (msg != null && !msg.isEmpty())
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                return;
            }

            if (v.getId() == getCurrentNavItemId()) return;

            Class<?> target = null;
            int id = v.getId();

            if (id == R.id.btnShop)          target = ShopActivity.class;
            else if (id == R.id.btnInvite)   target = InviteActivity.class;
            else if (id == R.id.btnInventory)target = InventoryActivity.class;
            else if (id == R.id.btnSettings) target = SettingsActivity.class;
            else if (id == R.id.btnHomeStatus) target = MainActivity.class;

            if (target != null) startActivity(new Intent(this, target));
        };

        setClick(btnShop, nav);
        setClick(btnInvite, nav);
        setClick(btnInventory, nav);
        setClick(btnSettings, nav);
        setClick(btnHomeStatus, nav);
    }

    private void setClick(ImageButton btn, View.OnClickListener l) {
        if (btn != null) btn.setOnClickListener(l);
    }

    /** Bounce animation */
    private void applyBounce(ImageButton btn) {
        if (btn == null) return;

        btn.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) v.startAnimation(animUp);
            else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL)
                v.startAnimation(animDown);
            return false;
        });
    }

    /** Child classes override these */
    protected abstract int getCurrentNavItemId();

    protected boolean shouldBlockNavigation() { return false; }

    @Nullable
    protected String getNavigationBlockedMessage() { return null; }
}
