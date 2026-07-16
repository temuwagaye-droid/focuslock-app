package com.example.data.repository

import com.example.data.local.FocusDao
import com.example.data.model.BlockedAppEntity
import com.example.data.model.SessionLogEntity
import com.example.data.model.SettingEntity
import com.example.data.model.TodoEntity
import kotlinx.coroutines.flow.Flow
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

    suspend fun insertBlockedApp(packageName: String, appName: String) {
        focusDao.insertBlockedApp(BlockedAppEntity(packageName, appName))
    }

    suspend fun removeBlockedApp(packageName: String) {
        focusDao.deleteBlockedApp(packageName)
    }

    suspend fun clearBlockedApps() {
        focusDao.clearAllBlockedApps()
    }

    suspend fun saveSessionLog(durationSeconds: Long, blockedAttempts: Int, isCompleted: Boolean, readingApp: String?) {
        val log = SessionLogEntity(
            durationSeconds = durationSeconds,
            blockedAttempts = blockedAttempts,
            isCompleted = isCompleted,
            readingAppPackage = readingApp
        )
        focusDao.insertSessionLog(log)
    }

    // Setting helpers
    fun isWhitelistModeEnabled(): Flow<Boolean> {
        return focusDao.getSettingFlow(KEY_WHITELIST_MODE).map { it?.value == "true" }
    }

    suspend fun isWhitelistModeEnabledSync(): Boolean {
        return focusDao.getSetting(KEY_WHITELIST_MODE)?.value == "true"
    }

    suspend fun setWhitelistModeEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_WHITELIST_MODE, enabled.toString()))
    }

    fun getReadingAppPackage(): Flow<String?> {
        return focusDao.getSettingFlow(KEY_READING_APP_PKG).map { it?.value }
    }

    suspend fun getReadingAppPackageSync(): String? {
        return focusDao.getSetting(KEY_READING_APP_PKG)?.value
    }

    suspend fun setReadingAppPackage(packageName: String?) {
        focusDao.insertSetting(SettingEntity(KEY_READING_APP_PKG, packageName ?: ""))
    }

    fun isScheduleEnabled(): Flow<Boolean> {
        return focusDao.getSettingFlow(KEY_SCHEDULE_ENABLED).map { it?.value == "true" }
    }

    suspend fun setScheduleEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_ENABLED, enabled.toString()))
    }

    fun getScheduleStartHour(): Flow<Int> {
        return focusDao.getSettingFlow(KEY_SCHEDULE_START).map { it?.value?.toIntOrNull() ?: 20 }
    }

    suspend fun setScheduleStartHour(hour: Int) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_START, hour.toString()))
    }

    fun getScheduleEndHour(): Flow<Int> {
        return focusDao.getSettingFlow(KEY_SCHEDULE_END).map { it?.value?.toIntOrNull() ?: 21 }
    }

    suspend fun setScheduleEndHour(hour: Int) {
        focusDao.insertSetting(SettingEntity(KEY_SCHEDULE_END, hour.toString()))
    }

    fun isDarkModeEnabled(): Flow<Boolean> {
        return focusDao.getSettingFlow(KEY_DARK_MODE).map { it?.value == "true" }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        focusDao.insertSetting(SettingEntity(KEY_DARK_MODE, enabled.toString()))
    }

    companion object {
        const val KEY_WHITELIST_MODE = "whitelist_mode"
        const val KEY_READING_APP_PKG = "reading_app_package"
        const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        const val KEY_SCHEDULE_START = "schedule_start"
        const val KEY_SCHEDULE_END = "schedule_end"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
