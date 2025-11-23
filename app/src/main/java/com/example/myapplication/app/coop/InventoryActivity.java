package com.example.myapplication.app.coop;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends BaseActivity {

    private static final String ROOM_BACKGROUND = "base";
    private static final String ROOM_GYM       = "gym";
    private static final String ROOM_STUDY     = "study";
    private static final String ROOM_BEDROOM   = "bedroom";

    private LinearLayout rowInvBase, rowInvGym, rowInvStudy, rowInvBedroom;
    private TextView tvInv;

    private List<String> ownedItemsList = new ArrayList<>();
    private final List<BgItem> allItems = new ArrayList<>();

    static class BgItem {
        final String id, room;
        final int resId;
        final boolean isDefault;

        BgItem(String id, String room, int resId, boolean isDefault) {
            this.id = id; this.room = room; this.resId = resId; this.isDefault = isDefault;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        bindViews();
        initBaseNavigation();
        initItems();
        loadInventory();
    }

    @Override
    protected int getCurrentNavItemId() { return R.id.btnInventory; }

    private void bindViews() {
        rowInvBase    = findViewById(R.id.rowInvBase);
        rowInvGym     = findViewById(R.id.rowInvGym);
        rowInvStudy   = findViewById(R.id.rowInvStudy);
        rowInvBedroom = findViewById(R.id.rowInvBedroom);
        tvInv         = findViewById(R.id.tvInv);
    }

    private void initItems() {
        allItems.clear();

        allItems.add(new BgItem("bg_background", ROOM_BACKGROUND, R.drawable.bg_background, true));

        allItems.add(new BgItem("bg_gym",      ROOM_GYM,     R.drawable.bg_gym,      true));
        allItems.add(new BgItem("im_gym2",     ROOM_GYM,     R.drawable.im_gym2,     false));

        allItems.add(new BgItem("bg_study",    ROOM_STUDY,   R.drawable.bg_study,    true));
        allItems.add(new BgItem("im_study2",   ROOM_STUDY,   R.drawable.im_study2,   false));

        allItems.add(new BgItem("bg_bedroom",  ROOM_BEDROOM, R.drawable.bg_bedroom,  true));
        allItems.add(new BgItem("im_bedroom2", ROOM_BEDROOM, R.drawable.im_bedroom2, false));
    }

    private void loadInventory() {
        if (FirestoreHelper.getCurrentUser() == null) {
            tvInv.setText("Inventory");
            return;
        }

        tvInv.setText("Loading...");

        FirestoreHelper.loadCurrentUser(new FirestoreHelper.UserDocCallback() {
            @Override
            public void onSuccess(DocumentSnapshot snap) {
                AppUser user = AppUser.fromSnapshot(snap);
                ownedItemsList = (user.ownedItems != null) ? user.ownedItems : new ArrayList<>();
                tvInv.setText("Inventory");
                buildInventoryUi();
            }

            @Override
            public void onError(String msg) {
                tvInv.setText("Error!");
                UIHelper.showToast(InventoryActivity.this, "Failed: " + msg);
            }
        });
    }

    private void buildInventoryUi() {
        rowInvBase.removeAllViews();
        rowInvGym.removeAllViews();
        rowInvStudy.removeAllViews();
        rowInvBedroom.removeAllViews();

        for (BgItem item : allItems)
            if (item.isDefault || ownedItemsList.contains(item.id))
                addItemToRow(item);
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
        img.setBackgroundResource(R.drawable.background_bottom_icon_bg);
        img.setPadding(pad, pad, pad, pad);
        img.setImageResource(item.resId);

        img.setOnClickListener(v -> applyBackground(item));
        parent.addView(img);
    }

    private LinearLayout getRow(String room) {
        switch (room) {
            case ROOM_BACKGROUND: return rowInvBase;
            case ROOM_GYM:        return rowInvGym;
            case ROOM_STUDY:      return rowInvStudy;
            case ROOM_BEDROOM:    return rowInvBedroom;
            default: return null;
        }
    }

    private void applyBackground(BgItem item) {
        String field = "current_bg_" + item.room;

        FirestoreHelper.updateUserField(field, item.id, new FirestoreHelper.SimpleCallback() {
            @Override public void onSuccess() {
                UIHelper.showToast(InventoryActivity.this, "Applied: " + item.id);
            }
            @Override public void onError(String msg) {
                UIHelper.showToast(InventoryActivity.this, "Failed: " + msg);
            }
        });
    }
}
