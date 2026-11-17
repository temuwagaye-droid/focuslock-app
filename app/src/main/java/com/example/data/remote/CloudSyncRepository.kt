package com.example.data.remote

import com.example.data.model.BlockedAppEntity
import com.example.data.model.SessionLogEntity
import com.example.data.model.TodoEntity
import com.example.telemetry.TelemetryLogEntry
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Authentication required to access cloud sync.")
    }

    suspend fun syncProfileAndSettings(
        displayName: String,
        focusScore: Int,
        totalFocusMinutes: Int,
        completedSessionsCount: Int,
        currentStreak: Int,
        totalBlockedAttempts: Int,
        treeStage: Int,
        isLeaderboardVisible: Boolean,
        settings: CloudPrivateSettings
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val now = Timestamp.now()
            val userRef = db.collection("users").document(uid)
            val existing = userRef.get().awaitResult()
            val createdAt = existing.getTimestamp("createdAt") ?: now

            val profileMap = hashMapOf<String, Any>(
                "userId" to uid,
                "displayName" to displayName.take(50).ifBlank { "Reader" },
                "focusScore" to focusScore.coerceAtLeast(0),
                "totalFocusMinutes" to totalFocusMinutes.coerceAtLeast(0),
                "completedSessionsCount" to completedSessionsCount.coerceAtLeast(0),
                "currentStreak" to currentStreak.coerceAtLeast(0),
                "totalBlockedAttempts" to totalBlockedAttempts.coerceAtLeast(0),
                "treeStage" to treeStage.coerceIn(1, 5),
                "isLeaderboardVisible" to isLeaderboardVisible,
                "isPro" to settings.isPro,
                "createdAt" to createdAt,
                "updatedAt" to now
            )
            userRef.set(profileMap).awaitResult()

            val settingsMap = hashMapOf<String, Any>(
                "userId" to uid,
                "isDarkMode" to settings.isDarkMode,
                "accentTheme" to settings.accentTheme.take(32),
                "isWhitelistMode" to settings.isWhitelistMode,
                "designatedReadingApp" to settings.designatedReadingApp.take(160),
                "sessionDurationMinutes" to settings.sessionDurationMinutes,
                "autoDndEnabled" to settings.autoDndEnabled,
                "ambientSound" to settings.ambientSound.take(32),
                "reminderEnabled" to settings.reminderEnabled,
                "reminderHour" to settings.reminderHour,
                "reminderMinute" to settings.reminderMinute,
                "partnerName" to settings.partnerName.take(60),
                "partnerEmail" to settings.partnerEmail.take(120),
                "notifyPartnerOnQuit" to settings.notifyPartnerOnQuit,
                "parentalLockEnabled" to settings.parentalLockEnabled,
                "parentalPinHash" to settings.parentalPinHash.take(128),
                "analyticsOptIn" to settings.analyticsOptIn,
                "crashReportingOptIn" to settings.crashReportingOptIn,
                "cloudSyncEnabled" to settings.cloudSyncEnabled,
                "isPro" to settings.isPro,
                "updatedAt" to now
            )
            userRef.collection("private").document("settings").set(settingsMap).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "profile sync")))
        }
    }

    suspend fun backupUserData(
        sessions: List<SessionLogEntity>,
        blockedApps: List<BlockedAppEntity>,
        todos: List<TodoEntity>
    ): Result<Int> {
        return try {
            val uid = requireUid()
            val userRef = db.collection("users").document(uid)
            val now = Timestamp.now()
            val batch = db.batch()
            var count = 0

            sessions.take(50).forEach { s ->
                val sid = "s_${s.timestamp}_${s.id}"
                val data = hashMapOf<String, Any>(
                    "userId" to uid,
                    "sessionId" to sid,
                    "timestampMs" to s.timestamp.coerceAtLeast(0L),
                    "durationSeconds" to s.durationSeconds.coerceAtLeast(0L),
                    "blockedAttempts" to s.blockedAttempts.coerceAtLeast(0),
                    "isCompleted" to s.isCompleted,
                    "readingAppPackage" to (s.readingAppPackage ?: "").take(160),
                    "createdAt" to now
                )
                batch.set(userRef.collection("sessions").document(sid), data)
                count++
            }

            blockedApps.take(80).forEach { app ->
                val docId = app.packageName.replace(".", "_")
                val data = hashMapOf<String, Any>(
                    "userId" to uid,
                    "packageName" to app.packageName.take(160),
                    "appName" to app.appName.take(100),
                    "isBlocked" to app.isBlocked,
                    "updatedAt" to now
                )
                batch.set(userRef.collection("blockedApps").document(docId), data)
                count++
            }

            todos.take(80).forEach { todo ->
                val tid = "t_${todo.timestamp}_${todo.id}"
                val data = hashMapOf<String, Any>(
                    "userId" to uid,
                    "todoId" to tid,
                    "title" to todo.title.take(200).ifBlank { "Reading Task" },
                    "isCompleted" to todo.isCompleted,
                    "timestampMs" to todo.timestamp.coerceAtLeast(0L),
                    "updatedAt" to now
                )
                batch.set(userRef.collection("todos").document(tid), data)
                count++
            }

            if (count > 0) {
                batch.commit().awaitResult()
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "cloud backup")))
        }
    }

    suspend fun fetchCloudRestoreBundle(): Result<CloudRestoreBundle> {
        return try {
            val uid = requireUid()
            val userRef = db.collection("users").document(uid)

            val settingsSnap = userRef.collection("private").document("settings").get().awaitResult()
            val settings = if (settingsSnap.exists()) {
                CloudPrivateSettings(
                    userId = settingsSnap.getString("userId") ?: uid,
                    isDarkMode = settingsSnap.getBoolean("isDarkMode") ?: false,
                    accentTheme = settingsSnap.getString("accentTheme") ?: "violet",
                    isWhitelistMode = settingsSnap.getBoolean("isWhitelistMode") ?: false,
                    designatedReadingApp = settingsSnap.getString("designatedReadingApp") ?: "",
                    sessionDurationMinutes = (settingsSnap.getLong("sessionDurationMinutes") ?: 15L).toInt(),
                    autoDndEnabled = settingsSnap.getBoolean("autoDndEnabled") ?: false,
                    ambientSound = settingsSnap.getString("ambientSound") ?: "none",
                    reminderEnabled = settingsSnap.getBoolean("reminderEnabled") ?: false,
                    reminderHour = (settingsSnap.getLong("reminderHour") ?: 20L).toInt(),
                    reminderMinute = (settingsSnap.getLong("reminderMinute") ?: 0L).toInt(),
                    partnerName = settingsSnap.getString("partnerName") ?: "",
                    partnerEmail = settingsSnap.getString("partnerEmail") ?: "",
                    notifyPartnerOnQuit = settingsSnap.getBoolean("notifyPartnerOnQuit") ?: false,
                    parentalLockEnabled = settingsSnap.getBoolean("parentalLockEnabled") ?: false,
                    parentalPinHash = settingsSnap.getString("parentalPinHash") ?: "",
                    analyticsOptIn = settingsSnap.getBoolean("analyticsOptIn") ?: true,
                    crashReportingOptIn = settingsSnap.getBoolean("crashReportingOptIn") ?: true,
                    cloudSyncEnabled = settingsSnap.getBoolean("cloudSyncEnabled") ?: true,
                    isPro = settingsSnap.getBoolean("isPro") ?: false,
                    updatedAt = settingsSnap.getTimestamp("updatedAt")
                )
            } else null

            val sessionsSnap = userRef.collection("sessions").get().awaitResult()
            val sessions = sessionsSnap.documents.map { d ->
                CloudSessionBackup(
                    userId = d.getString("userId") ?: uid,
                    sessionId = d.getString("sessionId") ?: d.id,
                    timestampMs = d.getLong("timestampMs") ?: 0L,
                    durationSeconds = d.getLong("durationSeconds") ?: 0L,
                    blockedAttempts = (d.getLong("blockedAttempts") ?: 0L).toInt(),
                    isCompleted = d.getBoolean("isCompleted") ?: true,
                    readingAppPackage = d.getString("readingAppPackage") ?: "",
                    createdAt = d.getTimestamp("createdAt")
                )
            }

            val appsSnap = userRef.collection("blockedApps").get().awaitResult()
            val apps = appsSnap.documents.map { d ->
                CloudBlockedAppBackup(
                    userId = d.getString("userId") ?: uid,
                    packageName = d.getString("packageName") ?: "",
                    appName = d.getString("appName") ?: "",
                    isBlocked = d.getBoolean("isBlocked") ?: true,
                    updatedAt = d.getTimestamp("updatedAt")
                )
            }.filter { it.packageName.isNotBlank() }

            val todosSnap = userRef.collection("todos").get().awaitResult()
            val todos = todosSnap.documents.map { d ->
                CloudTodoBackup(
                    userId = d.getString("userId") ?: uid,
                    todoId = d.getString("todoId") ?: d.id,
                    title = d.getString("title") ?: "",
                    isCompleted = d.getBoolean("isCompleted") ?: false,
                    timestampMs = d.getLong("timestampMs") ?: 0L,
                    updatedAt = d.getTimestamp("updatedAt")
                )
            }.filter { it.title.isNotBlank() }

            Result.success(CloudRestoreBundle(settings, sessions, apps, todos))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "cloud restore")))
        }
    }

    suspend fun uploadTelemetry(entry: TelemetryLogEntry): Result<Unit> {
        return try {
            val uid = requireUid()
            val colName = if (entry.kind == "crash") "crashReports" else "analytics"
            val docId = entry.id.take(64).ifBlank { "log_${System.currentTimeMillis()}" }
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "kind" to entry.kind.take(30),
                "name" to entry.name.take(80),
                "detail" to entry.detail.take(1500),
                "createdAt" to Timestamp.now()
            )
            db.collection("users").document(uid).collection(colName).document(docId).set(payload).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "telemetry upload")))
        }
    }

    suspend fun deleteAllCloudData(): Result<Unit> {
        return try {
            val uid = requireUid()
            val userRef = db.collection("users").document(uid)
            val subcollections = listOf("private", "sessions", "blockedApps", "todos", "analytics", "crashReports")
            for (sub in subcollections) {
                val snap = userRef.collection(sub).get().awaitResult()
                for (doc in snap.documents) {
                    doc.reference.delete().awaitResult()
                }
            }
            userRef.delete().awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "cloud data deletion")))
        }
    }

    fun observeLeaderboard(limit: Long = 30): Flow<List<CloudUserProfile>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return callbackFlow {
            val registration = db.collection("users")
                .whereEqualTo("isLeaderboardVisible", true)
                .orderBy("focusScore", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val profiles = snap?.documents?.map { d ->
                        CloudUserProfile(
                            userId = d.getString("userId") ?: d.id,
                            displayName = d.getString("displayName") ?: "Reader",
                            focusScore = (d.getLong("focusScore") ?: 0L).toInt(),
                            totalFocusMinutes = (d.getLong("totalFocusMinutes") ?: 0L).toInt(),
                            completedSessionsCount = (d.getLong("completedSessionsCount") ?: 0L).toInt(),
                            currentStreak = (d.getLong("currentStreak") ?: 0L).toInt(),
                            totalBlockedAttempts = (d.getLong("totalBlockedAttempts") ?: 0L).toInt(),
                            treeStage = (d.getLong("treeStage") ?: 1L).toInt(),
                            isLeaderboardVisible = d.getBoolean("isLeaderboardVisible") ?: true,
                            isPro = d.getBoolean("isPro") ?: false,
                            createdAt = d.getTimestamp("createdAt"),
                            updatedAt = d.getTimestamp("updatedAt")
                        )
                    }.orEmpty()
                    trySend(profiles)
                }
            awaitClose { registration.remove() }
        }
    }
}
