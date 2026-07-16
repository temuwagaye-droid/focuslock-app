package com.example.service

import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object FocusSessionManager {

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isBreakActive = MutableStateFlow(false)
    val isBreakActive: StateFlow<Boolean> = _isBreakActive.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(0L)
    val timeLeftSeconds: StateFlow<Long> = _timeLeftSeconds.asStateFlow()

    private val _blockedAttemptsCount = MutableStateFlow(0)
    val blockedAttemptsCount: StateFlow<Int> = _blockedAttemptsCount.asStateFlow()

    var totalDurationSeconds: Long = 0L
        private set

    var isWhitelistMode: Boolean = false
        private set

    var blockedPackages: List<String> = emptyList()
        private set

    var designatedReadingApp: String? = null
        private set

    private val managerScope = CoroutineScope(Dispatchers.IO)

    fun startSession(
        context: Context,
        durationMinutes: Int,
        readingAppPackage: String?,
        isWhitelist: Boolean,
        blockedPkgs: List<String>
    ) {
        if (_isSessionActive.value) return

        totalDurationSeconds = durationMinutes * 60L
        _timeLeftSeconds.value = totalDurationSeconds
        _blockedAttemptsCount.value = 0
        isWhitelistMode = isWhitelist
        blockedPackages = blockedPkgs
        designatedReadingApp = readingAppPackage
        _isSessionActive.value = true
        _isBreakActive.value = false

        // Start Foreground Service to keep it running
        val intent = Intent(context, FocusService::class.java).apply {
            action = FocusService.ACTION_START_SESSION
            putExtra(FocusService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        context.startForegroundService(intent)
    }

    fun stopSession(context: Context, completed: Boolean) {
        if (!_isSessionActive.value) return

        val durationSec = totalDurationSeconds - _timeLeftSeconds.value
        val blockedAttempts = _blockedAttemptsCount.value
        val readingApp = designatedReadingApp

        _isSessionActive.value = false
        _timeLeftSeconds.value = 0L

        // Save session log in background
        managerScope.launch {
            val db = AppDatabase.getDatabase(context)
            val repository = FocusRepository(db.focusDao())
            repository.saveSessionLog(
                durationSeconds = durationSec,
                blockedAttempts = blockedAttempts,
                isCompleted = completed,
                readingApp = readingApp
            )
        }

        if (completed) {
            // Automatically start Coffee Break session
            startBreak(context, durationMinutes = 5)
        } else {
            // Stop foreground service
            val intent = Intent(context, FocusService::class.java).apply {
                action = FocusService.ACTION_STOP_SESSION
            }
            context.startService(intent)
        }
    }

    fun startBreak(context: Context, durationMinutes: Int) {
        if (_isBreakActive.value) return

        totalDurationSeconds = durationMinutes * 60L
        _timeLeftSeconds.value = totalDurationSeconds
        _isBreakActive.value = true
        _isSessionActive.value = false

        // Start Foreground Service with BREAK action
        val intent = Intent(context, FocusService::class.java).apply {
            action = FocusService.ACTION_START_BREAK
            putExtra(FocusService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        context.startForegroundService(intent)
    }

    fun stopBreak(context: Context) {
        if (!_isBreakActive.value) return

        _isBreakActive.value = false
        _timeLeftSeconds.value = 0L

        // Stop foreground service
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

    fun incrementBlockedAttempts() {
        if (_isSessionActive.value) {
            _blockedAttemptsCount.value += 1
        }
    }
}
