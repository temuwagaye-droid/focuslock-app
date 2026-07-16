package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BlockedAppEntity
import com.example.data.model.SessionLogEntity
import com.example.data.repository.FocusRepository
import com.example.service.FocusSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

data class AppInfoModel(
    val packageName: String,
    val label: String
)

data class FocusStatsSummary(
    val totalFocusMinutes: Long,
    val totalBlockedAttempts: Int,
    val completedSessionsCount: Int,
    val currentStreak: Int,
    val focusScore: Int
)

class FocusViewModel(private val repository: FocusRepository) : ViewModel() {

    // Installed launcher apps list
    private val _installedApps = MutableStateFlow<List<AppInfoModel>>(emptyList())
    val installedApps: StateFlow<List<AppInfoModel>> = _installedApps.asStateFlow()

    // Database elements
    val blockedAppsList: StateFlow<List<BlockedAppEntity>> = repository.allBlockedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isWhitelistMode: StateFlow<Boolean> = repository.isWhitelistModeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val designatedReadingApp: StateFlow<String?> = repository.getReadingAppPackage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isScheduleEnabled: StateFlow<Boolean> = repository.isScheduleEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val scheduleStartHour: StateFlow<Int> = repository.getScheduleStartHour()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val scheduleEndHour: StateFlow<Int> = repository.getScheduleEndHour()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21)

    // Gamified stats calculated from raw session history
    val statsSummary: StateFlow<FocusStatsSummary> = repository.allSessionLogs
        .mapLogsToSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusStatsSummary(0, 0, 0, 0, 0))

    val sessionLogs: StateFlow<List<SessionLogEntity>> = repository.allSessionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active session bindings
    val isSessionActive = FocusSessionManager.isSessionActive
    val isBreakActive = FocusSessionManager.isBreakActive
    val timeLeftSeconds = FocusSessionManager.timeLeftSeconds
    val blockedAttemptsCount = FocusSessionManager.blockedAttemptsCount

    fun loadInstalledLauncherApps(context: Context) {
        viewModelScope.launch {
            // If apps are already loaded, don't query again to keep UI loading instant
            if (_installedApps.value.isNotEmpty()) return@launch

            val appList = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val list = mutableListOf<AppInfoModel>()
                for (info in resolveInfos) {
                    val packageName = info.activityInfo.packageName
                    if (packageName == context.packageName) continue // Hide self
                    val label = info.loadLabel(pm).toString()
                    list.add(AppInfoModel(packageName, label))
                }
                list.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
            }
            _installedApps.value = appList
        }
    }

    fun toggleAppBlock(packageName: String, appName: String, isBlocked: Boolean) {
        viewModelScope.launch {
            if (isBlocked) {
                repository.insertBlockedApp(packageName, appName)
            } else {
                repository.removeBlockedApp(packageName)
            }
        }
    }

    fun toggleWhitelistMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setWhitelistModeEnabled(enabled)
        }
    }

    fun selectReadingApp(packageName: String?) {
        viewModelScope.launch {
            repository.setReadingAppPackage(packageName)
        }
    }

    fun toggleSchedule(enabled: Boolean) {
        viewModelScope.launch {
            repository.setScheduleEnabled(enabled)
        }
    }

    fun saveScheduleHours(start: Int, end: Int) {
        viewModelScope.launch {
            repository.setScheduleStartHour(start)
            repository.setScheduleEndHour(end)
        }
    }

    fun startFocusSession(context: Context, durationMinutes: Int) {
        viewModelScope.launch {
            val isWhitelist = isWhitelistMode.value
            val readingApp = designatedReadingApp.value
            val blockedPkgs = repository.getBlockedAppsSync().filter { it.isBlocked }.map { it.packageName }
            FocusSessionManager.startSession(
                context = context,
                durationMinutes = durationMinutes,
                readingAppPackage = readingApp,
                isWhitelist = isWhitelist,
                blockedPkgs = blockedPkgs
            )
        }
    }

    fun stopFocusSession(context: Context, completed: Boolean) {
        FocusSessionManager.stopSession(context, completed)
    }

    fun stopBreakSession(context: Context) {
        FocusSessionManager.stopBreak(context)
    }

    // Helper to calculate statistics and streaks
    private fun kotlinx.coroutines.flow.Flow<List<SessionLogEntity>>.mapLogsToSummary(): kotlinx.coroutines.flow.Flow<FocusStatsSummary> {
        return combine(isWhitelistMode) { logs, _ ->
            if (logs.isEmpty()) {
                return@combine FocusStatsSummary(0, 0, 0, 0, 0)
            }

            var totalSec = 0L
            var totalBlocked = 0
            var completedCount = 0

            for (log in logs) {
                totalSec += log.durationSeconds
                totalBlocked += log.blockedAttempts
                if (log.isCompleted) {
                    completedCount++
                }
            }

            val totalFocusMin = totalSec / 60
            val streak = calculateStreak(logs)
            
            // Gamification score formula
            val focusScore = ((completedCount * 50) + (totalFocusMin * 2).toInt() - (totalBlocked * 5)).coerceAtLeast(0)

            FocusStatsSummary(
                totalFocusMinutes = totalFocusMin,
                totalBlockedAttempts = totalBlocked,
                completedSessionsCount = completedCount,
                currentStreak = streak,
                focusScore = focusScore
            )
        }
    }

    private fun calculateStreak(logs: List<SessionLogEntity>): Int {
        val completedLogs = logs.filter { it.isCompleted && it.durationSeconds > 60 }
        if (completedLogs.isEmpty()) return 0

        // Map to unique local days since epoch
        val localTimeZone = TimeZone.getDefault()
        val uniqueDays = completedLogs.map { log ->
            val cal = Calendar.getInstance(localTimeZone)
            cal.timeInMillis = log.timestamp
            // Normalize to midnight
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis / (1000 * 60 * 60 * 24)
        }.distinct().sortedDescending() // Newest to oldest

        if (uniqueDays.isEmpty()) return 0

        val todayCal = Calendar.getInstance(localTimeZone)
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        val todayEpochDay = todayCal.timeInMillis / (1000 * 60 * 60 * 24)

        val latestCompletedDay = uniqueDays.first()

        // Streak is only active if user completed a session today or yesterday
        if (todayEpochDay - latestCompletedDay > 1) {
            return 0
        }

        var streak = 1
        for (i in 0 until uniqueDays.size - 1) {
            if (uniqueDays[i] - uniqueDays[i + 1] == 1L) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}

class FocusViewModelFactory(private val repository: FocusRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FocusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FocusViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
