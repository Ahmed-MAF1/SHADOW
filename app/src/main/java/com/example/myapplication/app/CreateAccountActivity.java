package com.example.myapplication.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.textfield.TextInputEditText;

public class CreateAccountActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnCreateAccount;
    private TextView tvGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        btnCreateAccount.setOnClickListener(v -> doCreateAccount());

        tvGoLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void doCreateAccount() {
        String user = getText(etUsername);
        String email = getText(etEmail);
        String pass = getText(etPassword);
        String confirm = getText(etConfirmPassword);

        if (user.isEmpty()) {
            etUsername.setError("Enter username");
            etUsername.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Enter email");
            etEmail.requestFocus();
            return;
        }
        if (pass.isEmpty()) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }
        if (!pass.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }


        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
