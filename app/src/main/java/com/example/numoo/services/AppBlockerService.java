package com.example.numoo.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.example.numoo.activities.BlockActivity;
import com.example.numoo.supabase.SupabaseAuthHelper;
import com.example.numoo.supabase.SupabaseDbHelper;
import com.example.numoo.models.AppLimit;
import com.example.numoo.models.UsageData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppBlockerService extends AccessibilityService {

    private static final String TAG = "AppBlockerService";
    private static final long CHECK_INTERVAL = 10 * 1000; // 10 seconds

    private SupabaseDbHelper firestoreHelper;
    private SupabaseAuthHelper authHelper;
    private Handler handler;
    private Runnable checkRunnable;
    private String currentForegroundPackage = "";
    
    private final Map<String, AppLimit> appLimitsMap = new HashMap<>();
    private final Map<String, Long> currentUsageMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            firestoreHelper = new SupabaseDbHelper(this);
            authHelper = new SupabaseAuthHelper(this);
            handler = new Handler(Looper.getMainLooper());
            
            startListeningToLimits();
            startPeriodicUsageFetch();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    private void startListeningToLimits() {
        String uid = authHelper.getCurrentUid();
        if (uid == null) return;

        firestoreHelper.listenToLimits(uid, new SupabaseDbHelper.FirestoreCallback<List<AppLimit>>() {
            @Override
            public void onSuccess(List<AppLimit> result) {
                appLimitsMap.clear();
                if (result != null) {
                    for (AppLimit limit : result) {
                        appLimitsMap.put(limit.getPackageName(), limit);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error listening to limits: " + error);
            }
        });
    }

    private void startPeriodicUsageFetch() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                fetchTodayUsage();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.post(checkRunnable);
    }

    private void fetchTodayUsage() {
        String uid = authHelper.getCurrentUid();
        if (uid == null) return;

        firestoreHelper.getUsageDataForDate(uid, SupabaseDbHelper.getTodayDate(), 
            new SupabaseDbHelper.FirestoreCallback<List<UsageData>>() {
                @Override
                public void onSuccess(List<UsageData> result) {
                    currentUsageMap.clear();
                    if (result != null) {
                        for (UsageData data : result) {
                            currentUsageMap.put(data.getPackageName(), data.getUsageTimeMillis());
                        }
                    }
                    // Re-check current foreground app after usage update
                    if (!currentForegroundPackage.isEmpty()) {
                        checkAndBlock(currentForegroundPackage);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error fetching usage: " + error);
                }
            });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

            CharSequence packageNameCS = event.getPackageName();
            if (packageNameCS == null) return;

            String packageName = packageNameCS.toString();
            if (packageName.equals(getPackageName()) || packageName.contains("launcher") || packageName.equals("com.android.systemui")) return;

            if (!packageName.equals(currentForegroundPackage)) {
                currentForegroundPackage = packageName;
                checkAndBlock(packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onAccessibilityEvent", e);
        }
    }

    private void checkAndBlock(String packageName) {
        AppLimit limit = appLimitsMap.get(packageName);
        if (limit == null) return;

        if (limit.isBlocked()) {
            launchBlockScreen(packageName, limit.getAppName(), 0, limit.getLimitMillis());
            return;
        }

        Long usage = currentUsageMap.get(packageName);
        if (usage != null && limit.getLimitMillis() > 0 && usage >= limit.getLimitMillis()) {
            launchBlockScreen(packageName, limit.getAppName(), usage, limit.getLimitMillis());
        }
    }

    private void launchBlockScreen(String packageName, String appName, long usageTime, long limitTime) {
        Intent intent = new Intent(this, BlockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("packageName", packageName);
        intent.putExtra("appName", appName != null ? appName : packageName);
        intent.putExtra("usageTime", usageTime);
        intent.putExtra("limitTime", limitTime);
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        firestoreHelper.removeLimitsListener();
    }
}

