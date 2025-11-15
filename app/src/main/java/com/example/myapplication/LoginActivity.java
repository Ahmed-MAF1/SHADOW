package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;


public class LoginActivity extends AppCompatActivity {

    TextInputEditText etUsername, etPassword;
    Button btnLogin;
    TextView tvCreate, tvForgot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvCreate = findViewById(R.id.tvCreate);
        tvForgot = findViewById(R.id.tvForgot);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        tvCreate.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(i);
        });

        tvForgot.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Reset password flow", Toast.LENGTH_SHORT).show();
        });
    }

    private void doLogin() {
        String user = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (user.isEmpty()) {
            etUsername.setError("Please write your username");
            etUsername.requestFocus();
            return;
        }
        if (pass.isEmpty()) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        if (user.equalsIgnoreCase("ahmed") && pass.equals("123")) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Login data is incorrect", Toast.LENGTH_SHORT).show();
        }
    }
}
