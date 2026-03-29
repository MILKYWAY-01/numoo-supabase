package com.example.numoo.supabase

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Maps to public.users. PostgREST JSON keys must match DB column names.
 * Supabase/Postgres defaults use snake_case (admin_id, admin_code, created_at).
 */
@Serializable
data class SupabaseUserDto(
    val uid: String,
    val name: String,
    val username: String,
    val email: String,
    val role: String,
    @SerialName("admin_id") val adminId: String,
    @SerialName("admin_code") val adminCode: String,
    @SerialName("created_at") val createdAt: Long
)

class SupabaseAuthHelper(context: Context) {

    private val PREFS_NAME = "numoo_prefs"
    private val KEY_ROLE = "user_role"
    private val KEY_ADMIN_ID = "admin_id"
    private val KEY_USER_NAME = "user_name"
    private val KEY_USERNAME = "username"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val client = SupabaseClientConfig.client
    private val scope = CoroutineScope(Dispatchers.IO)

    interface AuthCallback {
        fun onSuccess(message: String?)
        fun onError(error: String?)
    }

    interface RoleCallback {
        fun onRoleFound(role: String?)
        fun onError(error: String?)
    }

    fun isLoggedIn(): Boolean {
        return client.auth.currentSessionOrNull() != null
    }

    fun getCurrentUid(): String? {
        return client.auth.currentSessionOrNull()?.user?.id
    }

    fun getCachedRole(): String? = prefs.getString(KEY_ROLE, null)
    fun getCachedAdminId(): String? = prefs.getString(KEY_ADMIN_ID, null)
    fun getCachedUserName(): String? = prefs.getString(KEY_USER_NAME, "User")
    fun getCachedUsername(): String? = prefs.getString(KEY_USERNAME, "")

    fun registerAdmin(name: String, username: String, email: String, pass: String, callback: AuthCallback) {
        scope.launch {
            try {
                val userResult = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                
                val uid = userResult?.id ?: throw Exception("Registration failed, no ID returned")

                // RLS policies in `supabase_schema.sql` only allow access for authenticated users.
                // So do any `users` table reads/writes *after* signUp.
                val existing = client.postgrest["users"]
                    .select { filter { eq("username", username) } }
                    .decodeList<SupabaseUserDto>()
                if (existing.isNotEmpty()) {
                    withContext(Dispatchers.Main) { callback.onError("Username already taken") }
                    return@launch
                }

                val adminCode = UUID.randomUUID().toString().substring(0, 8).uppercase()

                val dto = SupabaseUserDto(uid, name, username, email, "ADMIN", uid, adminCode, System.currentTimeMillis())
                client.postgrest["users"].insert(dto)

                saveUserPrefs("ADMIN", uid, name, username)
                withContext(Dispatchers.Main) { callback.onSuccess(adminCode) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Unknown error") }
            }
        }
    }

    fun registerUser(name: String, username: String, email: String, pass: String, adminCode: String, callback: AuthCallback) {
        scope.launch {
            try {
                val userResult = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }

                val uid = userResult?.id ?: throw Exception("Registration failed")

                // RLS policies require an authenticated session for `users` table access.
                // Therefore validate admin code + username uniqueness after signUp.
                val adminQuery = client.postgrest["users"]
                    .select { filter { eq("admin_code", adminCode.uppercase()); eq("role", "ADMIN") } }
                    .decodeList<SupabaseUserDto>()
                
                if (adminQuery.isEmpty()) {
                    withContext(Dispatchers.Main) { callback.onError("Invalid admin code") }
                    return@launch
                }
                val adminId = adminQuery[0].uid

                val userQuery = client.postgrest["users"]
                    .select { filter { eq("username", username) } }
                    .decodeList<SupabaseUserDto>()
                if (userQuery.isNotEmpty()) {
                    withContext(Dispatchers.Main) { callback.onError("Username already taken") }
                    return@launch
                }

                val dto = SupabaseUserDto(uid, name, username, email, "USER", adminId, "", System.currentTimeMillis())
                
                client.postgrest["users"].insert(dto)

                saveUserPrefs("USER", adminId, name, username)
                withContext(Dispatchers.Main) { callback.onSuccess("Registration successful") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Unknown error") }
            }
        }
    }

    fun login(email: String, pass: String, callback: AuthCallback) {
        scope.launch {
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                val uid = client.auth.currentSessionOrNull()?.user?.id ?: throw Exception("Login failed")
                fetchAndCacheUserRoleNow(uid, callback)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Unknown error") }
            }
        }
    }

    fun fetchAndCacheUserRole(uid: String, callback: AuthCallback) {
        scope.launch {
            try {
                fetchAndCacheUserRoleNow(uid, callback)
            } catch(e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Unknown error") }
            }
        }
    }

    private suspend fun fetchAndCacheUserRoleNow(uid: String, callback: AuthCallback) {
        val userQuery = client.postgrest["users"]
            .select { filter { eq("uid", uid) } }
            .decodeList<SupabaseUserDto>()
        
        if (userQuery.isNotEmpty()) {
            val user = userQuery[0]
            saveUserPrefs(user.role, user.adminId, user.name, user.username)
            withContext(Dispatchers.Main) { callback.onSuccess(user.role) }
        } else {
            withContext(Dispatchers.Main) { callback.onError("User document not found") }
        }
    }

    fun getUserRole(callback: RoleCallback) {
        val cachedRole = getCachedRole()
        if (cachedRole != null) {
            callback.onRoleFound(cachedRole)
            return
        }

        val uid = getCurrentUid()
        if (uid == null) {
            callback.onError("Not logged in")
            return
        }

        scope.launch {
            try {
                val userQuery = client.postgrest["users"]
                    .select { filter { eq("uid", uid) } }
                    .decodeList<SupabaseUserDto>()
                if (userQuery.isNotEmpty()) {
                    val user = userQuery[0]
                    saveUserPrefs(user.role, user.adminId, user.name, user.username)
                    withContext(Dispatchers.Main) { callback.onRoleFound(user.role) }
                } else {
                    withContext(Dispatchers.Main) { callback.onError("User not found") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Unknown error") }
            }
        }
    }

    fun logout() {
        scope.launch {
            try { client.auth.signOut() } catch(e: Exception) {}
        }
        prefs.edit().clear().apply()
    }

    private fun saveUserPrefs(role: String, adminId: String, name: String, username: String) {
        prefs.edit()
            .putString(KEY_ROLE, role)
            .putString(KEY_ADMIN_ID, adminId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USERNAME, username)
            .apply()
    }
}
