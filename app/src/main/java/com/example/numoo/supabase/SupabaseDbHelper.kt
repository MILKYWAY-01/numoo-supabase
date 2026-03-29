package com.example.numoo.supabase

import android.content.Context
import android.util.Log
import com.example.numoo.models.AppLimit
import com.example.numoo.models.NotificationRequest
import com.example.numoo.models.UsageData
import com.example.numoo.models.User
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class SupabaseUsageDataDto(
    val uid: String,
    val date: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("app_name") val appName: String,
    @SerialName("usage_time_millis") val usageTimeMillis: Long,
    @SerialName("last_updated") val lastUpdated: Long
)

@Serializable
data class SupabaseAppLimitDto(
    @SerialName("user_id") val userId: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("app_name") val appName: String,
    @SerialName("limit_millis") val limitMillis: Long,
    @SerialName("is_blocked") val isBlocked: Boolean,
    @SerialName("set_by") val setBy: String,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class SupabaseNotificationDto(
    @SerialName("notif_id") val notifId: String,
    @SerialName("admin_id") val adminId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("app_name") val appName: String,
    val message: String,
    val status: String,
    val timestamp: Long
)

class SupabaseDbHelper(context: Context) {

    private val TAG = "SupabaseDbHelper"
    private val client = SupabaseClientConfig.client
    private val scope = CoroutineScope(Dispatchers.IO)
    private var limitsJob: Job? = null

    interface FirestoreCallback<T> {
        fun onSuccess(result: T?)
        fun onError(error: String?)
    }

    // ==================== USAGE DATA ====================

    fun updateUsageData(uid: String, usageData: UsageData) {
        scope.launch {
            try {
                val today = getTodayDate()
                val dto = SupabaseUsageDataDto(
                    uid = uid,
                    date = today,
                    packageName = usageData.packageName ?: "",
                    appName = usageData.appName ?: "",
                    usageTimeMillis = usageData.usageTimeMillis,
                    lastUpdated = usageData.lastUpdated
                )
                // Upsert based on composite key (uid, date, packageName)
                client.postgrest["usage_data"].upsert(dto)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed for " + usageData.packageName, e)
            }
        }
    }

    fun getUsageDataForDate(uid: String, date: String, callback: FirestoreCallback<List<UsageData>>) {
        scope.launch {
            try {
                val result = client.postgrest["usage_data"]
                    .select { filter { eq("uid", uid); eq("date", date) } }
                    .decodeList<SupabaseUsageDataDto>()

                val list = result.map { dto ->
                    val ud = UsageData(dto.appName, dto.packageName, dto.usageTimeMillis)
                    ud.lastUpdated = dto.lastUpdated
                    ud
                }
                withContext(Dispatchers.Main) { callback.onSuccess(list) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message) }
            }
        }
    }

    // ==================== LIMITS ====================

    fun setAppLimit(userId: String, limit: AppLimit, callback: FirestoreCallback<Void>) {
        scope.launch {
            try {
                val dto = SupabaseAppLimitDto(
                    userId = userId,
                    packageName = limit.packageName ?: "",
                    appName = limit.appName ?: "",
                    limitMillis = limit.limitMillis,
                    isBlocked = limit.isBlocked,
                    setBy = limit.setBy ?: "",
                    updatedAt = limit.updatedAt
                )
                client.postgrest["app_limits"].upsert(dto)
                withContext(Dispatchers.Main) { callback.onSuccess(null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message) }
            }
        }
    }

    fun getAppLimits(userId: String, callback: FirestoreCallback<List<AppLimit>>) {
        scope.launch {
            try {
                val result = client.postgrest["app_limits"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<SupabaseAppLimitDto>()

                val list = result.map { it.toAppLimit() }
                withContext(Dispatchers.Main) { callback.onSuccess(list) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message) }
            }
        }
    }

    fun listenToLimits(userId: String, callback: FirestoreCallback<List<AppLimit>>) {
        removeLimitsListener()
        // First fetch current state
        getAppLimits(userId, callback)

        // Then listen for changes
        limitsJob = scope.launch {
            try {
                val channel = client.channel("public:app_limits")
                val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "app_limits"
                    filter = "user_id=eq.$userId"
                }

                flow.onEach {
                    // On any change, re-fetch. Alternatively we could map the payload directly.
                    getAppLimits(userId, callback)
                }.launchIn(this)
                
                channel.subscribe()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message) }
            }
        }
    }

    fun removeLimitsListener() {
        limitsJob?.cancel()
        limitsJob = null
    }

    // ==================== USERS ====================

    fun getLinkedUsers(adminId: String, callback: FirestoreCallback<List<User>>) {
        scope.launch {
            try {
                val result = client.postgrest["users"]
                    .select { filter { eq("admin_id", adminId); eq("role", "USER") } }
                    .decodeList<SupabaseUserDto>()

                val users = result.map { it.toUser() }
                withContext(Dispatchers.Main) { callback.onSuccess(users) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message) }
            }
        }
    }

    fun getUserInfo(uid: String, callback: FirestoreCallback<User>) {
        scope.launch {
            try {
                val result = client.postgrest["users"]
                    .select { filter { eq("uid", uid) } }
                    .decodeList<SupabaseUserDto>()

                if (result.isNotEmpty()) {
                    withContext(Dispatchers.Main) { callback.onSuccess(result[0].toUser()) }
                } else {
                    withContext(Dispatchers.Main) { callback.onError("User not found") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message) }
            }
        }
    }

    // ==================== NOTIFICATIONS ====================

    fun sendTimeRequest(adminId: String, request: NotificationRequest, callback: FirestoreCallback<Void>) {
        scope.launch {
            try {
                val dto = SupabaseNotificationDto(
                    notifId = request.notifId ?: "",
                    adminId = adminId,
                    userId = request.userId ?: "",
                    userName = request.userName ?: "",
                    packageName = request.packageName ?: "",
                    appName = request.appName ?: "",
                    message = request.message ?: "",
                    status = request.status ?: "PENDING",
                    timestamp = request.timestamp
                )
                client.postgrest["notifications"].insert(dto)
                withContext(Dispatchers.Main) { callback.onSuccess(null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message) }
            }
        }
    }

    fun getNotifications(adminId: String, callback: FirestoreCallback<List<NotificationRequest>>) {
        scope.launch {
            try {
                // order by timestamp desc
                val result = client.postgrest["notifications"]
                    .select { 
                        filter { eq("admin_id", adminId) }
                        order("timestamp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<SupabaseNotificationDto>()

                val list = result.map { it.toNotificationRequest() }
                withContext(Dispatchers.Main) { callback.onSuccess(list) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message) }
            }
        }
    }

    // ==================== HELPERS ====================

    private fun SupabaseUserDto.toUser(): User {
        val user = User(uid, name, username, email, role, adminId, adminCode)
        user.createdAt = createdAt
        return user
    }

    private fun SupabaseAppLimitDto.toAppLimit(): AppLimit {
        val limit = AppLimit(appName, packageName, limitMillis, isBlocked, setBy)
        limit.updatedAt = updatedAt
        return limit
    }

    private fun SupabaseNotificationDto.toNotificationRequest(): NotificationRequest {
        val req = NotificationRequest(userId, userName, packageName, appName, message)
        req.notifId = notifId
        req.status = status
        req.timestamp = timestamp
        return req
    }

    companion object {
        @JvmStatic
        fun getTodayDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }
}
