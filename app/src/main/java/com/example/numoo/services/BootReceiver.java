package com.example.numoo.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent == null || intent.getAction() == null) return;

            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {

                Log.d(TAG, "Device booted / package replaced, starting UsageTrackingService");

                Intent serviceIntent = new Intent(context, UsageTrackingService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive", e);
        }
    }
}
