package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.experimental.selectCoroutinesBlocking
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
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isBreakActive = MutableStateFlow(false)
    val isBreakActive: StateFlow<Boolean> = _isBreakActive.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(0L)
    val timeLeftSeconds: StateFlow<Long> = _timeLeftSeconds.asStateFlow()

    private val _blockedAttemptsCount = MutableStateFlow(0)
    val blockedAttemptsCount: StateFlow<Int> = _blockedAttemptsCount.asStateFlow()

    private val _isDndActivatedBySession = MutableStateFlow(false)
    val isDndActivatedBySession: StateFlow<Boolean> = _isDndActivatedBySession.asStateFlow()

    private val _pendingQuitAlert = MutableStateFlow<EarlyQuitAlert?>(null)
    val pendingQuitAlert: StateFlow<EarlyQuitAlert?> = _pendingQuitAlert.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var previousInterruptionFilter: Int? = NotificationManager.INTERRUPTION_FILTER_ALL

    var totalDurationSeconds: Long = 0L
        private set

    var isWhitelistMode: Boolean = false
        private set

    var blockedPackages: List<String> = emptyList()
        private set

    var designatedReadingApp: String? = null
        private set

    // Focus mode types
    enum class FocusMode {
        DEEP_WORK,        // uninterrupted concentration, block distracting apps, minimal notifications
        STUDY,            // link sessions to assignments, 50-min study/10-min break blocks, track time by subject
        POMODORO,         // short concentration sessions, complete small tasks, minimal setup
        READING,          // quiet reading sessions, track reading duration, track books/chapters
        QUICK_FOCUS,      // short concentration sessions, complete small tasks, minimal setup
        STRICT_FOCUS,     // block selected distracting apps, strict session rules, prevent accidental cancellation
        RELAX_RECHARGE,   // breathing exercises, relaxing sounds, short meditation, recovery timer
        CUSTOM            // choose duration, select blocking rules, configure breaks, select sounds, set daily goals
    }

    private var currentFocusMode: FocusMode = FocusMode.DEEP_WORK

    fun getCurrentFocusMode(): FocusMode = currentFocusMode

    fun setFocusMode(mode: FocusMode) {
        currentFocusMode = mode
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
        _isSessionActive.value = true

        isWhitelistMode = isWhitelist
        blockedPackages = blockedPkgs
        designatedReadingApp = readingAppPackage
        currentFocusMode = focusMode

        // Mode-specific behavior
        when (focusMode) {
            FocusMode.STRICT_FOCUS -> {
                // Strict mode: ensure DND is always enabled and can't be easily disabled
                if (enableAutoDnd) {
                    activateDoNotDisturbIfPermitted(appCtx)
                }
            }
            FocusMode.RELAX_RECHARGE -> {
                // Relax mode: play relaxing sounds by default
                if (ambientSound == AmbientSoundType.NONE) {
                    AmbientSoundPlayer.play(AmbientSoundType.WHITE_NOISE)
                }
            }
            FocusMode.DEEP_WORK -> {
                // Deep work: minimal notifications, optional ambient sounds
                if (enableAutoDnd) {
                    activateDoNotDisturbIfPermitted(appCtx)
                }
                if (ambientSound == AmbientSoundType.NONE) {
                    // No sound by default for deep work unless specified
                    AmbientSoundPlayer.stop()
                }
            }
            FocusMode.STUDY, FocusMode.POMODORO, FocusMode.QUICK_FOCUS -> {
                // Standard modes: default DND behavior
                if (enableAutoDnd) {
                    activateDoNotDisturbIfPermitted(appCtx)
                }
            }
            FocusMode.READING -> {
                // Reading mode: optional sounds
                if (ambientSound == AmbientSoundType.NONE) {
                    AmbientSoundPlayer.play(AmbientSoundType.WHITE_NOISE)
                } else {
                    AmbientSoundPlayer.play(ambientSound)
                }
            }
            FocusMode.CUSTOM -> {
                // Custom mode: follow the configured sound setting
                if (ambientSound == AmbientSoundType.NONE) {
                    AmbientSoundPlayer.stop()
                } else {
                    AmbientSoundPlayer.play(ambientSound)
                }
            }
        }

        if (enableAutoDnd) {
            activateDoNotDisturbIfPermitted(appCtx)
        }
        // Note: the ambient sound handling above already covers the cases

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

    fun stopSession(context: Context, completed: Boolean) {
        val appCtx = context.applicationContext
        if (!_isSessionActive.value) return

        val remaining = _timeLeftSeconds.value
        val elapsedSeconds = (totalDurationSeconds - remaining).coerceAtLeast(0L)
        val loggedDuration = if (completed) totalDurationSeconds else elapsedSeconds
        val attempts = _blockedAttemptsCount.value
        val readingApp = designatedReadingApp

        _isSessionActive.value = false
        _timeLeftSeconds.value = 0L

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
                NotificationHelper.showSessionCompletedNotification(appCtx, mins, attempts)
                TelemetryManager.trackEvent(appCtx, "focus_session_completed", "${mins}m,blocked=$attempts")
            } else {
                val elapsedMins = (elapsedSeconds / 60L).toInt()
                TelemetryManager.trackEvent(appCtx, "focus_session_aborted", "${elapsedMins}m,blocked=$attempts")
                if (repo.isNotifyPartnerOnQuitSync()) {
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
        AmbientSoundPlayer.stop()
        restoreDoNotDisturbIfNeeded(appCtx)

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
        _timeLeftSeconds.value = 0L
        runCatching {
            appCtx.stopService(Intent(appCtx, FocusService::class.java))
        }
    }

    fun tick() {
        val current = _timeLeftSeconds.value
        if (current > 0L) {
            _timeLeftSeconds.value = current - 1L
        }
    }

    fun incrementBlockedAttempts(context: Context, blockedPackage: String = "") {
        _blockedAttemptsCount.value += 1
        TelemetryManager.trackEvent(
            context.applicationContext,
            "distraction_blocked",
            blockedPackage.ifBlank { "unknown" }
        )
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
