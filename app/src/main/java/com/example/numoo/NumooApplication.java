package com.example.numoo;

import android.app.Application;
import com.example.numoo.supabase.SupabaseClientConfig;

public class NumooApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Supabase client
        try {
            // Supabase client is initialized lazily via SupabaseClientConfig.client
            // This ensures the client is ready when needed
            SupabaseClientConfig.INSTANCE.getClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
