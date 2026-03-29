package com.example.numoo.firebase;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.numoo.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class FirebaseAuthHelper {

    private static final String PREFS_NAME = "numoo_prefs";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_ADMIN_ID = "admin_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USERNAME = "username";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final SharedPreferences prefs;

    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface RoleCallback {
        void onRoleFound(String role);
        void onError(String error);
    }

    public FirebaseAuthHelper(Context context) {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCachedRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getCachedAdminId() {
        return prefs.getString(KEY_ADMIN_ID, null);
    }

    public String getCachedUserName() {
        return prefs.getString(KEY_USER_NAME, "User");
    }

    public String getCachedUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void registerAdmin(String name, String username, String email, String password, AuthCallback callback) {
        try {
            // Check username uniqueness
            db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(usernameQuery -> {
                    if (!usernameQuery.isEmpty()) {
                        callback.onError("Username already taken");
                        return;
                    }

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser firebaseUser = authResult.getUser();
                            if (firebaseUser == null) {
                                callback.onError("Registration failed");
                                return;
                            }
                            String uid = firebaseUser.getUid();
                            String adminCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                            User user = new User(uid, name, username, email, "ADMIN", uid, adminCode);

                            db.collection("users").document(uid)
                                .set(user.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    saveUserPrefs("ADMIN", uid, name, username);
                                    callback.onSuccess(adminCode);
                                })
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void registerUser(String name, String username, String email,
                             String password, String adminCode, AuthCallback callback) {
        try {
            // First verify admin code exists
            db.collection("users")
                .whereEqualTo("adminCode", adminCode.toUpperCase())
                .whereEqualTo("role", "ADMIN")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError("Invalid admin code");
                        return;
                    }

                    String adminId = querySnapshot.getDocuments().get(0).getId();

                    // Check username uniqueness
                    db.collection("users")
                        .whereEqualTo("username", username)
                        .get()
                        .addOnSuccessListener(usernameQuery -> {
                            if (!usernameQuery.isEmpty()) {
                                callback.onError("Username already taken");
                                return;
                            }

                            // Create auth account
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener(authResult -> {
                                    FirebaseUser firebaseUser = authResult.getUser();
                                    if (firebaseUser == null) {
                                        callback.onError("Registration failed");
                                        return;
                                    }
                                    String uid = firebaseUser.getUid();
                                    User user = new User(uid, name, username, email,
                                            "USER", adminId, "");

                                    db.collection("users").document(uid)
                                        .set(user.toMap())
                                        .addOnSuccessListener(aVoid -> {
                                            saveUserPrefs("USER", adminId, name, username);
                                            callback.onSuccess("Registration successful");
                                        })
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                                })
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void login(String email, String password, AuthCallback callback) {
        try {
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Login failed");
                        return;
                    }
                    // Fetch user role from Firestore
                    fetchAndCacheUserRole(firebaseUser.getUid(), callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void fetchAndCacheUserRole(String uid, AuthCallback callback) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String role = document.getString("role");
                    String adminId = document.getString("adminId");
                    String name = document.getString("name");
                    String username = document.getString("username");
                    saveUserPrefs(role, adminId, name, username != null ? username : "");
                    callback.onSuccess(role != null ? role : "USER");
                } else {
                    callback.onError("User document not found");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserRole(RoleCallback callback) {
        String cachedRole = getCachedRole();
        if (cachedRole != null) {
            callback.onRoleFound(cachedRole);
            return;
        }

        String uid = getCurrentUid();
        if (uid == null) {
            callback.onError("Not logged in");
            return;
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String role = document.getString("role");
                    String adminId = document.getString("adminId");
                    String name = document.getString("name");
                    String username = document.getString("username");
                    saveUserPrefs(role, adminId, name, username != null ? username : "");
                    callback.onRoleFound(role != null ? role : "USER");
                } else {
                    callback.onError("User not found");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void logout() {
        auth.signOut();
        prefs.edit().clear().apply();
    }

    private void saveUserPrefs(String role, String adminId, String name, String username) {
        prefs.edit()
            .putString(KEY_ROLE, role)
            .putString(KEY_ADMIN_ID, adminId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USERNAME, username)
            .apply();
    }
}
