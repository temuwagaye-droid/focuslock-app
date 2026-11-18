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
import com.example.ui.components.calculateTreeStage
import com.example.widget.FocusQuickWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    var totalDurationSeconds: Long = 0L
        private set

    var isWhitelistMode: Boolean = false
        private set

    var blockedPackages: List<String> = emptyList()
        private set

    var designatedReadingApp: String? = null
        private set

    private var previousInterruptionFilter: Int? = null
    private val managerScope = CoroutineScope(Dispatchers.IO)

    fun startSession(
        context: Context,
        durationMinutes: Int,
        readingAppPackage: String?,
        isWhitelist: Boolean,
        blockedPkgs: List<String>,
        enableAutoDnd: Boolean = false,
        ambientSound: AmbientSoundType = AmbientSoundType.NONE
    ) {
        if (_isSessionActive.value) return
        if (readingAppPackage.isNullOrBlank()) return

        totalDurationSeconds = durationMinutes * 60L
        _timeLeftSeconds.value = totalDurationSeconds
        _blockedAttemptsCount.value = 0
        _pendingQuitAlert.value = null
        isWhitelistMode = isWhitelist
        blockedPackages = blockedPkgs
        designatedReadingApp = readingAppPackage
        _isSessionActive.value = true
        _isBreakActive.value = false

        if (enableAutoDnd) {
            activateDoNotDisturbIfPermitted(context)
        }

        if (ambientSound != AmbientSoundType.NONE) {
            AmbientSoundPlayer.play(ambientSound)
        }

        TelemetryManager.trackEvent(
            context,
            "session_started",
            "duration=${durationMinutes}m,whitelist=$isWhitelist,sound=${ambientSound.id}"
        )

        val intent = Intent(context, FocusService::class.java).apply {
            action = FocusService.ACTION_START_SESSION
            putExtra(FocusService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopSession(context: Context, completed: Boolean) {
        if (!_isSessionActive.value) return

        val durationSec = (totalDurationSeconds - _timeLeftSeconds.value).coerceAtLeast(0L)
        val blockedAttempts = _blockedAttemptsCount.value
        val readingApp = designatedReadingApp

        _isSessionActive.value = false
        _timeLeftSeconds.value = 0L

        restoreDoNotDisturbIfNeeded(context)
        AmbientSoundPlayer.stop()

        managerScope.launch {
            val db = AppDatabase.getDatabase(context)
            val repository = FocusRepository(db.focusDao())
            repository.saveSessionLog(
                durationSeconds = durationSec,
                blockedAttempts = blockedAttempts,
                isCompleted = completed,
                readingApp = readingApp
            )

            // Cache summary for Home/Lock Widget
            val logs = repository.getSessionLogsSync()
            val totalMinutes = logs.sumOf { it.durationSeconds } / 60L
            val completedCount = logs.count { it.isCompleted }
            val totalBlocked = logs.sumOf { it.blockedAttempts }
            context.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE).edit()
                .putLong("widget_total_minutes", totalMinutes)
                .putInt("widget_blocked_attempts", totalBlocked)
                .putInt("widget_completed_sessions", completedCount)
                .apply()
            FocusQuickWidgetProvider.requestWidgetUpdate(context)

            if (completed) {
                val stage = calculateTreeStage(totalMinutes, completedCount)
                NotificationHelper.showSessionCompletedNotification(
                    context = context,
                    durationMinutes = (durationSec / 60L).toInt().coerceAtLeast(1),
                    treeStage = stage
                )
                TelemetryManager.trackEvent(
                    context,
                    "session_completed",
                    "durationSec=$durationSec,blocked=$blockedAttempts,treeStage=$stage"
                )
            } else {
                val notifyPartner = repository.isNotifyPartnerOnQuitSync()
                val elapsedMin = (durationSec / 60L).toInt().coerceAtLeast(0)
                TelemetryManager.trackEvent(
                    context,
                    "session_quit_early",
                    "elapsedSec=$durationSec,blocked=$blockedAttempts,notifyPartner=$notifyPartner"
                )
                if (notifyPartner) {
                    val partnerName = repository.getPartnerNameSync()
                    val partnerEmail = repository.getPartnerEmailSync()
                    NotificationHelper.showAccountabilityQuitNotification(
                        context = context,
                        partnerName = partnerName,
                        elapsedMinutes = elapsedMin,
                        blockedAttempts = blockedAttempts
                    )
                    _pendingQuitAlert.value = EarlyQuitAlert(
                        elapsedMinutes = elapsedMin,
                        blockedAttempts = blockedAttempts,
                        partnerName = partnerName,
                        partnerEmail = partnerEmail
                    )
                }
            }
        }

        if (completed) {
            startBreak(context, durationMinutes = 5)
        } else {
            val intent = Intent(context, FocusService::class.java).apply {
                action = FocusService.ACTION_STOP_SESSION
            }
            context.startService(intent)
        }
    }

    fun clearPendingQuitAlert() {
        _pendingQuitAlert.value = null
    }

    fun startBreak(context: Context, durationMinutes: Int) {
        if (_isBreakActive.value) return

        totalDurationSeconds = durationMinutes * 60L
        _timeLeftSeconds.value = totalDurationSeconds
        _isBreakActive.value = true
        _isSessionActive.value = false

        val intent = Intent(context, FocusService::class.java).apply {
            action = FocusService.ACTION_START_BREAK
            putExtra(FocusService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopBreak(context: Context) {
        if (!_isBreakActive.value) return

        _isBreakActive.value = false
        _timeLeftSeconds.value = 0L

        val intent = Intent(context, FocusService::class.java).apply {
            action = FocusService.ACTION_STOP_SESSION
        }
        context.startService(intent)
    }

    fun tick() {
        if (!_isSessionActive.value && !_isBreakActive.value) return
        if (_timeLeftSeconds.value > 0) {
            _timeLeftSeconds.value -= 1
        }
    }

    fun incrementBlockedAttempts(context: Context? = null, blockedPkg: String? = null) {
        if (_isSessionActive.value) {
            _blockedAttemptsCount.value += 1
            if (context != null) {
                TelemetryManager.trackEvent(context, "distraction_blocked", "pkg=${blockedPkg ?: "unknown"}")
            }
        }
    }

    private fun activateDoNotDisturbIfPermitted(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                previousInterruptionFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                _isDndActivatedBySession.value = true
            }
        } catch (_: Throwable) {
            _isDndActivatedBySession.value = false
        }
    }

    private fun restoreDoNotDisturbIfNeeded(context: Context) {
        val prev = previousInterruptionFilter ?: return
        previousInterruptionFilter = null
        _isDndActivatedBySession.value = false
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(prev)
            }
        } catch (_: Throwable) {
        }
    }
}
