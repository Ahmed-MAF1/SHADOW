package com.example.myapplication.app.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Model for Firestore users/{uid} */
public class AppUser {

    @Nullable public String id, username, email, currentRoom, dailyDate;
    public long coins, timeGym, timeStudy, timeBedroom, dailyTime;
    @NonNull public List<String> ownedItems = new ArrayList<>();

    public AppUser() { }

    public static AppUser fromSnapshot(@NonNull DocumentSnapshot snap) {
        AppUser u = new AppUser();

        u.id         = snap.getId();
        u.username   = snap.getString("username");
        u.email      = snap.getString("email");
        u.currentRoom = snap.getString("currentRoom");
        u.dailyDate   = snap.getString("daily_date");

        u.coins       = getLong(snap, "coins");
        u.timeGym     = getLong(snap, "time_gym");
        u.timeStudy   = getLong(snap, "time_study");
        u.timeBedroom = getLong(snap, "time_bedroom");
        u.dailyTime   = getLong(snap, "daily_time");

        Object owned = snap.get("owned_items");
        if (owned instanceof List)
            //noinspection unchecked
            u.ownedItems = (List<String>) owned;

        return u;
    }

    private static long getLong(DocumentSnapshot snap, String key) {
        Long v = snap.getLong(key);
        return v != null ? v : 0L;
    }
}
