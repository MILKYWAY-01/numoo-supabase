package com.example.numoo.models;

import java.util.HashMap;
import java.util.Map;

public class UsageData {
    private String appName;
    private String packageName;
    private long usageTimeMillis;
    private long lastUpdated;

    public UsageData() {
        // Required empty constructor for Firestore
    }

    public UsageData(String appName, String packageName, long usageTimeMillis) {
        this.appName = appName;
        this.packageName = packageName;
        this.usageTimeMillis = usageTimeMillis;
        this.lastUpdated = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("appName", appName);
        map.put("packageName", packageName);
        map.put("usageTimeMillis", usageTimeMillis);
        map.put("lastUpdated", lastUpdated);
        return map;
    }

    // Getters and Setters
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public long getUsageTimeMillis() { return usageTimeMillis; }
    public void setUsageTimeMillis(long usageTimeMillis) { this.usageTimeMillis = usageTimeMillis; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
