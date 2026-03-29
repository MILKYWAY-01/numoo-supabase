package com.example.numoo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.numoo.R;
import com.example.numoo.supabase.SupabaseAuthHelper;
import com.example.numoo.supabase.SupabaseDbHelper;
import com.example.numoo.models.UsageData;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class UsageTrackingService extends Service {

    private static final String TAG = "UsageTrackingService";
    private static final String CHANNEL_ID = "numoo_tracking";
    private static final String CHANNEL_NAME = "Usage Tracking";
    private static final int NOTIFICATION_ID = 1001;
    private static final long TRACK_INTERVAL = 60 * 1000; // 1 minute

    private Handler handler;
    private Runnable trackRunnable;
    private SupabaseDbHelper firestoreHelper;
    private SupabaseAuthHelper authHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            handler = new Handler(Looper.getMainLooper());
            firestoreHelper = new SupabaseDbHelper(this);
            authHelper = new SupabaseAuthHelper(this);
            createNotificationChannel();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification notification = buildNotification();
            startForeground(NOTIFICATION_ID, notification);
            startTracking();
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand", e);
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (handler != null && trackRunnable != null) {
                handler.removeCallbacks(trackRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        try {
            Intent restartIntent = new Intent(getApplicationContext(), UsageTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restarting service", e);
        }
    }

    private void startTracking() {
        trackRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    trackUsage();
                } catch (Exception e) {
                    Log.e(TAG, "Error tracking usage", e);
                }
                handler.postDelayed(this, TRACK_INTERVAL);
            }
        };
        handler.post(trackRunnable);
    }

    private void trackUsage() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String uid = authHelper.getCurrentUid();
                if (uid == null) return;

                UsageStatsManager usageStatsManager = (UsageStatsManager)
                        getSystemService(Context.USAGE_STATS_SERVICE);
                if (usageStatsManager == null) return;

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startTime = calendar.getTimeInMillis();
                long endTime = System.currentTimeMillis();

                List<UsageStats> stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

                if (stats == null || stats.isEmpty()) return;

                PackageManager pm = getPackageManager();

                for (UsageStats usageStat : stats) {
                    try {
                        String packageName = usageStat.getPackageName();
                        long totalTime = usageStat.getTotalTimeInForeground();

                        if (totalTime <= 0) continue;
                        if (packageName.equals(getPackageName())) continue;

                        String appName;
                        try {
                            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                            appName = pm.getApplicationLabel(appInfo).toString();
                        } catch (PackageManager.NameNotFoundException e) {
                            appName = packageName;
                        }

                        UsageData usageData = new UsageData(appName, packageName, totalTime);
                        firestoreHelper.updateUsageData(uid, usageData);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing usage stat", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in trackUsage", e);
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Tracks app usage in background");
                channel.setShowBadge(false);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Numoo")
                .setContentText("Numoo is protecting your device")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
}

