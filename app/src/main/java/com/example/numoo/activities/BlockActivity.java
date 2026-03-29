package com.example.numoo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.numoo.R;
import com.example.numoo.supabase.SupabaseAuthHelper;
import com.example.numoo.supabase.SupabaseDbHelper;
import com.example.numoo.models.NotificationRequest;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class BlockActivity extends AppCompatActivity {

    private String packageName, appName;
    private long usageTime, limitTime;
    private TextView tvCountdown;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);

        try {
            packageName = getIntent().getStringExtra("packageName");
            appName = getIntent().getStringExtra("appName");
            usageTime = getIntent().getLongExtra("usageTime", 0);
            limitTime = getIntent().getLongExtra("limitTime", 0);

            initViews();
            startCountdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        try {
            TextView tvAppName = findViewById(R.id.tv_app_name);
            TextView tvMessage = findViewById(R.id.tv_message);
            TextView tvUsageInfo = findViewById(R.id.tv_usage_info);
            tvCountdown = findViewById(R.id.tv_countdown);
            MaterialButton btnRequestTime = findViewById(R.id.btn_request_time);
            MaterialButton btnGoHome = findViewById(R.id.btn_go_home);

            tvAppName.setText(appName != null ? appName : "App");
            tvMessage.setText("Time limit reached!");

            String usageStr = formatTime(usageTime);
            String limitStr = formatTime(limitTime);
            tvUsageInfo.setText("Used: " + usageStr + " / Limit: " + limitStr);

            btnRequestTime.setOnClickListener(v -> requestMoreTime());
            btnGoHome.setOnClickListener(v -> goHome());

            // Try to load app icon
            try {
                ImageView ivAppIcon = findViewById(R.id.iv_app_icon);
                if (packageName != null) {
                    ivAppIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
                }
            } catch (Exception e) {
                // Use default icon
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCountdown() {
        try {
            Calendar midnight = Calendar.getInstance();
            midnight.set(Calendar.HOUR_OF_DAY, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.set(Calendar.MILLISECOND, 0);
            midnight.add(Calendar.DAY_OF_MONTH, 1);

            long millisUntilMidnight = midnight.getTimeInMillis() - System.currentTimeMillis();

            countDownTimer = new CountDownTimer(millisUntilMidnight, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    try {
                        long hours = millisUntilFinished / (1000 * 60 * 60);
                        long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                        long seconds = (millisUntilFinished / 1000) % 60;
                        tvCountdown.setText(String.format("Resets in: %02d:%02d:%02d",
                                hours, minutes, seconds));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFinish() {
                    finish();
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestMoreTime() {
        try {
            SupabaseAuthHelper authHelper = new SupabaseAuthHelper(this);
            SupabaseDbHelper firestoreHelper = new SupabaseDbHelper(this);

            String uid = authHelper.getCurrentUid();
            String userName = authHelper.getCachedUserName();
            String adminId = authHelper.getCachedAdminId();

            if (uid == null || adminId == null) {
                Toast.makeText(this, "Error: Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            NotificationRequest request = new NotificationRequest(
                    uid, userName, packageName, appName,
                    userName + " is requesting more time for " + appName
            );

            firestoreHelper.sendTimeRequest(adminId, request,
                    new SupabaseDbHelper.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(BlockActivity.this,
                                    "Request sent to admin!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(BlockActivity.this,
                                    "Failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goHome() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        // Override back to go home instead of back to blocked app
        goHome();
        // Call through so lint (MissingSuperCall) and the framework behavior stay consistent.
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private String formatTime(long millis) {
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis / (1000 * 60)) % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}

