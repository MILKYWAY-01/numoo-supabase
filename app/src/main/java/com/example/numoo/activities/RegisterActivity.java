package com.example.numoo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.numoo.R;
import com.example.numoo.viewmodels.AuthViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etName, etUsername, etEmail, etPassword, etAdminCode;
    private TextInputLayout tilUsername, tilAdminCode;
    private Button btnRegister;
    private ProgressBar progressBar;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        try {
            authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
            role = getIntent().getStringExtra("role");
            if (role == null) role = "USER";

            initViews();
            setupForRole();
            observeViewModel();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etAdminCode = findViewById(R.id.et_admin_code);
        tilUsername = findViewById(R.id.til_username);
        tilAdminCode = findViewById(R.id.til_admin_code);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvLogin = findViewById(R.id.tv_login);

        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        tvTitle.setText("ADMIN".equals(role) ? "Admin Registration" : "User Registration");
    }

    private void setupForRole() {
        tilUsername.setVisibility(View.VISIBLE);
        if ("ADMIN".equals(role)) {
            tilAdminCode.setVisibility(View.GONE);
        } else {
            tilAdminCode.setVisibility(View.VISIBLE);
        }
    }

    private void attemptRegister() {
        try {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (name.isEmpty()) { etName.setError("Name required"); return; }
            if (username.isEmpty()) { etUsername.setError("Username required"); return; }
            if (email.isEmpty()) { etEmail.setError("Email required"); return; }
            if (password.isEmpty()) { etPassword.setError("Password required"); return; }
            if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }

            if ("ADMIN".equals(role)) {
                authViewModel.registerAdmin(name, username, email, password);
            } else {
                String adminCode = etAdminCode.getText() != null ? etAdminCode.getText().toString().trim() : "";
                if (adminCode.isEmpty()) { etAdminCode.setError("Admin code required"); return; }

                authViewModel.registerUser(name, username, email, password, adminCode);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, loading -> {
            if (loading != null) {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
                btnRegister.setEnabled(!loading);
            }
        });

        authViewModel.getAuthResult().observe(this, result -> {
            if (result != null) {
                if (result.startsWith("ADMIN_CODE:")) {
                    String code = result.replace("ADMIN_CODE:", "");
                    new AlertDialog.Builder(this)
                        .setTitle("Registration Successful!")
                        .setMessage("Your Admin Code is:\n\n" + code +
                                "\n\nShare this code with users to link them to your account.")
                        .setPositiveButton("Continue", (d, w) -> {
                            startActivity(new Intent(this, AdminDashboardActivity.class));
                            finish();
                        })
                        .setCancelable(false)
                        .show();
                } else if ("USER_REGISTERED".equals(result)) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, PermissionSetupActivity.class));
                    finish();
                }
            }
        });

        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
