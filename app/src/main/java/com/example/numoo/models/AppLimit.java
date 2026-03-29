package com.example.numoo.models;

import java.util.HashMap;
import java.util.Map;

public class AppLimit {
    private String appName;
    private String packageName;
    private long limitMillis;
    private boolean isBlocked;
    private String setBy;
    private long updatedAt;

    public AppLimit() {
        // Required empty constructor for Firestore
    }

    public AppLimit(String appName, String packageName, long limitMillis,
                    boolean isBlocked, String setBy) {
        this.appName = appName;
        this.packageName = packageName;
        this.limitMillis = limitMillis;
        this.isBlocked = isBlocked;
        this.setBy = setBy;
        this.updatedAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("appName", appName);
        map.put("packageName", packageName);
        map.put("limitMillis", limitMillis);
        map.put("isBlocked", isBlocked);
        map.put("setBy", setBy);
        map.put("updatedAt", updatedAt);
        return map;
    }

    // Getters and Setters
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public long getLimitMillis() { return limitMillis; }
    public void setLimitMillis(long limitMillis) { this.limitMillis = limitMillis; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public String getSetBy() { return setBy; }
    public void setSetBy(String setBy) { this.setBy = setBy; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
