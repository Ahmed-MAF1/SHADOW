package com.example.myapplication.app.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateAccountActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnCreateAccount;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        etUsername        = findViewById(R.id.etUsername);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreateAccount  = findViewById(R.id.btnCreateAccount);

        TextView tvGoLogin = findViewById(R.id.tvGoLogin);
        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnCreateAccount.setOnClickListener(v -> attemptCreate());
    }

    private void attemptCreate() {
        String username = get(etUsername);
        String email    = get(etEmail);
        String pass     = get(etPassword);
        String pass2    = get(etConfirmPassword);

        if (!validate(username, email, pass, pass2)) return;

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> saveUser(res.getUser().getUid(), username, email))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validate(String user, String email, String pass, String pass2) {
        if (user.isEmpty()) return error(etUsername, "Enter username");
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return error(etEmail, "Enter a valid email");
        if (pass.length() < 6) return error(etPassword, "Min 6 characters");
        if (!pass.equals(pass2)) return error(etConfirmPassword, "Passwords do not match");
        return true;
    }

    private boolean error(TextInputEditText et, String msg) {
        et.setError(msg);
        et.requestFocus();
        return false;
    }

    private void saveUser(String uid, String username, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("email", email);
        data.put("coins", 0L);
        data.put("currentRoom", "gym");
        data.put("time_gym", 0L);
        data.put("time_study", 0L);
        data.put("time_bedroom", 0L);

        db.collection("users").document(uid).set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
    }

    private void setLoading(boolean loading) {
        btnCreateAccount.setEnabled(!loading);
        btnCreateAccount.setText(loading ? "Creating..." : getString(R.string.create_account));
    }

    private String get(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
