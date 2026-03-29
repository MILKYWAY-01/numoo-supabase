package com.example.numoo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.numoo.R;
import com.example.numoo.viewmodels.AuthViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        try {
            authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

            etEmail = findViewById(R.id.et_email);
            etPassword = findViewById(R.id.et_password);
            btnLogin = findViewById(R.id.btn_login);
            progressBar = findViewById(R.id.progress_bar);
            TextView tvRegister = findViewById(R.id.tv_register);

            btnLogin.setOnClickListener(v -> attemptLogin());

            tvRegister.setOnClickListener(v -> {
                startActivity(new Intent(this, RoleSelectionActivity.class));
                finish();
            });

            observeViewModel();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void attemptLogin() {
        try {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (email.isEmpty()) {
                etEmail.setError("Email required");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password required");
                return;
            }

            authViewModel.login(email, password);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, loading -> {
            if (loading != null) {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
                btnLogin.setEnabled(!loading);
            }
        });

        authViewModel.getAuthResult().observe(this, result -> {
            if (result != null) {
                if ("ADMIN".equals(result)) {
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                } else {
                    startActivity(new Intent(this, PermissionSetupActivity.class));
                }
                finish();
            }
        });

        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
