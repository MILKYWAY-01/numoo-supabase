package com.example.numoo.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NotificationRequest {
    private String notifId;
    private String userId;
    private String userName;
    private String packageName;
    private String appName;
    private String message;
    private long timestamp;
    private String status; // "PENDING", "APPROVED", "DENIED"

    public NotificationRequest() {
        // Required empty constructor for Firestore
    }

    public NotificationRequest(String userId, String userName, String packageName,
                                String appName, String message) {
        this.notifId = UUID.randomUUID().toString();
        this.userId = userId;
        this.userName = userName;
        this.packageName = packageName;
        this.appName = appName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.status = "PENDING";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("notifId", notifId);
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("packageName", packageName);
        map.put("appName", appName);
        map.put("message", message);
        map.put("timestamp", timestamp);
        map.put("status", status);
        return map;
    }

    // Getters and Setters
    public String getNotifId() { return notifId; }
    public void setNotifId(String notifId) { this.notifId = notifId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
