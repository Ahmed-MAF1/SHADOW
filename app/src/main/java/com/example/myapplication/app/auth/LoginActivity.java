package com.example.myapplication.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.app.home.MainActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvCreateAccount, tvForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        bindViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) goToMain();
    }

    private void bindViews() {
        etEmail         = findViewById(R.id.etUsername); // نفس الـ id في XML
        etPassword      = findViewById(R.id.etPassword);
        btnLogin        = findViewById(R.id.btnLogin);
        tvCreateAccount = findViewById(R.id.tvCreate);
        tvForgotPassword= findViewById(R.id.tvForgot);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(this, CreateAccountActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Reset password flow", Toast.LENGTH_SHORT).show());
    }

    private void attemptLogin() {
        String email    = getTrimmed(etEmail);
        String password = getTrimmed(etPassword);

        if (email.isEmpty()) {
            etEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        setLoggingIn(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(a -> goToMain())
                .addOnFailureListener(e -> {
                    setLoggingIn(false);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getTrimmed(TextInputEditText editText) {
        CharSequence s = editText.getText();
        return s == null ? "" : s.toString().trim();
    }

    private void setLoggingIn(boolean loggingIn) {
        btnLogin.setEnabled(!loggingIn);
        btnLogin.setText(loggingIn ? "Logging in..." : getString(R.string.log_in));
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
