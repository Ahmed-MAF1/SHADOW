package com.example.myapplication.app.core;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * FirestoreHelper — تبسيط التعامل مع users/{uid}
 */
public class FirestoreHelper {

    public interface UserDocCallback {
        void onSuccess(DocumentSnapshot snapshot);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    public static FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    @Nullable
    public static String getCurrentUid() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @Nullable
    public static DocumentReference getUserDoc() {
        String uid = getCurrentUid();
        return uid == null ? null : db.collection("users").document(uid);
    }

    // ------------------ Load User ------------------

    public static void loadCurrentUser(UserDocCallback cb) {
        DocumentReference ref = getUserDoc();
        if (ref == null) {
            cb.onError("User not logged in");
            return;
        }

        ref.get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) cb.onSuccess(snap);
                    else cb.onError("User document not found");
                })
                .addOnFailureListener(e -> cb.onError(msg(e)));
    }

    // ------------------ Update Username ------------------

    public static void updateUsername(String newName, SimpleCallback cb) {
        if (TextUtils.isEmpty(newName)) {
            cb.onError("Name is empty");
            return;
        }
        updateUserField("username", newName, cb);
    }

    // ------------------ Increment Field ------------------

    public static void incrementUserField(String field, long value, SimpleCallback cb) {
        updateUserField(field, FieldValue.increment(value), cb);
    }

    // ------------------ Update Single Field ------------------

    public static void updateUserField(String field, Object value, SimpleCallback cb) {
        DocumentReference ref = getUserDoc();
        if (ref == null) {
            cb.onError("User not logged in");
            return;
        }

        ref.update(field, value)
                .addOnSuccessListener(a -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(msg(e)));
    }

    // ------------------ Update Multiple Fields ------------------

    public static void updateUserFields(Map<String, Object> map, SimpleCallback cb) {
        DocumentReference ref = getUserDoc();
        if (ref == null) {
            cb.onError("User not logged in");
            return;
        }

        ref.update(map)
                .addOnSuccessListener(a -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(msg(e)));
    }

    // ------------------ Helper ------------------

    private static String msg(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }
}
