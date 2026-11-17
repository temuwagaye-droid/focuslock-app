package com.example.data.repository

import com.example.data.local.FocusDao
import com.example.data.model.BlockedAppEntity
import com.example.data.model.SessionLogEntity
import com.example.data.model.SettingEntity
import com.example.data.model.TodoEntity
import com.example.data.remote.CloudRestoreBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FocusRepository(private val focusDao: FocusDao) {

    val allBlockedApps: Flow<List<BlockedAppEntity>> = focusDao.getAllBlockedAppsFlow()
    val allSessionLogs: Flow<List<SessionLogEntity>> = focusDao.getAllSessionLogsFlow()
    val allTodoItems: Flow<List<TodoEntity>> = focusDao.getAllTodoItemsFlow()

    suspend fun insertTodoItem(title: String, isCompleted: Boolean = false) {
        focusDao.insertTodoItem(TodoEntity(title = title, isCompleted = isCompleted))
    }

    suspend fun updateTodoItem(item: TodoEntity) {
        focusDao.insertTodoItem(item)
    }

    suspend fun deleteTodoItem(id: Long) {
        focusDao.deleteTodoItem(id)
    }

    suspend fun getBlockedAppsSync(): List<BlockedAppEntity> {
        return focusDao.getAllBlockedApps()
    }

    suspend fun getSessionLogsSync(): List<SessionLogEntity> {
        return focusDao.getAllSessionLogsFlow().first()
    }

    suspend fun getTodoItemsSync(): List<TodoEntity> {
        return focusDao.getAllTodoItemsFlow().first()
    }

    suspend fun insertBlockedApp(packageName: String, appName: String) {
        focusDao.insertBlockedApp(BlockedAppEntity(packageName, appName))
    }

    suspend fun removeBlockedApp(packageName: String) {
        focusDao.deleteBlockedApp(packageName)
    }

    suspend fun clearBlockedApps() {
        focusDao.clearAllBlockedApps()
    }

    suspend fun saveSessionLog(
        durationSeconds: Long,
        blockedAttempts: Int,
        isCompleted: Boolean,
        readingApp: String?
    ) {
        val log = SessionLogEntity(
            durationSeconds = durationSeconds,
            blockedAttempts = blockedAttempts,
            isCompleted = isCompleted,
            readingAppPackage = readingApp
        )
        focusDao.insertSessionLog(log)
    }

    // Core settings
    fun isWhitelistModeEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_WHITELIST_MODE).map { it?.value == "true" }

    suspend fun isWhitelistModeEnabledSync(): Boolean =
        focusDao.getSetting(KEY_WHITELIST_MODE)?.value == "true"

    suspend fun setWhitelistModeEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_WHITELIST_MODE, enabled.toString()))
    }

    fun getReadingAppPackage(): Flow<String?> =
        focusDao.getSettingFlow(KEY_READING_APP_PKG).map { it?.value?.takeIf { pkg -> pkg.isNotBlank() } }

    suspend fun getReadingAppPackageSync(): String? =
        focusDao.getSetting(KEY_READING_APP_PKG)?.value?.takeIf { it.isNotBlank() }

    suspend fun setReadingAppPackage(packageName: String?) {
        focusDao.insertSetting(SettingEntity(KEY_READING_APP_PKG, packageName ?: ""))
    }

    fun isScheduleEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_SCHEDULE_ENABLED).map { it?.value == "true" }

    suspend fun setScheduleEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_ENABLED, enabled.toString()))
    }

    fun getScheduleStartHour(): Flow<Int> =
        focusDao.getSettingFlow(KEY_SCHEDULE_START).map { it?.value?.toIntOrNull() ?: 20 }

    suspend fun setScheduleStartHour(hour: Int) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_START, hour.toString()))
    }

    fun getScheduleEndHour(): Flow<Int> =
        focusDao.getSettingFlow(KEY_SCHEDULE_END).map { it?.value?.toIntOrNull() ?: 21 }

    suspend fun setScheduleEndHour(hour: Int) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_END, hour.toString()))
    }

    fun isDarkModeEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_DARK_MODE).map { it?.value == "true" }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_DARK_MODE, enabled.toString()))
    }

    // Premium & Accent Themes
    fun isProTier(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_PRO_TIER).map { it?.value == "true" }

    suspend fun setProTier(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_PRO_TIER, enabled.toString()))
    }

    fun getAccentThemeId(): Flow<String> =
        focusDao.getSettingFlow(KEY_ACCENT_THEME).map { it?.value?.ifBlank { null } ?: "violet" }

    suspend fun setAccentThemeId(themeId: String) {
        focusDao.insertSetting(SettingEntity(KEY_ACCENT_THEME, themeId))
    }

    // Do Not Disturb & Ambient Sound
    fun isAutoDndEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_AUTO_DND).map { it?.value == "true" }

    suspend fun isAutoDndEnabledSync(): Boolean =
        focusDao.getSetting(KEY_AUTO_DND)?.value == "true"

    suspend fun setAutoDndEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_AUTO_DND, enabled.toString()))
    }

    fun getAmbientSoundId(): Flow<String> =
        focusDao.getSettingFlow(KEY_AMBIENT_SOUND).map { it?.value?.ifBlank { null } ?: "none" }

    suspend fun getAmbientSoundIdSync(): String =
        focusDao.getSetting(KEY_AMBIENT_SOUND)?.value?.ifBlank { null } ?: "none"

    suspend fun setAmbientSoundId(soundId: String) {
        focusDao.insertSetting(SettingEntity(KEY_AMBIENT_SOUND, soundId))
    }

    // Notifications & Reminders
    fun isReminderEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_REMINDER_ENABLED).map { it?.value == "true" }

    suspend fun setReminderEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_REMINDER_ENABLED, enabled.toString()))
    }

    fun getReminderHour(): Flow<Int> =
        focusDao.getSettingFlow(KEY_REMINDER_HOUR).map { it?.value?.toIntOrNull() ?: 20 }

    fun getReminderMinute(): Flow<Int> =
        focusDao.getSettingFlow(KEY_REMINDER_MINUTE).map { it?.value?.toIntOrNull() ?: 0 }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        focusDao.insertSetting(SettingEntity(KEY_REMINDER_HOUR, hour.coerceIn(0, 23).toString()))
        focusDao.insertSetting(SettingEntity(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59).toString()))
    }

    // Accountability Partner
    fun getPartnerName(): Flow<String> =
        focusDao.getSettingFlow(KEY_PARTNER_NAME).map { it?.value ?: "" }

    suspend fun getPartnerNameSync(): String =
        focusDao.getSetting(KEY_PARTNER_NAME)?.value ?: ""

    suspend fun setPartnerName(name: String) {
        focusDao.insertSetting(SettingEntity(KEY_PARTNER_NAME, name))
    }

    fun getPartnerEmail(): Flow<String> =
        focusDao.getSettingFlow(KEY_PARTNER_EMAIL).map { it?.value ?: "" }

    suspend fun getPartnerEmailSync(): String =
        focusDao.getSetting(KEY_PARTNER_EMAIL)?.value ?: ""

    suspend fun setPartnerEmail(email: String) {
        focusDao.insertSetting(SettingEntity(KEY_PARTNER_EMAIL, email))
    }

    fun isNotifyPartnerOnQuit(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_NOTIFY_PARTNER_QUIT).map { it?.value == "true" }

    suspend fun isNotifyPartnerOnQuitSync(): Boolean =
        focusDao.getSetting(KEY_NOTIFY_PARTNER_QUIT)?.value == "true"

    suspend fun setNotifyPartnerOnQuit(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_NOTIFY_PARTNER_QUIT, enabled.toString()))
    }

    // Parental Control Mode
    fun isParentalLockEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_PARENTAL_LOCK).map { it?.value == "true" }

    suspend fun setParentalLockEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_PARENTAL_LOCK, enabled.toString()))
    }

    fun getParentalPinHash(): Flow<String> =
        focusDao.getSettingFlow(KEY_PARENTAL_PIN_HASH).map { it?.value ?: "" }

    suspend fun setParentalPinHash(pinHash: String) {
        focusDao.insertSetting(SettingEntity(KEY_PARENTAL_PIN_HASH, pinHash))
    }

    // Privacy, Cloud Sync & Telemetry
    fun isLeaderboardVisible(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_LEADERBOARD_VISIBLE).map { it?.value != "false" }

    suspend fun setLeaderboardVisible(visible: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_LEADERBOARD_VISIBLE, visible.toString()))
    }

    fun isCloudSyncEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_CLOUD_SYNC).map { it?.value != "false" }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_CLOUD_SYNC, enabled.toString()))
    }

    fun isAnalyticsOptIn(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_ANALYTICS_OPT_IN).map { it?.value != "false" }

    suspend fun setAnalyticsOptIn(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_ANALYTICS_OPT_IN, enabled.toString()))
    }

    fun isCrashReportingOptIn(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_CRASH_OPT_IN).map { it?.value != "false" }

    suspend fun setCrashReportingOptIn(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_CRASH_OPT_IN, enabled.toString()))
    }

    suspend fun restoreFromCloudBundle(bundle: CloudRestoreBundle) {
        bundle.settings?.let { s ->
            setDarkModeEnabled(s.isDarkMode)
            setAccentThemeId(s.accentTheme)
            setWhitelistModeEnabled(s.isWhitelistMode)
            setReadingAppPackage(s.designatedReadingApp)
            setAutoDndEnabled(s.autoDndEnabled)
            setAmbientSoundId(s.ambientSound)
            setReminderEnabled(s.reminderEnabled)
            setReminderTime(s.reminderHour, s.reminderMinute)
            setPartnerName(s.partnerName)
            setPartnerEmail(s.partnerEmail)
            setNotifyPartnerOnQuit(s.notifyPartnerOnQuit)
            setParentalLockEnabled(s.parentalLockEnabled)
            if (s.parentalPinHash.isNotBlank()) {
                setParentalPinHash(s.parentalPinHash)
            }
            setAnalyticsOptIn(s.analyticsOptIn)
            setCrashReportingOptIn(s.crashReportingOptIn)
            setCloudSyncEnabled(s.cloudSyncEnabled)
            setProTier(s.isPro)
        }

        if (bundle.blockedApps.isNotEmpty()) {
            for (app in bundle.blockedApps) {
                if (app.isBlocked) {
                    focusDao.insertBlockedApp(BlockedAppEntity(app.packageName, app.appName, true))
                }
            }
        }

        val existingLogs = getSessionLogsSync().map { it.timestamp }.toSet()
        for (s in bundle.sessions) {
            if (!existingLogs.contains(s.timestampMs)) {
                focusDao.insertSessionLog(
                    SessionLogEntity(
                        timestamp = s.timestampMs,
                        durationSeconds = s.durationSeconds,
                        blockedAttempts = s.blockedAttempts,
                        isCompleted = s.isCompleted,
                        readingAppPackage = s.readingAppPackage.ifBlank { null }
                    )
                )
            }
        }

        val existingTodos = getTodoItemsSync().map { it.title }.toSet()
        for (t in bundle.todos) {
            if (!existingTodos.contains(t.title)) {
                focusDao.insertTodoItem(
                    TodoEntity(
                        title = t.title,
                        isCompleted = t.isCompleted,
                        timestamp = t.timestampMs
                    )
                )
            }
        }
    }

    companion object {
        const val KEY_WHITELIST_MODE = "whitelist_mode"
        const val KEY_READING_APP_PKG = "reading_app_package"
        const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        const val KEY_SCHEDULE_START = "schedule_start"
        const val KEY_SCHEDULE_END = "schedule_end"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_PRO_TIER = "pro_tier_enabled"
        const val KEY_ACCENT_THEME = "accent_theme_id"
        const val KEY_AUTO_DND = "auto_dnd_enabled"
        const val KEY_AMBIENT_SOUND = "ambient_sound_id"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_PARTNER_NAME = "partner_name"
        const val KEY_PARTNER_EMAIL = "partner_email"
        const val KEY_NOTIFY_PARTNER_QUIT = "notify_partner_on_quit"
        const val KEY_PARENTAL_LOCK = "parental_lock_enabled"
        const val KEY_PARENTAL_PIN_HASH = "parental_pin_hash"
        const val KEY_LEADERBOARD_VISIBLE = "leaderboard_visible"
        const val KEY_CLOUD_SYNC = "cloud_sync_enabled"
        const val KEY_ANALYTICS_OPT_IN = "analytics_opt_in"
        const val KEY_CRASH_OPT_IN = "crash_reporting_opt_in"
    }
}
