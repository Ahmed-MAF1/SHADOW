package com.example.myapplication.app.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.example.myapplication.app.auth.LoginActivity;
import com.example.myapplication.app.core.AppUser;
import com.example.myapplication.app.core.BaseActivity;
import com.example.myapplication.app.core.FirestoreHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class SettingsActivity extends BaseActivity {

    private TextView tvEmailSettings;
    private EditText etEditUsername;
    private Button btnSaveProfile, btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        bindViews();
        initBaseNavigation();
        loadUserData();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected int getCurrentNavItemId() { return R.id.btnSettings; }

    private void bindViews() {
        tvEmailSettings = findViewById(R.id.tvEmailSettings);
        etEditUsername  = findViewById(R.id.etEditUsername);
        btnSaveProfile  = findViewById(R.id.btnSaveProfile);
        btnLogout       = findViewById(R.id.btnLogout);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { goToLogin(); return; }

        tvEmailSettings.setText(user.getEmail() != null ? user.getEmail() : "Unknown");

        FirestoreHelper.loadCurrentUser(new FirestoreHelper.UserDocCallback() {
            @Override
            public void onSuccess(DocumentSnapshot snap) {
                AppUser u = AppUser.fromSnapshot(snap);
                if (u.username != null) etEditUsername.setText(u.username);
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(SettingsActivity.this, "Failed: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String newName = etEditUsername.getText() != null ? etEditUsername.getText().toString().trim() : "";
        if (newName.isEmpty()) {
            etEditUsername.setError("Enter a display name");
            etEditUsername.requestFocus();
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");

        FirestoreHelper.updateUsername(newName, new FirestoreHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(SettingsActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                resetSaveButton();
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(SettingsActivity.this, "Failed: " + msg, Toast.LENGTH_SHORT).show();
                resetSaveButton();
            }
        });
    }

    private void resetSaveButton() {
        btnSaveProfile.setEnabled(true);
        btnSaveProfile.setText("Save Changes");
    }

    private void logout() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        goToLogin();
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
