package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AmbientSoundPlayer
import com.example.audio.AmbientSoundType
import com.example.data.model.BlockedAppEntity
import com.example.data.model.SessionLogEntity
import com.example.data.model.TodoEntity
import com.example.data.remote.CloudPrivateSettings
import com.example.data.remote.CloudSyncRepository
import com.example.data.remote.CloudUserProfile
import com.example.data.remote.FirebaseService
import com.example.data.remote.FocusRoomDto
import com.example.data.remote.FocusRoomRepository
import com.example.data.remote.RoomParticipantDto
import com.example.data.repository.FocusRepository
import com.example.notifications.ReminderScheduler
import com.example.service.FocusSessionManager
import com.example.telemetry.TelemetryManager
import com.example.ui.components.calculateTreeStage
import com.example.ui.theme.AppAccentTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModel(
    private val repository: FocusRepository,
    private val appContext: Context? = null
) : ViewModel() {

    companion object {
        const val FREE_TIER_MAX_BLOCKED_APPS = 5
    }

    private val isFirebaseAvailable: Boolean = appContext?.let { ctx ->
        runCatching { FirebaseApp.getApps(ctx).isNotEmpty() }.getOrDefault(false)
    } ?: false

    private val firebaseAuth: FirebaseAuth? = if (isFirebaseAvailable) runCatching { FirebaseService.auth() }.getOrNull() else null
    private val cloudSyncRepo: CloudSyncRepository? = appContext?.let { ctx ->
        firebaseAuth?.let { auth ->
            if (isFirebaseAvailable) {
                runCatching { CloudSyncRepository(FirebaseService.firestore(ctx), auth) }.getOrNull()
            } else null
        }
    }
    private val roomRepo: FocusRoomRepository? = appContext?.let { ctx ->
        firebaseAuth?.let { auth ->
            if (isFirebaseAvailable) {
                runCatching { FocusRoomRepository(FirebaseService.firestore(ctx), auth) }.getOrNull()
            } else null
        }
    }

    // Auth state stream (emits null when signed out or Firebase not initialized)
    val currentUser: StateFlow<FirebaseUser?> = if (firebaseAuth != null) {
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                trySend(auth.currentUser)
            }
            firebaseAuth.addAuthStateListener(listener)
            trySend(firebaseAuth.currentUser)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firebaseAuth.currentUser)
    } else {
        MutableStateFlow(null)
    }

    // Installed launcher apps list
    private val _installedApps = MutableStateFlow<List<AppInfoModel>>(emptyList())
    val installedApps: StateFlow<List<AppInfoModel>> = _installedApps.asStateFlow()

    // Core Database elements
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

    val todoItems: StateFlow<List<TodoEntity>> = repository.allTodoItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDarkModeEnabled: StateFlow<Boolean> = repository.isDarkModeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Pro tier & Accent Theme
    val isProTier: StateFlow<Boolean> = repository.isProTier()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val accentThemeId: StateFlow<String> = repository.getAccentThemeId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppAccentTheme.VIOLET.id)

    private val _showProLimitPrompt = MutableStateFlow(false)
    val showProLimitPrompt: StateFlow<Boolean> = _showProLimitPrompt.asStateFlow()

    // DND & Ambient Sound
    val isAutoDndEnabled: StateFlow<Boolean> = repository.isAutoDndEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedAmbientSoundId: StateFlow<String> = repository.getAmbientSoundId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AmbientSoundType.NONE.id)

    // Reminders
    val isReminderEnabled: StateFlow<Boolean> = repository.isReminderEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderHour: StateFlow<Int> = repository.getReminderHour()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val reminderMinute: StateFlow<Int> = repository.getReminderMinute()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Accountability Partner
    val partnerName: StateFlow<String> = repository.getPartnerName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val partnerEmail: StateFlow<String> = repository.getPartnerEmail()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val notifyPartnerOnQuit: StateFlow<Boolean> = repository.isNotifyPartnerOnQuit()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Parental Controls
    val isParentalLockEnabled: StateFlow<Boolean> = repository.isParentalLockEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val parentalPinHash: StateFlow<String> = repository.getParentalPinHash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Privacy, Sync & Telemetry
    val isLeaderboardVisible: StateFlow<Boolean> = repository.isLeaderboardVisible()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isCloudSyncEnabled: StateFlow<Boolean> = repository.isCloudSyncEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val analyticsOptIn: StateFlow<Boolean> = repository.isAnalyticsOptIn()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val crashReportingOptIn: StateFlow<Boolean> = repository.isCrashReportingOptIn()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Stats
    val sessionLogs: StateFlow<List<SessionLogEntity>> = repository.allSessionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsSummary: StateFlow<FocusStatsSummary> = repository.allSessionLogs
        .mapLogsToSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusStatsSummary(0, 0, 0, 0, 0))

    // Active session bindings
    val isSessionActive = FocusSessionManager.isSessionActive
    val isBreakActive = FocusSessionManager.isBreakActive
    val timeLeftSeconds = FocusSessionManager.timeLeftSeconds
    val blockedAttemptsCount = FocusSessionManager.blockedAttemptsCount
    val isDndActivatedBySession = FocusSessionManager.isDndActivatedBySession
    val pendingQuitAlert = FocusSessionManager.pendingQuitAlert

    // Social, Leaderboard & Sync UI State (strictly auth-gated via flatMapLatest on currentUser)
    private val _selectedRoomId = MutableStateFlow<String?>(null)
    val selectedRoomId: StateFlow<String?> = _selectedRoomId.asStateFlow()

    val leaderboardEntries: StateFlow<List<CloudUserProfile>> = currentUser
        .flatMapLatest { user ->
            if (user == null || cloudSyncRepo == null) flowOf(emptyList())
            else cloudSyncRepo.observeLeaderboard()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val publicRooms: StateFlow<List<FocusRoomDto>> = currentUser
        .flatMapLatest { user ->
            if (user == null || roomRepo == null) flowOf(emptyList())
            else roomRepo.observePublicRooms()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRoom: StateFlow<FocusRoomDto?> = combine(currentUser, _selectedRoomId) { user, rid ->
        if (user == null || rid.isNullOrBlank()) null else rid
    }.flatMapLatest { rid ->
        if (rid == null || roomRepo == null) flowOf(null)
        else roomRepo.observeRoom(rid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeRoomParticipants: StateFlow<List<RoomParticipantDto>> = combine(currentUser, _selectedRoomId) { user, rid ->
        if (user == null || rid.isNullOrBlank()) null else rid
    }.flatMapLatest { rid ->
        if (rid == null || roomRepo == null) flowOf(emptyList())
        else roomRepo.observeParticipants(rid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _cloudStatusMessage = MutableStateFlow<String?>(null)
    val cloudStatusMessage: StateFlow<String?> = _cloudStatusMessage.asStateFlow()

    private val _lastCloudSyncText = MutableStateFlow("Not synced yet")
    val lastCloudSyncText: StateFlow<String> = _lastCloudSyncText.asStateFlow()

    init {
        viewModelScope.launch {
            analyticsOptIn.collect { TelemetryManager.isAnalyticsOptIn = it }
        }
        viewModelScope.launch {
            crashReportingOptIn.collect { TelemetryManager.isCrashReportingOptIn = it }
        }
    }

    fun loadInstalledLauncherApps(context: Context) {
        viewModelScope.launch {
            if (_installedApps.value.isNotEmpty()) return@launch
            val appList = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val list = mutableListOf<AppInfoModel>()
                for (info in resolveInfos) {
                    val pkgName = info.activityInfo.packageName
                    if (pkgName == context.packageName) continue
                    val label = info.loadLabel(pm).toString()
                    list.add(AppInfoModel(pkgName, label))
                }
                list.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
            }
            _installedApps.value = appList
        }
    }

    fun toggleAppBlock(packageName: String, appName: String, isBlocked: Boolean) {
        viewModelScope.launch {
            if (isBlocked) {
                val currentBlockedCount = blockedAppsList.value.count { it.isBlocked }
                if (!isProTier.value && currentBlockedCount >= FREE_TIER_MAX_BLOCKED_APPS) {
                    _showProLimitPrompt.value = true
                    return@launch
                }
                repository.insertBlockedApp(packageName, appName)
            } else {
                repository.removeBlockedApp(packageName)
            }
        }
    }

    fun dismissProLimitPrompt() {
        _showProLimitPrompt.value = false
    }

    fun toggleWhitelistMode(enabled: Boolean) {
        viewModelScope.launch { repository.setWhitelistModeEnabled(enabled) }
    }

    fun selectReadingApp(packageName: String?) {
        viewModelScope.launch { repository.setReadingAppPackage(packageName) }
    }

    fun toggleSchedule(enabled: Boolean) {
        viewModelScope.launch { repository.setScheduleEnabled(enabled) }
    }

    fun saveScheduleHours(start: Int, end: Int) {
        viewModelScope.launch {
            repository.setScheduleStartHour(start)
            repository.setScheduleEndHour(end)
        }
    }

    fun startFocusSession(context: Context, durationMinutes: Int) {
        viewModelScope.launch {
            val readingApp = repository.getReadingAppPackageSync()?.takeIf { it.isNotBlank() }
                ?: designatedReadingApp.value?.takeIf { it.isNotBlank() }
                ?: return@launch
            val isWhitelist = repository.isWhitelistModeEnabledSync()
            val blockedPkgs = repository.getBlockedAppsSync().filter { it.isBlocked }.map { it.packageName }
            if (!isWhitelist && blockedPkgs.isEmpty()) {
                return@launch
            }
            val autoDnd = repository.isAutoDndEnabledSync()
            val sound = AmbientSoundType.fromId(repository.getAmbientSoundIdSync())
            val effectiveSound = if (sound.isProOnly && !isProTier.value) AmbientSoundType.WHITE_NOISE else sound

            FocusSessionManager.startSession(
                context = context,
                durationMinutes = durationMinutes,
                readingAppPackage = readingApp,
                isWhitelist = isWhitelist,
                blockedPkgs = blockedPkgs,
                enableAutoDnd = autoDnd,
                ambientSound = effectiveSound
            )

            // Update active multiplayer room participant status if inside a room
            val rid = _selectedRoomId.value
            if (!rid.isNullOrBlank() && currentUser.value != null) {
                val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
                roomRepo?.updateMyParticipantStatus(rid, "focusing", 0, stage)
            }
        }
    }

    fun stopFocusSession(context: Context, completed: Boolean) {
        FocusSessionManager.stopSession(context, completed)
        val rid = _selectedRoomId.value
        if (!rid.isNullOrBlank() && currentUser.value != null) {
            viewModelScope.launch {
                val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
                roomRepo?.updateMyParticipantStatus(
                    rid,
                    if (completed) "completed" else "ready",
                    blockedAttemptsCount.value,
                    stage
                )
            }
        }
    }

    fun stopBreakSession(context: Context) {
        FocusSessionManager.stopBreak(context)
    }

    fun clearPendingQuitAlert() {
        FocusSessionManager.clearPendingQuitAlert()
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkModeEnabled(enabled) }
    }

    fun setProTier(enabled: Boolean) {
        viewModelScope.launch {
            repository.setProTier(enabled)
            if (!enabled) {
                val theme = AppAccentTheme.fromId(accentThemeId.value)
                if (theme.isProOnly) repository.setAccentThemeId(AppAccentTheme.VIOLET.id)
                val sound = AmbientSoundType.fromId(selectedAmbientSoundId.value)
                if (sound.isProOnly) repository.setAmbientSoundId(AmbientSoundType.WHITE_NOISE.id)
            }
            appContext?.let { TelemetryManager.trackEvent(it, "tier_updated", "isPro=$enabled") }
        }
    }

    fun selectAccentTheme(theme: AppAccentTheme) {
        viewModelScope.launch {
            if (theme.isProOnly && !isProTier.value) {
                _showProLimitPrompt.value = true
                return@launch
            }
            repository.setAccentThemeId(theme.id)
            appContext?.let { TelemetryManager.trackEvent(it, "theme_selected", theme.id) }
        }
    }

    fun setAutoDndEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoDndEnabled(enabled) }
    }

    fun selectAmbientSound(sound: AmbientSoundType, previewIfSessionActive: Boolean = true) {
        viewModelScope.launch {
            if (sound.isProOnly && !isProTier.value) {
                _showProLimitPrompt.value = true
                return@launch
            }
            repository.setAmbientSoundId(sound.id)
            if (previewIfSessionActive && isSessionActive.value) {
                if (sound == AmbientSoundType.NONE) AmbientSoundPlayer.stop() else AmbientSoundPlayer.play(sound)
            }
        }
    }

    fun updateReminderSettings(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.setReminderEnabled(enabled)
            repository.setReminderTime(hour, minute)
            ReminderScheduler.scheduleDailyReminder(context, enabled, hour, minute)
            TelemetryManager.trackEvent(context, "reminder_updated", "enabled=$enabled,$hour:$minute")
        }
    }

    fun saveAccountabilityPartner(name: String, email: String, notifyOnQuit: Boolean) {
        viewModelScope.launch {
            repository.setPartnerName(name.trim())
            repository.setPartnerEmail(email.trim())
            repository.setNotifyPartnerOnQuit(notifyOnQuit)
        }
    }

    fun setParentalLock(enabled: Boolean, pinHash: String? = null) {
        viewModelScope.launch {
            if (!pinHash.isNullOrBlank()) {
                repository.setParentalPinHash(pinHash)
            }
            repository.setParentalLockEnabled(enabled)
        }
    }

    fun setLeaderboardVisible(visible: Boolean) {
        viewModelScope.launch {
            repository.setLeaderboardVisible(visible)
            syncNowInternal(showStatus = false)
        }
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setCloudSyncEnabled(enabled) }
    }

    fun setAnalyticsOptIn(optIn: Boolean) {
        viewModelScope.launch {
            repository.setAnalyticsOptIn(optIn)
            TelemetryManager.isAnalyticsOptIn = optIn
        }
    }

    fun setCrashReportingOptIn(optIn: Boolean) {
        viewModelScope.launch {
            repository.setCrashReportingOptIn(optIn)
            TelemetryManager.isCrashReportingOptIn = optIn
        }
    }

    fun addTodoItem(title: String) {
        viewModelScope.launch { repository.insertTodoItem(title) }
    }

    fun toggleTodoItem(item: TodoEntity) {
        viewModelScope.launch { repository.updateTodoItem(item.copy(isCompleted = !item.isCompleted)) }
    }

    fun deleteTodoItem(id: Long) {
        viewModelScope.launch { repository.deleteTodoItem(id) }
    }

    // --- Google Sign-In & Cloud Operations ---
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _cloudStatusMessage.value = null
            val res = FirebaseService.signInWithGoogle(activityContext)
            res.onSuccess { user ->
                _cloudStatusMessage.value = "Signed in as ${user.displayName ?: user.email ?: "Reader"}"
                TelemetryManager.trackEvent(activityContext, "google_sign_in_success")
                syncNowInternal(showStatus = true)
            }.onFailure { err ->
                _cloudStatusMessage.value = err.message ?: "Sign-in failed."
            }
            _isAuthBusy.value = false
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            _selectedRoomId.value = null
            FirebaseService.signOut(context)
            _cloudStatusMessage.value = "Signed out cleanly."
        }
    }

    fun syncCloudNow() {
        viewModelScope.launch { syncNowInternal(showStatus = true) }
    }

    private suspend fun syncNowInternal(showStatus: Boolean) {
        val repo = cloudSyncRepo ?: return
        val user = currentUser.value ?: return
        _isSyncingCloud.value = true
        try {
            val prefs = appContext?.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE)
            val savedDuration = prefs?.getInt("saved_session_minutes", 15) ?: 15
            val summary = statsSummary.value
            val stage = calculateTreeStage(summary.totalFocusMinutes, summary.completedSessionsCount)

            val settings = CloudPrivateSettings(
                userId = user.uid,
                isDarkMode = isDarkModeEnabled.value,
                accentTheme = accentThemeId.value,
                isWhitelistMode = isWhitelistMode.value,
                designatedReadingApp = designatedReadingApp.value ?: "",
                sessionDurationMinutes = savedDuration,
                autoDndEnabled = isAutoDndEnabled.value,
                ambientSound = selectedAmbientSoundId.value,
                reminderEnabled = isReminderEnabled.value,
                reminderHour = reminderHour.value,
                reminderMinute = reminderMinute.value,
                partnerName = partnerName.value,
                partnerEmail = partnerEmail.value,
                notifyPartnerOnQuit = notifyPartnerOnQuit.value,
                parentalLockEnabled = isParentalLockEnabled.value,
                parentalPinHash = parentalPinHash.value,
                analyticsOptIn = analyticsOptIn.value,
                crashReportingOptIn = crashReportingOptIn.value,
                cloudSyncEnabled = isCloudSyncEnabled.value,
                isPro = isProTier.value
            )

            repo.syncProfileAndSettings(
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Reader",
                focusScore = summary.focusScore,
                totalFocusMinutes = summary.totalFocusMinutes.toInt(),
                completedSessionsCount = summary.completedSessionsCount,
                currentStreak = summary.currentStreak,
                totalBlockedAttempts = summary.totalBlockedAttempts,
                treeStage = stage,
                isLeaderboardVisible = isLeaderboardVisible.value,
                settings = settings
            ).getOrThrow()

            val sessions = repository.getSessionLogsSync()
            val blocked = repository.getBlockedAppsSync()
            val todos = repository.getTodoItemsSync()
            val count = repo.backupUserData(sessions, blocked, todos).getOrThrow()

            if (appContext != null && analyticsOptIn.value) {
                TelemetryManager.getAnalyticsEvents(appContext).take(10).forEach { entry ->
                    repo.uploadTelemetry(entry)
                }
            }
            if (appContext != null && crashReportingOptIn.value) {
                TelemetryManager.getCrashReports(appContext).take(5).forEach { entry ->
                    repo.uploadTelemetry(entry)
                }
            }

            _lastCloudSyncText.value = "Synced ($count items) just now"
            if (showStatus) {
                _cloudStatusMessage.value = "Cloud backup & profile sync complete ($count items)."
            }
        } catch (e: Exception) {
            if (showStatus) {
                _cloudStatusMessage.value = e.message ?: "Cloud sync failed."
            }
        } finally {
            _isSyncingCloud.value = false
        }
    }

    fun restoreFromCloud() {
        val repo = cloudSyncRepo ?: return
        if (currentUser.value == null) return
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val res = repo.fetchCloudRestoreBundle()
            res.onSuccess { bundle ->
                repository.restoreFromCloudBundle(bundle)
                _lastCloudSyncText.value = "Restored from cloud"
                _cloudStatusMessage.value = "Restored ${bundle.sessions.size} sessions, ${bundle.blockedApps.size} apps & ${bundle.todos.size} tasks."
            }.onFailure { err ->
                _cloudStatusMessage.value = err.message ?: "Cloud restore failed."
            }
            _isSyncingCloud.value = false
        }
    }

    fun deleteCloudData() {
        val repo = cloudSyncRepo ?: return
        if (currentUser.value == null) return
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val res = repo.deleteAllCloudData()
            res.onSuccess {
                _lastCloudSyncText.value = "Cloud data cleared"
                _cloudStatusMessage.value = "All your cloud backup and leaderboard records were deleted."
            }.onFailure { err ->
                _cloudStatusMessage.value = err.message ?: "Failed to delete cloud data."
            }
            _isSyncingCloud.value = false
        }
    }

    // --- Group Focus Rooms ---
    fun createFocusRoom(name: String, durationMinutes: Int, isPublic: Boolean) {
        val repo = roomRepo ?: return
        if (currentUser.value == null) return
        viewModelScope.launch {
            val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
            val res = repo.createRoom(name, durationMinutes, isPublic, stage)
            res.onSuccess { room ->
                _selectedRoomId.value = room.roomId
                _cloudStatusMessage.value = "Room ${room.roomCode} created!"
                appContext?.let { TelemetryManager.trackEvent(it, "room_created", room.roomCode) }
            }.onFailure { err ->
                _cloudStatusMessage.value = err.message ?: "Could not create room."
            }
        }
    }

    fun joinFocusRoom(roomId: String) {
        val repo = roomRepo ?: return
        if (currentUser.value == null) return
        viewModelScope.launch {
            val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
            val res = repo.joinRoom(roomId, stage)
            res.onSuccess {
                _selectedRoomId.value = roomId
                appContext?.let { TelemetryManager.trackEvent(it, "room_joined", roomId) }
            }.onFailure { err ->
                _cloudStatusMessage.value = err.message ?: "Could not join room."
            }
        }
    }

    fun joinFocusRoomByCode(code: String) {
        val repo = roomRepo ?: return
        if (currentUser.value == null) return
        viewModelScope.launch {
            val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
            val res = repo.joinRoomByCode(code, stage)
            res.onSuccess { room ->
                _selectedRoomId.value = room.roomId
                _cloudStatusMessage.value = "Joined ${room.name}!"
                appContext?.let { TelemetryManager.trackEvent(it, "room_joined_by_code", room.roomCode) }
            }.onFailure { err ->
                _cloudStatusMessage.value = err.message ?: "Room code not found."
            }
        }
    }

    fun setRoomStatus(roomId: String, status: String) {
        val repo = roomRepo ?: return
        if (currentUser.value == null) return
        viewModelScope.launch {
            repo.updateRoomStatus(roomId, status).onFailure {
                _cloudStatusMessage.value = it.message
            }
        }
    }

    fun leaveActiveRoom() {
        val repo = roomRepo ?: return
        val rid = _selectedRoomId.value ?: return
        _selectedRoomId.value = null
        viewModelScope.launch {
            repo.leaveRoom(rid)
        }
    }

    fun clearCloudStatusMessage() {
        _cloudStatusMessage.value = null
    }

    private fun Flow<List<SessionLogEntity>>.mapLogsToSummary(): Flow<FocusStatsSummary> {
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
                if (log.isCompleted) completedCount++
            }
            val totalFocusMin = totalSec / 60
            val streak = calculateStreak(logs)
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

        val localTimeZone = TimeZone.getDefault()
        val uniqueDays = completedLogs.map { log ->
            val cal = Calendar.getInstance(localTimeZone)
            cal.timeInMillis = log.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis / (1000 * 60 * 60 * 24)
        }.distinct().sortedDescending()

        if (uniqueDays.isEmpty()) return 0

        val todayCal = Calendar.getInstance(localTimeZone)
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        val todayEpochDay = todayCal.timeInMillis / (1000 * 60 * 60 * 24)

        val latestCompletedDay = uniqueDays.first()
        if (todayEpochDay - latestCompletedDay > 1) return 0

        var streak = 1
        for (i in 0 until uniqueDays.size - 1) {
            if (uniqueDays[i] - uniqueDays[i + 1] == 1L) streak++ else break
        }
        return streak
    }
}

class FocusViewModelFactory(
    private val repository: FocusRepository,
    private val appContext: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FocusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FocusViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
