package com.example.data.remote

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ManagedUserRecord(
    val userId: String,
    val displayName: String,
    val email: String,
    val planTier: String = PLAN_FREE,
    val isEnabled: Boolean = true,
    val source: String = SOURCE_ADMIN,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val isPro: Boolean get() = planTier.equals(PLAN_PRO, ignoreCase = true)

    companion object {
        const val PLAN_FREE = "FREE"
        const val PLAN_PRO = "PRO"
        const val SOURCE_DEVICE = "DEVICE"
        const val SOURCE_GOOGLE = "GOOGLE"
        const val SOURCE_ADMIN = "ADMIN"
        const val PRIMARY_DEVICE_USER_ID = "primary_device_user"
    }
}

object AdminUserManagementStore {

    private const val PREFS_NAME = "focuslock_admin_users"
    private const val KEY_USERS_JSON = "managed_users_json"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val _users = MutableStateFlow<List<ManagedUserRecord>>(emptyList())
    val users: StateFlow<List<ManagedUserRecord>> = _users.asStateFlow()

    @Volatile
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_USERS_JSON, null)
            val parsed = parseJsonList(raw).toMutableList()
            if (parsed.none { it.userId == ManagedUserRecord.PRIMARY_DEVICE_USER_ID || it.source == ManagedUserRecord.SOURCE_GOOGLE }) {
                parsed.add(
                    0,
                    ManagedUserRecord(
                        userId = ManagedUserRecord.PRIMARY_DEVICE_USER_ID,
                        displayName = "This Device (Primary)",
                        email = "device@focuslock.local",
                        planTier = ManagedUserRecord.PLAN_FREE,
                        isEnabled = true,
                        source = ManagedUserRecord.SOURCE_DEVICE,
                        notes = "Active app user on this device"
                    )
                )
            }
            _users.value = parsed.sortedWith(
                compareByDescending<ManagedUserRecord> { it.userId == ManagedUserRecord.PRIMARY_DEVICE_USER_ID }
                    .thenByDescending { it.source == ManagedUserRecord.SOURCE_GOOGLE }
                    .thenByDescending { it.updatedAtMs }
            )
            persist(context, _users.value)
            loaded = true
        }
    }

    fun syncActiveDeviceAccount(
        context: Context,
        signedInUser: SupabaseUser?,
        isDevicePro: Boolean
    ) {
        ensureLoaded(context)
        val desiredTier = if (isDevicePro) ManagedUserRecord.PLAN_PRO else ManagedUserRecord.PLAN_FREE
        val list = _users.value.toMutableList()

        if (signedInUser != null) {
            val email = signedInUser.email?.trim().orEmpty().ifBlank { "${signedInUser.uid}@google.focuslock" }
            val name = signedInUser.displayName?.trim().orEmpty().ifBlank { email.substringBefore("@") }
            val existingIdx = list.indexOfFirst {
                it.userId == signedInUser.uid ||
                    (it.email.equals(email, ignoreCase = true) && it.userId != ManagedUserRecord.PRIMARY_DEVICE_USER_ID)
            }
            if (existingIdx >= 0) {
                val cur = list[existingIdx]
                list[existingIdx] = cur.copy(
                    userId = signedInUser.uid,
                    displayName = name,
                    email = email,
                    planTier = desiredTier,
                    source = ManagedUserRecord.SOURCE_GOOGLE,
                    updatedAtMs = System.currentTimeMillis()
                )
            } else {
                list.add(
                    0,
                    ManagedUserRecord(
                        userId = signedInUser.uid,
                        displayName = name,
                        email = email,
                        planTier = desiredTier,
                        isEnabled = true,
                        source = ManagedUserRecord.SOURCE_GOOGLE,
                        notes = "Google signed-in user"
                    )
                )
            }
        }

        val primaryIdx = list.indexOfFirst { it.userId == ManagedUserRecord.PRIMARY_DEVICE_USER_ID }
        if (primaryIdx >= 0) {
            list[primaryIdx] = list[primaryIdx].copy(
                planTier = desiredTier,
                updatedAtMs = System.currentTimeMillis()
            )
        }

        val sorted = sortUsers(list)
        _users.value = sorted
        persist(context, sorted)
    }

    suspend fun registerUser(
        context: Context,
        displayName: String,
        email: String,
        initialTier: String,
        passwordOptional: String = "",
        notes: String = ""
    ): Result<ManagedUserRecord> {
        ensureLoaded(context)
        val cleanName = displayName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanTier = if (initialTier.equals(ManagedUserRecord.PLAN_PRO, ignoreCase = true)) {
            ManagedUserRecord.PLAN_PRO
        } else {
            ManagedUserRecord.PLAN_FREE
        }

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter the user's full name."))
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.substringAfter("@").contains(".")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        val cleanPassword = passwordOptional.trim()
        if (cleanPassword.isNotEmpty() && cleanPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Optional password must be at least 6 characters."))
        }

        val current = _users.value
        if (current.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("A user with $cleanEmail is already registered."))
        }

        var finalNotes = notes.trim()
        var registeredUid = "usr_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        if (cleanPassword.length >= 6) {
            val sbResult = trySupabasePasswordSignup(cleanName, cleanEmail, cleanPassword)
            if (sbResult != null) {
                registeredUid = sbResult.first ?: registeredUid
                if (finalNotes.isBlank()) {
                    finalNotes = sbResult.second
                }
            }
        } else if (finalNotes.isBlank()) {
            finalNotes = "Registered by Admin"
        }

        val record = ManagedUserRecord(
            userId = registeredUid,
            displayName = cleanName,
            email = cleanEmail,
            planTier = cleanTier,
            isEnabled = true,
            source = ManagedUserRecord.SOURCE_ADMIN,
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis(),
            notes = finalNotes
        )

        val updated = sortUsers(listOf(record) + current)
        _users.value = updated
        persist(context, updated)
        return Result.success(record)
    }

    fun setUserPlan(context: Context, userId: String, planTier: String): ManagedUserRecord? {
        ensureLoaded(context)
        val normalizedTier = if (planTier.equals(ManagedUserRecord.PLAN_PRO, ignoreCase = true)) {
            ManagedUserRecord.PLAN_PRO
        } else {
            ManagedUserRecord.PLAN_FREE
        }
        var updatedItem: ManagedUserRecord? = null
        val updated = _users.value.map { user ->
            if (user.userId == userId) {
                user.copy(planTier = normalizedTier, updatedAtMs = System.currentTimeMillis()).also {
                    updatedItem = it
                }
            } else {
                user
            }
        }
        _users.value = updated
        persist(context, updated)
        return updatedItem
    }

    fun setUserEnabled(context: Context, userId: String, enabled: Boolean): ManagedUserRecord? {
        ensureLoaded(context)
        var updatedItem: ManagedUserRecord? = null
        val updated = _users.value.map { user ->
            if (user.userId == userId) {
                user.copy(isEnabled = enabled, updatedAtMs = System.currentTimeMillis()).also {
                    updatedItem = it
                }
            } else {
                user
            }
        }
        _users.value = updated
        persist(context, updated)
        return updatedItem
    }

    fun deleteUser(context: Context, userId: String): Boolean {
        ensureLoaded(context)
        if (userId == ManagedUserRecord.PRIMARY_DEVICE_USER_ID) return false
        val before = _users.value.size
        val updated = _users.value.filterNot { it.userId == userId }
        if (updated.size == before) return false
        _users.value = updated
        persist(context, updated)
        return true
    }

    private suspend fun trySupabasePasswordSignup(
        displayName: String,
        email: String,
        password: String
    ): Pair<String?, String>? = withContext(Dispatchers.IO) {
        val config = SupabaseAuthService.resolveSupabaseConfig() ?: return@withContext null
        runCatching {
            val payload = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("data", JSONObject().put("full_name", displayName))
            val reqBuilder = Request.Builder()
                .url("${config.first}/auth/v1/signup")
                .addHeader("apikey", config.second)
                .addHeader("Content-Type", "application/json")
            if (config.second.startsWith("eyJ")) {
                reqBuilder.addHeader("Authorization", "Bearer ${config.second}")
            }
            val req = reqBuilder
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val obj = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
                if (resp.isSuccessful) {
                    val sbUid = obj.optJSONObject("user")?.optString("id")
                        ?.takeIf { it.isNotBlank() }
                        ?: obj.optString("id").takeIf { it.isNotBlank() }
                    sbUid to "Supabase Auth + Admin Directory"
                } else {
                    null to "Admin Directory (Supabase pending)"
                }
            }
        }.getOrElse {
            null to "Registered locally (offline)"
        }
    }

    private fun sortUsers(list: List<ManagedUserRecord>): List<ManagedUserRecord> {
        return list.sortedWith(
            compareByDescending<ManagedUserRecord> { it.userId == ManagedUserRecord.PRIMARY_DEVICE_USER_ID }
                .thenByDescending { it.source == ManagedUserRecord.SOURCE_GOOGLE }
                .thenByDescending { it.createdAtMs }
        )
    }

    private fun persist(context: Context, list: List<ManagedUserRecord>) {
        val arr = JSONArray()
        list.forEach { u ->
            arr.put(
                JSONObject().apply {
                    put("userId", u.userId)
                    put("displayName", u.displayName)
                    put("email", u.email)
                    put("planTier", u.planTier)
                    put("isEnabled", u.isEnabled)
                    put("source", u.source)
                    put("createdAtMs", u.createdAtMs)
                    put("updatedAtMs", u.updatedAtMs)
                    put("notes", u.notes)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERS_JSON, arr.toString())
            .apply()
    }

    private fun parseJsonList(raw: String?): List<ManagedUserRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val uid = o.optString("userId").ifBlank { continue }
                    add(
                        ManagedUserRecord(
                            userId = uid,
                            displayName = o.optString("displayName", "FocusLock User"),
                            email = o.optString("email", ""),
                            planTier = o.optString("planTier", ManagedUserRecord.PLAN_FREE),
                            isEnabled = o.optBoolean("isEnabled", true),
                            source = o.optString("source", ManagedUserRecord.SOURCE_ADMIN),
                            createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis()),
                            updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis()),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @VisibleForTesting
    internal fun resetForTests(context: Context) {
        loaded = false
        _users.value = emptyList()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
