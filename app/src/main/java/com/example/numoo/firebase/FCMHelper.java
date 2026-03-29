package com.example.numoo.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.numoo.R;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMHelper extends FirebaseMessagingService {

    private static final String TAG = "FCMHelper";
    private static final String CHANNEL_ID = "numoo_notifications";
    private static final String CHANNEL_NAME = "Numoo Notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        try {
            String title = "Numoo";
            String body = "You have a new notification";

            if (remoteMessage.getNotification() != null) {
                title = remoteMessage.getNotification().getTitle() != null ?
                        remoteMessage.getNotification().getTitle() : title;
                body = remoteMessage.getNotification().getBody() != null ?
                        remoteMessage.getNotification().getBody() : body;
            }

            if (remoteMessage.getData().size() > 0) {
                if (remoteMessage.getData().containsKey("title")) {
                    title = remoteMessage.getData().get("title");
                }
                if (remoteMessage.getData().containsKey("body")) {
                    body = remoteMessage.getData().get("body");
                }
            }

            showNotification(title, body);
        } catch (Exception e) {
            Log.e(TAG, "Error handling FCM message", e);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token: " + token);
    }

    private void showNotification(String title, String body) {
        try {
            NotificationManager manager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Numoo app notifications");
                manager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }

    // Static helper methods
    public static void subscribeToTopic(String topic) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Subscribed to " + topic))
                    .addOnFailureListener(e -> Log.e(TAG, "Subscribe failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Error subscribing to topic", e);
        }
    }

    public static void unsubscribeFromTopic(String topic) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Unsubscribed from " + topic))
                    .addOnFailureListener(e -> Log.e(TAG, "Unsubscribe failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Error unsubscribing from topic", e);
        }
    }
}
