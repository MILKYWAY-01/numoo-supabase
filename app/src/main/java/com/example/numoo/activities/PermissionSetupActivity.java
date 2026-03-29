package com.example.numoo.activities;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.numoo.R;
import com.example.numoo.services.AppBlockerService;
import com.example.numoo.services.UsageTrackingService;
import com.google.android.material.card.MaterialCardView;

public class PermissionSetupActivity extends AppCompatActivity {

    private static final int STEP_USAGE = 0;
    private static final int STEP_OVERLAY = 1;
    private static final int STEP_ACCESSIBILITY = 2;
    private static final int STEP_BATTERY = 3;
    private static final int STEP_DONE = 4;

    private int currentStep = STEP_USAGE;
    private TextView tvStepTitle, tvStepDescription, tvStepNumber;
    private Button btnGrantPermission, btnSkip;
    private ImageView ivStepIcon;
    private MaterialCardView cardStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);

        try {
            tvStepTitle = findViewById(R.id.tv_step_title);
            tvStepDescription = findViewById(R.id.tv_step_description);
            tvStepNumber = findViewById(R.id.tv_step_number);
            btnGrantPermission = findViewById(R.id.btn_grant_permission);
            ivStepIcon = findViewById(R.id.iv_step_icon);
            cardStep = findViewById(R.id.card_step);

            btnGrantPermission.setOnClickListener(v -> grantCurrentPermission());

            updateStepUI();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndAdvanceStep();
    }

    private void checkAndAdvanceStep() {
        try {
            if (currentStep == STEP_USAGE && hasUsageAccess()) {
                currentStep = STEP_OVERLAY;
            }
            if (currentStep == STEP_OVERLAY && hasOverlayPermission()) {
                currentStep = STEP_ACCESSIBILITY;
            }
            if (currentStep == STEP_ACCESSIBILITY && isAccessibilityEnabled()) {
                currentStep = STEP_BATTERY;
            }
            if (currentStep == STEP_BATTERY && isBatteryOptimizationExempt()) {
                currentStep = STEP_DONE;
            }
            if (currentStep == STEP_DONE) {
                onAllPermissionsGranted();
                return;
            }
            updateStepUI();
        } catch (Exception e) {
            updateStepUI();
        }
    }

    private void updateStepUI() {
        try {
            switch (currentStep) {
                case STEP_USAGE:
                    tvStepNumber.setText("Step 1 of 4");
                    tvStepTitle.setText("Usage Access");
                    tvStepDescription.setText("Allow Numoo to access app usage data to track screen time.");
                    btnGrantPermission.setText("Grant Usage Access");
                    break;
                case STEP_OVERLAY:
                    tvStepNumber.setText("Step 2 of 4");
                    tvStepTitle.setText("Display Over Other Apps");
                    tvStepDescription.setText("Allow Numoo to display the block screen over other apps.");
                    btnGrantPermission.setText("Grant Overlay Permission");
                    break;
                case STEP_ACCESSIBILITY:
                    tvStepNumber.setText("Step 3 of 4");
                    tvStepTitle.setText("Accessibility Service");
                    tvStepDescription.setText("Enable Numoo's accessibility service to detect which app is in the foreground.");
                    btnGrantPermission.setText("Enable Accessibility");
                    break;
                case STEP_BATTERY:
                    tvStepNumber.setText("Step 4 of 4");
                    tvStepTitle.setText("Battery Optimization");
                    tvStepDescription.setText("Exempt Numoo from battery optimization to keep tracking running.");
                    btnGrantPermission.setText("Disable Battery Optimization");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void grantCurrentPermission() {
        try {
            switch (currentStep) {
                case STEP_USAGE:
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    break;
                case STEP_OVERLAY:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(overlayIntent);
                    }
                    break;
                case STEP_ACCESSIBILITY:
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    Toast.makeText(this, "Find and enable 'Numoo App Blocker'", Toast.LENGTH_LONG).show();
                    break;
                case STEP_BATTERY:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(batteryIntent);
                    } else {
                        currentStep = STEP_DONE;
                        checkAndAdvanceStep();
                    }
                    break;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Please grant permission manually in Settings", Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasUsageAccess() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasOverlayPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Settings.canDrawOverlays(this);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            ComponentName expectedComponent = new ComponentName(this, AppBlockerService.class);
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (TextUtils.isEmpty(enabledServices)) return false;
            return enabledServices.contains(expectedComponent.flattenToString());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBatteryOptimizationExempt() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private void onAllPermissionsGranted() {
        try {
            // Start tracking service
            Intent serviceIntent = new Intent(this, UsageTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, UserDashboardActivity.class));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error starting service: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
