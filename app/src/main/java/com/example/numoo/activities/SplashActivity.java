package com.example.numoo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.numoo.R;
import com.example.numoo.firebase.FirebaseAuthHelper;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        try {
            new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthState, SPLASH_DELAY);
        } catch (Exception e) {
            navigateToRoleSelection();
        }
    }

    private void checkAuthState() {
        try {
            FirebaseAuthHelper authHelper = new FirebaseAuthHelper(this);

            if (authHelper.isLoggedIn()) {
                String role = authHelper.getCachedRole();
                if (role != null) {
                    navigateByRole(role);
                } else {
                    // Fetch role from Firestore
                    authHelper.getUserRole(new FirebaseAuthHelper.RoleCallback() {
                        @Override
                        public void onRoleFound(String role) {
                            navigateByRole(role);
                        }

                        @Override
                        public void onError(String error) {
                            navigateToRoleSelection();
                        }
                    });
                }
            } else {
                navigateToRoleSelection();
            }
        } catch (Exception e) {
            navigateToRoleSelection();
        }
    }

    private void navigateByRole(String role) {
        Intent intent;
        if ("ADMIN".equals(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, PermissionSetupActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void navigateToRoleSelection() {
        startActivity(new Intent(this, RoleSelectionActivity.class));
        finish();
    }
}
