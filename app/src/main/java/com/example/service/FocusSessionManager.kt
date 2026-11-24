package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import com.example.audio.AmbientSoundPlayer
import com.example.audio.AmbientSoundType
import com.example.data.local.AppDatabase
import com.example.data.repository.FocusRepository
import com.example.notifications.NotificationHelper
import com.example.telemetry.TelemetryManager
import com.example.widget.FocusQuickWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EarlyQuitAlert(
    val elapsedMinutes: Int,
    val blockedAttempts: Int,
    val partnerName: String,
    val partnerEmail: String
)

object FocusSessionManager {
    private const val PREFS_NAME = "focuslock_engine_state"
    private const val KEY_SESSION_ACTIVE = "engine_session_active"
    private const val KEY_BREAK_ACTIVE = "engine_break_active"
    private const val KEY_IS_PAUSED = "engine_is_paused"
    private const val KEY_START_TIME_MS = "engine_start_time_ms"
    private const val KEY_END_TIME_MS = "engine_end_time_ms"
    private const val KEY_TOTAL_SECS = "engine_total_secs"
    private const val KEY_PAUSED_LEFT_SECS = "engine_paused_left_secs"
    private const val KEY_MODE = "engine_mode"
    private const val KEY_POMODORO_CYCLES = "engine_pomodoro_cycles"
    private const val KEY_BLOCKED_ATTEMPTS = "engine_blocked_attempts"

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isBreakActive = MutableStateFlow(false)
    val isBreakActive: StateFlow<Boolean> = _isBreakActive.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(0L)
    val timeLeftSeconds: StateFlow<Long> = _timeLeftSeconds.asStateFlow()

    private val _blockedAttemptsCount = MutableStateFlow(0)
    val blockedAttemptsCount: StateFlow<Int> = _blockedAttemptsCount.asStateFlow()

    private val _pomodoroCycleCount = MutableStateFlow(1)
    val pomodoroCycleCount: StateFlow<Int> = _pomodoroCycleCount.asStateFlow()

    private val _isDndActivatedBySession = MutableStateFlow(false)
    val isDndActivatedBySession: StateFlow<Boolean> = _isDndActivatedBySession.asStateFlow()

    private val _pendingQuitAlert = MutableStateFlow<EarlyQuitAlert?>(null)
    val pendingQuitAlert: StateFlow<EarlyQuitAlert?> = _pendingQuitAlert.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var previousInterruptionFilter: Int? = NotificationManager.INTERRUPTION_FILTER_ALL

    var totalDurationSeconds: Long = 0L
        private set

    var sessionStartTimeMillis: Long = 0L
        private set

    var sessionEndTimeMillis: Long = 0L
        private set

    var isWhitelistMode: Boolean = false
        private set

    var blockedPackages: List<String> = emptyList()
        private set

    var designatedReadingApp: String? = null
        private set

    private var currentSound: AmbientSoundType = AmbientSoundType.NONE

    enum class FocusMode(
        val displayName: String,
        val defaultMinutes: Int,
        val breakMinutes: Int,
        val intensity: String
    ) {
        DEEP_WORK("Deep Work", 90, 15, "High Intensity"),
        STUDY("Study", 50, 10, "Medium Intensity"),
        POMODORO("Pomodoro", 25, 5, "Focus Cycles"),
        READING("Reading", 30, 5, "Quiet Focus"),
        QUICK_FOCUS("Quick Focus", 15, 3, "Light Focus"),
        STRICT_FOCUS("Strict Focus", 60, 10, "Strict Shield"),
        RELAX_RECHARGE("Relax & Recharge", 10, 0, "Recovery & Breath"),
        CUSTOM("Custom Focus", 25, 5, "Personalized")
    }

    private var currentFocusMode: FocusMode = FocusMode.DEEP_WORK

    fun getCurrentFocusMode(): FocusMode = currentFocusMode

    fun setFocusMode(mode: FocusMode) {
        currentFocusMode = mode
    }

    fun canPauseCurrentMode(): Boolean {
        return currentFocusMode != FocusMode.STRICT_FOCUS
    }

    fun isStrictExitRequired(): Boolean {
        return currentFocusMode == FocusMode.STRICT_FOCUS || currentFocusMode == FocusMode.DEEP_WORK
    }

    fun isPenaltyFreeExit(): Boolean {
        return currentFocusMode == FocusMode.QUICK_FOCUS
    }

    fun recoverSessionIfActive(context: Context) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionActive = prefs.getBoolean(KEY_SESSION_ACTIVE, false)
        val breakActive = prefs.getBoolean(KEY_BREAK_ACTIVE, false)
        if (!sessionActive && !breakActive) return

        val paused = prefs.getBoolean(KEY_IS_PAUSED, false)
        val modeStr = prefs.getString(KEY_MODE, FocusMode.DEEP_WORK.name) ?: FocusMode.DEEP_WORK.name
        val mode = runCatching { FocusMode.valueOf(modeStr) }.getOrDefault(FocusMode.DEEP_WORK)
        currentFocusMode = mode
        totalDurationSeconds = prefs.getLong(KEY_TOTAL_SECS, 1500L)
        _pomodoroCycleCount.value = prefs.getInt(KEY_POMODORO_CYCLES, 1)
        _blockedAttemptsCount.value = prefs.getInt(KEY_BLOCKED_ATTEMPTS, 0)
        _isPaused.value = paused

        if (paused) {
            val pausedRemaining = prefs.getLong(KEY_PAUSED_LEFT_SECS, 0L)
            _timeLeftSeconds.value = pausedRemaining
            _isSessionActive.value = sessionActive
            _isBreakActive.value = breakActive
        } else {
            val endTime = prefs.getLong(KEY_END_TIME_MS, 0L)
            val now = System.currentTimeMillis()
            val remainingSecs = (endTime - now) / 1000L
            if (remainingSecs > 0) {
                _timeLeftSeconds.value = remainingSecs
                sessionEndTimeMillis = endTime
                _isSessionActive.value = sessionActive
                _isBreakActive.value = breakActive
                val serviceIntent = Intent(appCtx, FocusService::class.java).apply {
                    action = if (sessionActive) FocusService.ACTION_START_SESSION else FocusService.ACTION_START_BREAK
                    putExtra(FocusService.EXTRA_DURATION_MINUTES, (totalDurationSeconds / 60L).toInt())
                    putExtra(FocusService.EXTRA_FOCUS_MODE, mode.name)
                }
                runCatching { ContextCompat.startForegroundService(appCtx, serviceIntent) }
            } else {
                clearPersistedSession(appCtx)
                if (sessionActive) {
                    stopSession(appCtx, completed = true)
                } else if (breakActive) {
                    stopBreak(appCtx)
                }
            }
        }
    }

    fun startSession(
        context: Context,
        durationMinutes: Int,
        readingAppPackage: String?,
        isWhitelist: Boolean,
        blockedPkgs: List<String>,
        enableAutoDnd: Boolean = false,
        ambientSound: AmbientSoundType = AmbientSoundType.NONE,
        focusMode: FocusMode = FocusMode.DEEP_WORK
    ) {
        val appCtx = context.applicationContext
        val clampedMinutes = durationMinutes.coerceIn(1, 180)
        totalDurationSeconds = clampedMinutes * 60L
        _timeLeftSeconds.value = totalDurationSeconds
        _blockedAttemptsCount.value = 0
        _isBreakActive.value = false
        _isPaused.value = false
        _isSessionActive.value = true

        isWhitelistMode = isWhitelist
        blockedPackages = blockedPkgs
        designatedReadingApp = readingAppPackage
        currentFocusMode = focusMode
        currentSound = ambientSound

        val now = System.currentTimeMillis()
        sessionStartTimeMillis = now
        sessionEndTimeMillis = now + (totalDurationSeconds * 1000L)

        when (focusMode) {
            FocusMode.STRICT_FOCUS -> {
                if (enableAutoDnd) activateDoNotDisturbIfPermitted(appCtx)
            }
            FocusMode.RELAX_RECHARGE -> {
                if (ambientSound == AmbientSoundType.NONE) {
                    AmbientSoundPlayer.play(AmbientSoundType.WHITE_NOISE)
                } else {
                    AmbientSoundPlayer.play(ambientSound)
                }
            }
            FocusMode.DEEP_WORK -> {
                if (enableAutoDnd) activateDoNotDisturbIfPermitted(appCtx)
                if (ambientSound != AmbientSoundType.NONE) AmbientSoundPlayer.play(ambientSound)
                else AmbientSoundPlayer.stop()
            }
            FocusMode.STUDY, FocusMode.POMODORO, FocusMode.QUICK_FOCUS -> {
                if (enableAutoDnd) activateDoNotDisturbIfPermitted(appCtx)
                if (ambientSound != AmbientSoundType.NONE) AmbientSoundPlayer.play(ambientSound)
            }
            FocusMode.READING -> {
                if (ambientSound == AmbientSoundType.NONE) {
                    AmbientSoundPlayer.play(AmbientSoundType.WHITE_NOISE)
                } else {
                    AmbientSoundPlayer.play(ambientSound)
                }
            }
            FocusMode.CUSTOM -> {
                if (enableAutoDnd) activateDoNotDisturbIfPermitted(appCtx)
                if (ambientSound != AmbientSoundType.NONE) AmbientSoundPlayer.play(ambientSound)
            }
        }

        savePersistedState(appCtx, isSession = true, isBreak = false, isPaused = false)
        NotificationHelper.showFocusStartedNotification(appCtx, focusMode.displayName, clampedMinutes)
        TelemetryManager.trackEvent(appCtx, "focus_session_started", "${clampedMinutes}m ${focusMode.name}")

        val serviceIntent = Intent(appCtx, FocusService::class.java).apply {
            action = FocusService.ACTION_START_SESSION
            putExtra(FocusService.EXTRA_DURATION_MINUTES, clampedMinutes)
            putExtra(FocusService.EXTRA_FOCUS_MODE, focusMode.name)
        }
        runCatching {
            ContextCompat.startForegroundService(appCtx, serviceIntent)
        }
    }

    fun pauseSession(context: Context) {
        if (!canPauseCurrentMode()) return
        if (!_isSessionActive.value && !_isBreakActive.value) return
        _isPaused.value = true
        AmbientSoundPlayer.pause()
        savePersistedState(context.applicationContext, isSession = _isSessionActive.value, isBreak = _isBreakActive.value, isPaused = true)
    }

    fun resumeSession(context: Context) {
        if (!_isPaused.value) return
        _isPaused.value = false
        val now = System.currentTimeMillis()
        sessionEndTimeMillis = now + (_timeLeftSeconds.value * 1000L)
        AmbientSoundPlayer.resume()
        savePersistedState(context.applicationContext, isSession = _isSessionActive.value, isBreak = _isBreakActive.value, isPaused = false)
    }

    fun stopSession(context: Context, completed: Boolean) {
        val appCtx = context.applicationContext
        if (!_isSessionActive.value) return

        val remaining = _timeLeftSeconds.value
        val elapsedSeconds = (totalDurationSeconds - remaining).coerceAtLeast(0L)
        val loggedDuration = if (completed) totalDurationSeconds else elapsedSeconds
        val attempts = _blockedAttemptsCount.value
        val readingApp = designatedReadingApp
        val mode = currentFocusMode

        _isSessionActive.value = false
        _isPaused.value = false
        _timeLeftSeconds.value = 0L

        clearPersistedSession(appCtx)
        AmbientSoundPlayer.stop()
        restoreDoNotDisturbIfNeeded(appCtx)

        val stopIntent = Intent(appCtx, FocusService::class.java).apply {
            action = FocusService.ACTION_STOP_SESSION
        }
        runCatching { appCtx.stopService(stopIntent) }

        managerScope.launch {
            val repo = FocusRepository(AppDatabase.getDatabase(appCtx).focusDao())
            if (loggedDuration > 0L) {
                repo.saveSessionLog(
                    durationSeconds = loggedDuration,
                    blockedAttempts = attempts,
                    isCompleted = completed,
                    readingAppPackage = readingApp
                )
            }
            if (completed) {
                val mins = (totalDurationSeconds / 60L).toInt().coerceAtLeast(1)
                NotificationHelper.showFocusCompletedNotification(appCtx, mode.displayName)
                NotificationHelper.showSessionCompletedNotification(appCtx, mins, attempts)
                TelemetryManager.trackEvent(appCtx, "focus_session_completed", "${mins}m,mode=${mode.name},blocked=$attempts")

                // Handle automatic break transitions
                when (mode) {
                    FocusMode.POMODORO -> {
                        val cycles = _pomodoroCycleCount.value
                        if (cycles % 4 == 0) {
                            startBreak(appCtx, breakMinutes = 20)
                            _pomodoroCycleCount.value = cycles + 1
                        } else {
                            startBreak(appCtx, breakMinutes = 5)
                            _pomodoroCycleCount.value = cycles + 1
                        }
                    }
                    FocusMode.DEEP_WORK -> startBreak(appCtx, breakMinutes = 15)
                    FocusMode.STUDY -> startBreak(appCtx, breakMinutes = 10)
                    FocusMode.READING -> startBreak(appCtx, breakMinutes = 5)
                    FocusMode.STRICT_FOCUS -> startBreak(appCtx, breakMinutes = 10)
                    FocusMode.QUICK_FOCUS -> { /* optional break */ }
                    FocusMode.RELAX_RECHARGE -> { /* recovery session completed */ }
                    FocusMode.CUSTOM -> { /* custom duration */ }
                }
            } else {
                val elapsedMins = (elapsedSeconds / 60L).toInt()
                TelemetryManager.trackEvent(appCtx, "focus_session_aborted", "${elapsedMins}m,mode=${mode.name},blocked=$attempts")
                if (!isPenaltyFreeExit() && repo.isNotifyPartnerOnQuitSync()) {
                    val partnerName = repo.getPartnerNameSync()
                    val partnerEmail = repo.getPartnerEmailSync()
                    _pendingQuitAlert.value = EarlyQuitAlert(
                        elapsedMinutes = elapsedMins,
                        blockedAttempts = attempts,
                        partnerName = partnerName,
                        partnerEmail = partnerEmail
                    )
                    NotificationHelper.showAccountabilityQuitNotification(
                        appCtx,
                        partnerName,
                        elapsedMins,
                        attempts
                    )
                }
            }
            FocusQuickWidgetProvider.refreshAllWidgets(appCtx)
        }
    }

    fun clearPendingQuitAlert() {
        _pendingQuitAlert.value = null
    }

    fun startBreak(context: Context, breakMinutes: Int = 5) {
        val appCtx = context.applicationContext
        val mins = breakMinutes.coerceIn(1, 30)
        totalDurationSeconds = mins * 60L
        _timeLeftSeconds.value = totalDurationSeconds
        _isSessionActive.value = false
        _isBreakActive.value = true
        _isPaused.value = false

        val now = System.currentTimeMillis()
        sessionStartTimeMillis = now
        sessionEndTimeMillis = now + (totalDurationSeconds * 1000L)

        AmbientSoundPlayer.stop()
        restoreDoNotDisturbIfNeeded(appCtx)
        savePersistedState(appCtx, isSession = false, isBreak = true, isPaused = false)
        NotificationHelper.showBreakStartedNotification(appCtx, mins)

        val serviceIntent = Intent(appCtx, FocusService::class.java).apply {
            action = FocusService.ACTION_START_BREAK
            putExtra(FocusService.EXTRA_DURATION_MINUTES, mins)
        }
        runCatching {
            ContextCompat.startForegroundService(appCtx, serviceIntent)
        }
    }

    fun stopBreak(context: Context) {
        val appCtx = context.applicationContext
        _isBreakActive.value = false
        _isPaused.value = false
        _timeLeftSeconds.value = 0L
        clearPersistedSession(appCtx)
        NotificationHelper.showBreakCompletedNotification(appCtx)
        runCatching {
            appCtx.stopService(Intent(appCtx, FocusService::class.java))
        }
    }

    fun tick() {
        if (_isPaused.value) return
        val current = _timeLeftSeconds.value
        if (current > 0L) {
            _timeLeftSeconds.value = current - 1L
        }
    }

    fun incrementBlockedAttempts(context: Context, blockedPackage: String = "") {
        _blockedAttemptsCount.value += 1
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BLOCKED_ATTEMPTS, _blockedAttemptsCount.value).apply()
        TelemetryManager.trackEvent(
            appCtx,
            "distraction_blocked",
            blockedPackage.ifBlank { "unknown" }
        )
    }

    private fun savePersistedState(context: Context, isSession: Boolean, isBreak: Boolean, isPaused: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, isSession)
            .putBoolean(KEY_BREAK_ACTIVE, isBreak)
            .putBoolean(KEY_IS_PAUSED, isPaused)
            .putLong(KEY_START_TIME_MS, sessionStartTimeMillis)
            .putLong(KEY_END_TIME_MS, sessionEndTimeMillis)
            .putLong(KEY_TOTAL_SECS, totalDurationSeconds)
            .putLong(KEY_PAUSED_LEFT_SECS, _timeLeftSeconds.value)
            .putString(KEY_MODE, currentFocusMode.name)
            .putInt(KEY_POMODORO_CYCLES, _pomodoroCycleCount.value)
            .putInt(KEY_BLOCKED_ATTEMPTS, _blockedAttemptsCount.value)
            .apply()
    }

    private fun clearPersistedSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .putBoolean(KEY_BREAK_ACTIVE, false)
            .putBoolean(KEY_IS_PAUSED, false)
            .putLong(KEY_PAUSED_LEFT_SECS, 0L)
            .apply()
    }

    private fun activateDoNotDisturbIfPermitted(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        runCatching {
            previousInterruptionFilter = nm.currentInterruptionFilter?.let { it as Int }
                ?: NotificationManager.INTERRUPTION_FILTER_PRIORITY
            nm.setInterruptionFilter(previousInterruptionFilter!!)
            _isDndActivatedBySession.value = true
        }
    }

    private fun restoreDoNotDisturbIfNeeded(context: Context) {
        if (!_isDndActivatedBySession.value) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        runCatching {
            val prev = previousInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
            nm.setInterruptionFilter(prev)
        }
        previousInterruptionFilter = null
        _isDndActivatedBySession.value = false
    }
}
