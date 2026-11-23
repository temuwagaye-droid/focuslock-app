package com.example.data.repository

import com.example.data.local.FocusDao
import com.example.data.model.BlockedAppEntity
import com.example.data.model.SessionLogEntity
import com.example.data.model.SettingEntity
import com.example.data.model.TodoEntity
import com.example.data.remote.CloudRestoreBundle
import com.example.security.ParentalControlHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FocusRepository(private val focusDao: FocusDao) {

    companion object {
        const val KEY_WHITELIST_MODE = "whitelist_mode"
        const val KEY_READING_APP = "reading_app_package"
        const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        const val KEY_SCHEDULE_START_HOUR = "schedule_start_hour"
        const val KEY_SCHEDULE_END_HOUR = "schedule_end_hour"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_PRO_TIER = "pro_tier_enabled"
        const val KEY_ACCENT_THEME = "accent_theme"
        const val KEY_AUTO_DND = "auto_dnd_enabled"
        const val KEY_AMBIENT_SOUND = "ambient_sound"
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
        const val KEY_PARENTAL_PIN = "parental_pin"
        const val KEY_REMINDER_INTERVAL = "reminder_interval"
        const val KEY_WIDGET_ENABLED = "widget_enabled"
        const val KEY_PRIVATE_TRACKING = "private_tracking_enabled"
    }

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

    suspend fun getBlockedAppsSync(): List<BlockedAppEntity> = focusDao.getAllBlockedApps()

    suspend fun getSessionLogsSync(): List<SessionLogEntity> = allSessionLogs.first()

    suspend fun getTodoItemsSync(): List<TodoEntity> = allTodoItems.first()

    suspend fun insertBlockedApp(packageName: String, appName: String) {
        focusDao.insertBlockedApp(BlockedAppEntity(packageName = packageName, appName = appName, isBlocked = true))
    }

    suspend fun removeBlockedApp(packageName: String) {
        focusDao.deleteBlockedApp(packageName)
    }

    suspend fun clearBlockedApps() {
        focusDao.clearAllBlockedApps()
    }

    suspend fun clearSessionLogs() {
        focusDao.clearAllSessionLogs()
    }

    suspend fun clearTodoItems() {
        focusDao.clearAllTodoItems()
    }

    suspend fun saveSessionLog(
        durationSeconds: Long,
        blockedAttempts: Int,
        isCompleted: Boolean,
        readingAppPackage: String?
    ) {
        focusDao.insertSessionLog(
            SessionLogEntity(
                timestamp = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                blockedAttempts = blockedAttempts,
                isCompleted = isCompleted,
                readingAppPackage = readingAppPackage
            )
        )
    }

    fun isWhitelistModeEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_WHITELIST_MODE).map { it?.value?.toBooleanStrictOrNull() ?: false }

    suspend fun isWhitelistModeEnabledSync(): Boolean =
        focusDao.getSetting(KEY_WHITELIST_MODE)?.value?.toBooleanStrictOrNull() ?: false

    suspend fun setWhitelistModeEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_WHITELIST_MODE, enabled.toString()))
    }

    fun getReadingAppPackage(): Flow<String?> =
        focusDao.getSettingFlow(KEY_READING_APP).map { it?.value?.takeIf { v -> v.isNotBlank() } }

    suspend fun getReadingAppPackageSync(): String? =
        focusDao.getSetting(KEY_READING_APP)?.value?.takeIf { it.isNotBlank() }

    suspend fun setReadingAppPackage(packageName: String?) {
        focusDao.insertSetting(SettingEntity(KEY_READING_APP, packageName.orEmpty()))
    }

    fun isScheduleEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_SCHEDULE_ENABLED).map { it?.value?.toBooleanStrictOrNull() ?: false }

    suspend fun setScheduleEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_ENABLED, enabled.toString()))
    }

    fun getScheduleStartHour(): Flow<Int> =
        focusDao.getSettingFlow(KEY_SCHEDULE_START_HOUR).map { it?.value?.toIntOrNull() ?: 21 }

    suspend fun setScheduleStartHour(hour: Int) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_START_HOUR, hour.coerceIn(0, 23).toString()))
    }

    fun getScheduleEndHour(): Flow<Int> =
        focusDao.getSettingFlow(KEY_SCHEDULE_END_HOUR).map { it?.value?.toIntOrNull() ?: 23 }

    suspend fun setScheduleEndHour(hour: Int) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_END_HOUR, hour.coerceIn(0, 23).toString()))
    }

    fun isDarkModeEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_DARK_MODE).map { it?.value?.toBooleanStrictOrNull() ?: false }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_DARK_MODE, enabled.toString()))
    }

    fun isProTier(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_PRO_TIER).map { it?.value?.toBooleanStrictOrNull() ?: false }

    suspend fun setProTier(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_PRO_TIER, enabled.toString()))
    }

    fun getAccentThemeId(): Flow<String> =
        focusDao.getSettingFlow(KEY_ACCENT_THEME).map { it?.value ?: "violet" }

    suspend fun setAccentThemeId(themeId: String) {
        focusDao.insertSetting(SettingEntity(KEY_ACCENT_THEME, themeId))
    }

    fun isAutoDndEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_AUTO_DND).map { it?.value?.toBooleanStrictOrNull() ?: false }

    suspend fun isAutoDndEnabledSync(): Boolean =
        focusDao.getSetting(KEY_AUTO_DND)?.value?.toBooleanStrictOrNull() ?: false

    suspend fun setAutoDndEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_AUTO_DND, enabled.toString()))
    }

    fun getAmbientSoundId(): Flow<String> =
        focusDao.getSettingFlow(KEY_AMBIENT_SOUND).map { it?.value ?: "none" }

    suspend fun getAmbientSoundIdSync(): String =
        focusDao.getSetting(KEY_AMBIENT_SOUND)?.value ?: "none"

    suspend fun setAmbientSoundId(soundId: String) {
        focusDao.insertSetting(SettingEntity(KEY_AMBIENT_SOUND, soundId))
    }

    fun isReminderEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_REMINDER_ENABLED).map { it?.value?.toBooleanStrictOrNull() ?: false }

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

    fun getPartnerName(): Flow<String> =
        focusDao.getSettingFlow(KEY_PARTNER_NAME).map { it?.value.orEmpty() }

    suspend fun getPartnerNameSync(): String =
        focusDao.getSetting(KEY_PARTNER_NAME)?.value.orEmpty()

    suspend fun setPartnerName(name: String) {
        focusDao.insertSetting(SettingEntity(KEY_PARTNER_NAME, name))
    }

    fun getPartnerEmail(): Flow<String> =
        focusDao.getSettingFlow(KEY_PARTNER_EMAIL).map { it?.value.orEmpty() }

    suspend fun getPartnerEmailSync(): String =
        focusDao.getSetting(KEY_PARTNER_EMAIL)?.value.orEmpty()

    suspend fun setPartnerEmail(email: String) {
        focusDao.insertSetting(SettingEntity(KEY_PARTNER_EMAIL, email))
    }

    fun isNotifyPartnerOnQuit(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_NOTIFY_PARTNER_QUIT).map { it?.value?.toBooleanStrictOrNull() ?: false }

    suspend fun isNotifyPartnerOnQuitSync(): Boolean =
        focusDao.getSetting(KEY_NOTIFY_PARTNER_QUIT)?.value?.toBooleanStrictOrNull() ?: false

    suspend fun setNotifyPartnerOnQuit(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_NOTIFY_PARTNER_QUIT, enabled.toString()))
    }

    fun isParentalLockEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_PARENTAL_LOCK).map { it?.value?.toBooleanStrictOrNull() ?: false }

    suspend fun setParentalLockEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_PARENTAL_LOCK, enabled.toString()))
    }

    fun getParentalPinHash(): Flow<String> =
        focusDao.getSettingFlow(KEY_PARENTAL_PIN_HASH).map { it?.value.orEmpty() }

    suspend fun setParentalPinHash(pinHash: String) {
        focusDao.insertSetting(SettingEntity(KEY_PARENTAL_PIN_HASH, pinHash))
    }

    fun isLeaderboardVisible(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_LEADERBOARD_VISIBLE).map { it?.value?.toBooleanStrictOrNull() ?: true }

    suspend fun setLeaderboardVisible(visible: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_LEADERBOARD_VISIBLE, visible.toString()))
    }

    fun isCloudSyncEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_CLOUD_SYNC).map { it?.value?.toBooleanStrictOrNull() ?: true }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_CLOUD_SYNC, enabled.toString()))
    }

    fun isAnalyticsOptIn(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_ANALYTICS_OPT_IN).map { it?.value?.toBooleanStrictOrNull() ?: true }

    suspend fun setAnalyticsOptIn(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_ANALYTICS_OPT_IN, enabled.toString()))
    }

    fun isCrashReportingOptIn(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_CRASH_OPT_IN).map { it?.value?.toBooleanStrictOrNull() ?: true }

    suspend fun setCrashReportingOptIn(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_CRASH_OPT_IN, enabled.toString()))
    }

    fun isProTierEnabled(): Flow<Boolean> = isProTier()
    suspend fun setProTierEnabled(enabled: Boolean) = setProTier(enabled)

    fun isDndEnabled(): Flow<Boolean> = isAutoDndEnabled()
    suspend fun setDndEnabled(enabled: Boolean) = setAutoDndEnabled(enabled)

    fun isPartnerNotificationsEnabled(): Flow<Boolean> = isNotifyPartnerOnQuit()
    suspend fun setPartnerNotificationsEnabled(enabled: Boolean) = setNotifyPartnerOnQuit(enabled)

    fun getParentalPin(): Flow<String> =
        focusDao.getSettingFlow(KEY_PARENTAL_PIN).map { it?.value ?: "" }

    suspend fun setParentalPin(pin: String) {
        focusDao.insertSetting(SettingEntity(KEY_PARENTAL_PIN, pin))
        if (pin.isNotBlank()) {
            setParentalPinHash(ParentalControlHelper.hashPin(pin))
        }
    }

    fun getReminderInterval(): Flow<String> =
        focusDao.getSettingFlow(KEY_REMINDER_INTERVAL).map { it?.value ?: "15" }

    suspend fun setReminderInterval(interval: String) {
        focusDao.insertSetting(SettingEntity(KEY_REMINDER_INTERVAL, interval))
    }

    fun isWidgetEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_WIDGET_ENABLED).map { it?.value?.toBooleanStrictOrNull() ?: false }

    suspend fun setWidgetEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_WIDGET_ENABLED, enabled.toString()))
    }

    fun isPrivateTrackingEnabled(): Flow<Boolean> =
        focusDao.getSettingFlow(KEY_PRIVATE_TRACKING).map { it?.value?.toBooleanStrictOrNull() ?: true }

    suspend fun setPrivateTrackingEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_PRIVATE_TRACKING, enabled.toString()))
    }

    suspend fun restoreFromCloudBundle(bundle: CloudRestoreBundle) {
        bundle.settings?.let { s ->
            setDarkModeEnabled(s.isDarkMode)
            setAccentThemeId(s.accentTheme)
            setWhitelistModeEnabled(s.isWhitelistMode)
            setReadingAppPackage(s.designatedReadingApp.ifBlank { null })
            setAutoDndEnabled(s.autoDndEnabled)
            setAmbientSoundId(s.ambientSound)
            setReminderEnabled(s.reminderEnabled)
            setReminderTime(s.reminderHour, s.reminderMinute)
            setPartnerName(s.partnerName)
            setPartnerEmail(s.partnerEmail)
            setNotifyPartnerOnQuit(s.notifyPartnerOnQuit)
            setParentalLockEnabled(s.parentalLockEnabled)
            setParentalPinHash(s.parentalPinHash)
            setAnalyticsOptIn(s.analyticsOptIn)
            setCrashReportingOptIn(s.crashReportingOptIn)
            setCloudSyncEnabled(s.cloudSyncEnabled)
            setProTier(s.isPro)
        }

        if (bundle.blockedApps.isNotEmpty()) {
            focusDao.clearAllBlockedApps()
            bundle.blockedApps.forEach { app ->
                if (app.packageName.isNotBlank()) {
                    focusDao.insertBlockedApp(
                        BlockedAppEntity(
                            packageName = app.packageName,
                            appName = app.appName.ifBlank { app.packageName },
                            isBlocked = app.isBlocked
                        )
                    )
                }
            }
        }

        if (bundle.sessions.isNotEmpty()) {
            focusDao.clearAllSessionLogs()
            bundle.sessions.forEach { session ->
                focusDao.insertSessionLog(
                    SessionLogEntity(
                        timestamp = session.timestampMs,
                        durationSeconds = session.durationSeconds,
                        blockedAttempts = session.blockedAttempts,
                        isCompleted = session.isCompleted,
                        readingAppPackage = session.readingAppPackage.ifBlank { null }
                    )
                )
            }
        }

        if (bundle.todos.isNotEmpty()) {
            focusDao.clearAllTodoItems()
            bundle.todos.forEach { todo ->
                focusDao.insertTodoItem(
                    TodoEntity(
                        title = todo.title,
                        isCompleted = todo.isCompleted,
                        timestamp = todo.timestampMs
                    )
                )
            }
        }
    }
}
