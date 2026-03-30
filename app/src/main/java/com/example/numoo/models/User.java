package com.example.numoo.models;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String name;
    private String username;
    private String email;
    private String role; // "ADMIN" or "USER"
    private String adminId;
    private String adminCode;
    private long createdAt;
    /** Total screen time today (ms), from usage_data — UI only, not stored in DB. */
    private long todayUsageMillis;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String name, String username, String email,
                String role, String adminId, String adminCode) {
        this.uid = uid;
        this.name = name;
        this.username = username;
        this.email = email;
        this.role = role;
        this.adminId = adminId;
        this.adminCode = adminCode;
        this.createdAt = System.currentTimeMillis();
    }

    // Convert to Firestore map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", name);
        map.put("username", username);
        map.put("email", email);
        map.put("role", role);
        map.put("adminId", adminId);
        map.put("adminCode", adminCode);
        map.put("createdAt", createdAt);
        return map;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getAdminCode() { return adminCode; }
    public void setAdminCode(String adminCode) { this.adminCode = adminCode; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getTodayUsageMillis() { return todayUsageMillis; }
    public void setTodayUsageMillis(long todayUsageMillis) { this.todayUsageMillis = todayUsageMillis; }
}
