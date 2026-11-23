package com.example.data.remote

data class CloudUserProfile(
    val userId: String = "",
    val displayName: String = "Reader",
    val focusScore: Int = 0,
    val totalFocusMinutes: Int = 0,
    val completedSessionsCount: Int = 0,
    val currentStreak: Int = 0,
    val totalBlockedAttempts: Int = 0,
    val treeStage: Int = 1,
    val isLeaderboardVisible: Boolean = true,
    val isPro: Boolean = false,
    val createdAtMs: Long? = null,
    val updatedAtMs: Long? = null
)

data class CloudPrivateSettings(
    val userId: String = "",
    val isDarkMode: Boolean = false,
    val accentTheme: String = "violet",
    val isWhitelistMode: Boolean = false,
    val designatedReadingApp: String = "",
    val sessionDurationMinutes: Int = 15,
    val autoDndEnabled: Boolean = false,
    val ambientSound: String = "none",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val partnerName: String = "",
    val partnerEmail: String = "",
    val notifyPartnerOnQuit: Boolean = false,
    val parentalLockEnabled: Boolean = false,
    val parentalPinHash: String = "",
    val analyticsOptIn: Boolean = true,
    val crashReportingOptIn: Boolean = true,
    val cloudSyncEnabled: Boolean = true,
    val isPro: Boolean = false,
    val updatedAtMs: Long? = null
)

data class CloudSessionBackup(
    val userId: String = "",
    val sessionId: String = "",
    val timestampMs: Long = 0L,
    val durationSeconds: Long = 0L,
    val blockedAttempts: Int = 0,
    val isCompleted: Boolean = true,
    val readingAppPackage: String = "",
    val createdAtMs: Long? = null
)

data class CloudBlockedAppBackup(
    val userId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val isBlocked: Boolean = true,
    val updatedAtMs: Long? = null
)

data class CloudTodoBackup(
    val userId: String = "",
    val todoId: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    val timestampMs: Long = 0L,
    val updatedAtMs: Long? = null
)

data class FocusRoomDto(
    val roomId: String = "",
    val roomCode: String = "",
    val name: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val durationMinutes: Int = 25,
    val status: String = "waiting",
    val participantCount: Int = 1,
    val isPublic: Boolean = true,
    val createdAtMs: Long? = null,
    val updatedAtMs: Long? = null
)

data class RoomParticipantDto(
    val userId: String = "",
    val displayName: String = "",
    val status: String = "ready",
    val distractionAttempts: Int = 0,
    val treeStage: Int = 1,
    val joinedAtMs: Long? = null,
    val updatedAtMs: Long? = null
)

data class RoomMessageDto(
    val messageId: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

data class CloudRestoreBundle(
    val settings: CloudPrivateSettings? = null,
    val sessions: List<CloudSessionBackup> = emptyList(),
    val blockedApps: List<CloudBlockedAppBackup> = emptyList(),
    val todos: List<CloudTodoBackup> = emptyList()
)
