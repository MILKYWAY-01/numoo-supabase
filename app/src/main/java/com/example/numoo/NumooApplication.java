package com.example.numoo;

import android.app.Application;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class NumooApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Firestore offline persistence
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
