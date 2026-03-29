package com.example.numoo.firebase;

import android.content.Context;
import android.util.Log;

import com.example.numoo.models.AppLimit;
import com.example.numoo.models.NotificationRequest;
import com.example.numoo.models.UsageData;
import com.example.numoo.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static final String USERS_COLLECTION = "users";
    private static final String USAGE_DATA_SUB = "usageData";
    private static final String LIMITS_COLLECTION = "limits";
    private static final String NOTIFICATIONS_COLLECTION = "notifications";

    private final FirebaseFirestore db;
    private ListenerRegistration limitsListener;

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public FirestoreHelper(Context context) {
        db = FirebaseFirestore.getInstance();
    }

    // ==================== USAGE DATA ====================

    public void updateUsageData(String uid, UsageData usageData) {
        try {
            String today = getTodayDate();
            db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(USAGE_DATA_SUB)
                .document(today)
                .collection("apps")
                .document(usageData.getPackageName())
                .set(usageData.toMap())
                .addOnFailureListener(e -> Log.e(TAG, "Sync failed for " + usageData.getPackageName(), e));
        } catch (Exception e) {
            Log.e(TAG, "updateUsageData error", e);
        }
    }

    public void getUsageDataForDate(String uid, String date, FirestoreCallback<List<UsageData>> callback) {
        try {
            db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(USAGE_DATA_SUB)
                .document(date)
                .collection("apps")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UsageData> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        UsageData data = doc.toObject(UsageData.class);
                        if (data != null) {
                            list.add(data);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // ==================== LIMITS ====================

    public void setAppLimit(String userId, AppLimit limit, FirestoreCallback<Void> callback) {
        try {
            db.collection(LIMITS_COLLECTION)
                .document(userId)
                .collection("apps")
                .document(limit.getPackageName())
                .set(limit.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getAppLimits(String userId, FirestoreCallback<List<AppLimit>> callback) {
        try {
            db.collection(LIMITS_COLLECTION)
                .document(userId)
                .collection("apps")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AppLimit> limits = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        AppLimit limit = doc.toObject(AppLimit.class);
                        if (limit != null) {
                            limits.add(limit);
                        }
                    }
                    callback.onSuccess(limits);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void listenToLimits(String userId, FirestoreCallback<List<AppLimit>> callback) {
        try {
            if (limitsListener != null) {
                limitsListener.remove();
            }
            limitsListener = db.collection(LIMITS_COLLECTION)
                .document(userId)
                .collection("apps")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<AppLimit> limits = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        AppLimit limit = doc.toObject(AppLimit.class);
                        if (limit != null) {
                            limits.add(limit);
                        }
                    }
                    callback.onSuccess(limits);
                });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void removeLimitsListener() {
        if (limitsListener != null) {
            limitsListener.remove();
            limitsListener = null;
        }
    }

    // ==================== USERS ====================

    public void getLinkedUsers(String adminId, FirestoreCallback<List<User>> callback) {
        try {
            db.collection(USERS_COLLECTION)
                .whereEqualTo("adminId", adminId)
                .whereEqualTo("role", "USER")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getUserInfo(String uid, FirestoreCallback<User> callback) {
        try {
            db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // ==================== NOTIFICATIONS ====================

    public void sendTimeRequest(String adminId, NotificationRequest request,
                                 FirestoreCallback<Void> callback) {
        try {
            db.collection(NOTIFICATIONS_COLLECTION)
                .document(adminId)
                .collection("requests")
                .document(request.getNotifId())
                .set(request.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getNotifications(String adminId, FirestoreCallback<List<NotificationRequest>> callback) {
        try {
            db.collection(NOTIFICATIONS_COLLECTION)
                .document(adminId)
                .collection("requests")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<NotificationRequest> notifications = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        NotificationRequest notif = doc.toObject(NotificationRequest.class);
                        if (notif != null) {
                            notifications.add(notif);
                        }
                    }
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // ==================== HELPERS ====================

    public static String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}
