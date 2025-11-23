package com.example.myapplication.app.shop;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.example.myapplication.app.core.UIHelper;
import com.example.myapplication.app.core.AppUser;
import com.example.myapplication.app.core.BaseActivity;
import com.example.myapplication.app.core.FirestoreHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopActivity extends BaseActivity {

    private static final String ROOM_BACKGROUND = "base", ROOM_GYM = "gym",
            ROOM_STUDY = "study", ROOM_BEDROOM = "bedroom";

    private LinearLayout rowBackground, rowGym, rowStudy, rowBedroom;
    private TextView tvShop;

    private long currentCoins = 0;
    private List<String> ownedItemsList = new ArrayList<>();

    static class BgItem {
        final String id, room;
        final int resId, price;
        BgItem(String id, String room, int resId, int price) {
            this.id = id; this.room = room; this.resId = resId; this.price = price;
        }
    }

    private final List<BgItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        bindViews();
        initBaseNavigation();
        initItems();
        loadUserData();
    }

    @Override
    protected int getCurrentNavItemId() { return R.id.btnShop; }

    private void bindViews() {
        rowBackground = findViewById(R.id.rowBackground);
        rowGym        = findViewById(R.id.rowGym);
        rowStudy      = findViewById(R.id.rowStudy);
        rowBedroom    = findViewById(R.id.rowBedroom);
        tvShop        = findViewById(R.id.tvShop);
    }

    private void initItems() {
        allItems.clear();
        allItems.add(new BgItem("im_gym2",     ROOM_GYM,     R.drawable.im_gym2,     50));
        allItems.add(new BgItem("im_study2",   ROOM_STUDY,   R.drawable.im_study2,   50));
        allItems.add(new BgItem("im_bedroom2", ROOM_BEDROOM, R.drawable.im_bedroom2, 50));
    }

    private void loadUserData() {
        if (FirestoreHelper.getCurrentUser() == null) {
            tvShop.setText("Shop");
            return;
        }

        tvShop.setText("Loading Coins...");
        FirestoreHelper.loadCurrentUser(new FirestoreHelper.UserDocCallback() {
            @Override
            public void onSuccess(DocumentSnapshot s) {
                AppUser u = AppUser.fromSnapshot(s);
                currentCoins = u.coins;
                ownedItemsList = (u.ownedItems != null) ? u.ownedItems : new ArrayList<>();
                tvShop.setText("Shop (Coins: " + currentCoins + ")");
                buildShopUi();
            }

            @Override
            public void onError(String m) {
                tvShop.setText("Shop");
                UIHelper.showToast(ShopActivity.this, "Failed: " + m);
            }
        });
    }

    private void buildShopUi() {
        rowBackground.removeAllViews();
        rowGym.removeAllViews();
        rowStudy.removeAllViews();
        rowBedroom.removeAllViews();

        for (BgItem item : allItems)
            if (!ownedItemsList.contains(item.id)) addItemToRow(item);
    }

    private void addItemToRow(BgItem item) {
        LinearLayout parent = getRow(item.room);
        if (parent == null) return;

        int size = UIHelper.dpToPx(this, 80);
        int pad  = UIHelper.dpToPx(this, 4);

        ImageView img = new ImageView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(pad, pad, pad, pad);

        img.setLayoutParams(lp);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageResource(item.resId);
        img.setBackgroundResource(R.drawable.background_bottom_icon_bg);
        img.setPadding(pad, pad, pad, pad);

        img.setOnClickListener(v -> tryBuyItem(item, v));
        parent.addView(img);
    }

    private LinearLayout getRow(String room) {
        switch (room) {
            case ROOM_BACKGROUND: return rowBackground;
            case ROOM_GYM:        return rowGym;
            case ROOM_STUDY:      return rowStudy;
            case ROOM_BEDROOM:    return rowBedroom;
            default: return null;
        }
    }

    private void tryBuyItem(BgItem item, View v) {
        if (currentCoins < item.price) {
            UIHelper.showToast(this, "Not enough coins! Price: " + item.price);
            return;
        }
        buyItem(item, v);
    }

    private void buyItem(BgItem item, View view) {
        if (FirestoreHelper.getCurrentUser() == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("coins", FieldValue.increment(-item.price));
        updates.put("owned_items", FieldValue.arrayUnion(item.id));

        FirestoreHelper.updateUserFields(updates, new FirestoreHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                UIHelper.showToast(ShopActivity.this, "Purchased!");
                currentCoins -= item.price;
                tvShop.setText("Shop (Coins: " + currentCoins + ")");
                ((ViewGroup) view.getParent()).removeView(view);
                ownedItemsList.add(item.id);
            }

            @Override
            public void onError(String m) {
                UIHelper.showToast(ShopActivity.this, "Failed: " + m);
            }
        });
    }
}
