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
import com.example.data.remote.FocusRoomSyncStore
import com.example.data.remote.RoomParticipantDto
import com.example.data.remote.SupabaseAuthService
import com.example.data.remote.SupabaseGoogleSignInResult
import com.example.data.remote.SupabaseUser
import com.example.data.repository.FocusRepository
import com.example.notifications.NotificationHelper
import com.example.notifications.NotificationHistoryEntry
import com.example.notifications.ReadingReminderItem
import com.example.notifications.ReadingReminderStore
import com.example.notifications.ReminderPreferences
import com.example.notifications.ReminderRepeatMode
import com.example.notifications.ReminderScheduler
import com.example.notifications.ReminderSoundOption
import com.example.service.FocusSessionManager
import com.example.telemetry.TelemetryManager
import com.example.ui.components.calculateTreeStage
import com.example.ui.theme.AppAccentTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val prefs = appContext?.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE)

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
    private val roomSyncStore = FocusRoomSyncStore(appContext)

    // Supabase Auth state stream
    val currentUser: StateFlow<SupabaseUser?> = SupabaseAuthService.currentUser

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
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            prefs?.getBoolean("cached_dark_mode", false) ?: false
        )

    // Pro tier & Accent Theme
    val isProTier: StateFlow<Boolean> = repository.isProTier()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val accentThemeId: StateFlow<String> = repository.getAccentThemeId()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            prefs?.getString("cached_accent_theme", AppAccentTheme.VIOLET.id) ?: AppAccentTheme.VIOLET.id
        )

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

    val readingReminders: StateFlow<List<ReadingReminderItem>> = ReadingReminderStore.reminders
    val reminderPreferences: StateFlow<ReminderPreferences> = ReadingReminderStore.preferences
    val notificationHistory: StateFlow<List<NotificationHistoryEntry>> = ReadingReminderStore.history

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
            if (user == null) flowOf(emptyList())
            else if (firebaseAuth?.currentUser != null && roomRepo != null) {
                combine(roomSyncStore.observePublicRooms(), roomRepo.observePublicRooms()) { local, remote ->
                    (local + remote).distinctBy { it.roomId }
                }
            } else {
                roomSyncStore.observePublicRooms()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRooms: StateFlow<List<FocusRoomDto>> = roomSyncStore.observeAllRooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRoom: StateFlow<FocusRoomDto?> = combine(currentUser, _selectedRoomId) { user, rid ->
        if (user == null || rid.isNullOrBlank()) null else rid
    }.flatMapLatest { rid ->
        if (rid == null) flowOf(null)
        else if (firebaseAuth?.currentUser != null && roomRepo != null) {
            combine(roomSyncStore.observeRoom(rid), roomRepo.observeRoom(rid)) { local, remote ->
                local ?: remote
            }
        } else {
            roomSyncStore.observeRoom(rid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeRoomParticipants: StateFlow<List<RoomParticipantDto>> = combine(currentUser, _selectedRoomId) { user, rid ->
        if (user == null || rid.isNullOrBlank()) null else rid
    }.flatMapLatest { rid ->
        if (rid == null) flowOf(emptyList())
        else if (firebaseAuth?.currentUser != null && roomRepo != null) {
            combine(roomSyncStore.observeParticipants(rid), roomRepo.observeParticipants(rid)) { local, remote ->
                if (local.isNotEmpty()) local else remote
            }
        } else {
            roomSyncStore.observeParticipants(rid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _telemetryRefreshTick = MutableStateFlow(0)
    val telemetryRefreshTick: StateFlow<Int> = _telemetryRefreshTick.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _cloudStatusMessage = MutableStateFlow<String?>(null)
    val cloudStatusMessage: StateFlow<String?> = _cloudStatusMessage.asStateFlow()

    private val _lastCloudSyncText = MutableStateFlow("Not synced yet")
    val lastCloudSyncText: StateFlow<String> = _lastCloudSyncText.asStateFlow()

    init {
        appContext?.let { ctx ->
            SupabaseAuthService.initialize(ctx)
            ReadingReminderStore.ensureLoaded(ctx)
            viewModelScope.launch {
                SupabaseAuthService.refreshSessionIfNeeded(ctx)
            }
        }
        viewModelScope.launch {
            isDarkModeEnabled.collect { dark ->
                prefs?.edit()?.putBoolean("cached_dark_mode", dark)?.apply()
            }
        }
        viewModelScope.launch {
            accentThemeId.collect { theme ->
                prefs?.edit()?.putString("cached_accent_theme", theme)?.apply()
            }
        }
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
        viewModelScope.launch {
            if (enabled && !isProTier.value) {
                _showProLimitPrompt.value = true
                return@launch
            }
            repository.setWhitelistModeEnabled(enabled)
        }
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
            if (!isWhitelist && blockedPkgs.isEmpty()) return@launch
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
            val user = currentUser.value
            if (!rid.isNullOrBlank() && user != null) {
                val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
                roomSyncStore.updateParticipantStatus(user.uid, rid, "focusing", 0, stage)
                if (firebaseAuth?.currentUser != null) {
                    roomRepo?.updateMyParticipantStatus(rid, "focusing", 0, stage)
                }
            }
        }
    }

    fun stopFocusSession(context: Context, completed: Boolean) {
        FocusSessionManager.stopSession(context, completed)
        val rid = _selectedRoomId.value
        val user = currentUser.value
        if (!rid.isNullOrBlank() && user != null) {
            viewModelScope.launch {
                val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
                val status = if (completed) "completed" else "ready"
                roomSyncStore.updateParticipantStatus(user.uid, rid, status, blockedAttemptsCount.value, stage)
                if (firebaseAuth?.currentUser != null) {
                    roomRepo?.updateMyParticipantStatus(
                        rid,
                        status,
                        blockedAttemptsCount.value,
                        stage
                    )
                }
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
        prefs?.edit()?.putBoolean("cached_dark_mode", enabled)?.apply()
        viewModelScope.launch { repository.setDarkModeEnabled(enabled) }
    }

    fun setProTier(enabled: Boolean) {
        viewModelScope.launch {
            repository.setProTier(enabled)
            if (!enabled) {
                repository.setWhitelistModeEnabled(false)
                val theme = AppAccentTheme.fromId(accentThemeId.value)
                if (theme.isProOnly) {
                    prefs?.edit()?.putString("cached_accent_theme", AppAccentTheme.VIOLET.id)?.apply()
                    repository.setAccentThemeId(AppAccentTheme.VIOLET.id)
                }
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
            prefs?.edit()?.putString("cached_accent_theme", theme.id)?.apply()
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

    fun createReadingReminder(
        context: Context,
        title: String,
        note: String,
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        repeatMode: ReminderRepeatMode,
        soundOption: ReminderSoundOption,
        customSoundUri: String? = null,
        customSoundLabel: String? = null
    ) {
        ReadingReminderStore.createReminder(
            context = context,
            title = title,
            note = note,
            year = year,
            month = month,
            dayOfMonth = dayOfMonth,
            hour = hour,
            minute = minute,
            repeatMode = repeatMode,
            soundOption = soundOption,
            customSoundUri = customSoundUri,
            customSoundLabel = customSoundLabel
        )
        TelemetryManager.trackEvent(context, "reading_reminder_created", "${repeatMode.id}:$hour:$minute")
    }

    fun updateReadingReminder(context: Context, item: ReadingReminderItem) {
        ReadingReminderStore.updateReminder(context, item)
        TelemetryManager.trackEvent(context, "reading_reminder_edited", item.id)
    }

    fun deleteReadingReminder(context: Context, reminderId: String) {
        ReadingReminderStore.deleteReminder(context, reminderId)
        TelemetryManager.trackEvent(context, "reading_reminder_deleted", reminderId)
    }

    fun toggleReadingReminder(context: Context, reminderId: String, enabled: Boolean) {
        ReadingReminderStore.setReminderEnabled(context, reminderId, enabled)
    }

    fun snoozeReadingReminder(context: Context, reminderId: String, snoozeMinutes: Int? = null) {
        ReadingReminderStore.snoozeReminder(context, reminderId, snoozeMinutes)
        TelemetryManager.trackEvent(context, "reading_reminder_snoozed", reminderId)
    }

    fun completeReadingReminder(context: Context, reminderId: String) {
        ReadingReminderStore.markReminderCompleted(context, reminderId)
        TelemetryManager.trackEvent(context, "reading_reminder_completed", reminderId)
    }

    fun dismissReadingReminder(context: Context, reminderId: String) {
        ReadingReminderStore.dismissReminder(context, reminderId)
        TelemetryManager.trackEvent(context, "reading_reminder_dismissed", reminderId)
    }

    fun updateReminderPreferences(context: Context, prefs: ReminderPreferences) {
        ReadingReminderStore.updatePreferences(context, prefs)
        viewModelScope.launch {
            repository.setReminderEnabled(prefs.globalEnabled)
        }
        TelemetryManager.trackEvent(context, "reading_reminder_prefs_updated", "quiet=${prefs.quietHoursEnabled}")
    }

    fun sendTestReminderNotification(context: Context) {
        ReadingReminderStore.ensureLoaded(context)
        val prefs = ReadingReminderStore.preferences.value
        val sample = ReadingReminderStore.reminders.value.firstOrNull()
            ?: ReadingReminderItem(
                id = "test_preview",
                title = "FocusLock Reading Time",
                note = "Open your designated reader and earn focus XP!",
                year = 2026,
                month = 1,
                dayOfMonth = 1,
                hour = 20,
                minute = 0,
                repeatMode = ReminderRepeatMode.DAILY,
                soundOption = prefs.defaultSoundOption,
                customSoundUri = prefs.defaultCustomSoundUri,
                customSoundLabel = prefs.defaultCustomSoundLabel
            )
        NotificationHelper.showCustomReminderNotification(
            context = context,
            reminder = sample,
            prefs = prefs,
            bypassQuietHours = false
        )
    }

    fun clearReminderHistory(context: Context) {
        ReadingReminderStore.clearHistory(context)
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

    private var lastProcessedAuthCallbackUri: String? = null

    // --- Supabase Google Sign-In & Cloud Operations ---
    fun signInWithGoogle(activityContext: Context) {
        if (_isAuthBusy.value) return
        viewModelScope.launch {
            _isAuthBusy.value = true
            _cloudStatusMessage.value = null
            try {
                val res = SupabaseAuthService.signInWithGoogle(activityContext)
                res.onSuccess { outcome ->
                    when (outcome) {
                        is SupabaseGoogleSignInResult.SignedIn -> {
                            val user = outcome.user
                            _cloudStatusMessage.value = "Signed in as ${user.displayName ?: user.email ?: "Reader"}"
                            TelemetryManager.trackEvent(activityContext, "supabase_google_sign_in_success")
                            if (firebaseAuth?.currentUser != null) {
                                syncNowInternal(showStatus = false)
                            }
                        }
                        is SupabaseGoogleSignInResult.BrowserFlowLaunched -> {
                            _cloudStatusMessage.value = "Complete Google Sign-In in your browser to finish connecting."
                        }
                    }
                }.onFailure { err ->
                    _cloudStatusMessage.value = err.message ?: "Sign-in failed."
                }
            } finally {
                _isAuthBusy.value = false
            }
        }
    }

    fun handleAuthCallbackIntent(context: Context, intent: Intent?) {
        val dataUri = intent?.data ?: return
        if (dataUri.scheme != "readlock" || dataUri.host != "auth") return
        val uriKey = dataUri.toString()
        if (lastProcessedAuthCallbackUri == uriKey) return
        lastProcessedAuthCallbackUri = uriKey

        viewModelScope.launch {
            _isAuthBusy.value = true
            try {
                val res = SupabaseAuthService.handleOAuthCallback(context, dataUri)
                res.onSuccess { user ->
                    _cloudStatusMessage.value = "Signed in as ${user.displayName ?: user.email ?: "Reader"}"
                    TelemetryManager.trackEvent(context, "supabase_oauth_callback_success")
                    if (firebaseAuth?.currentUser != null) {
                        syncNowInternal(showStatus = false)
                    }
                }.onFailure { err ->
                    _cloudStatusMessage.value = err.message ?: "OAuth sign-in failed."
                }
            } finally {
                _isAuthBusy.value = false
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            _selectedRoomId.value = null
            SupabaseAuthService.signOut(context)
            runCatching { firebaseAuth?.signOut() }
            _cloudStatusMessage.value = "Signed out cleanly."
        }
    }

    fun syncCloudNow() {
        viewModelScope.launch { syncNowInternal(showStatus = true) }
    }

    private suspend fun syncNowInternal(showStatus: Boolean) {
        val repo = cloudSyncRepo ?: run {
            if (showStatus) {
                _cloudStatusMessage.value = "Signed in with Supabase. Configure Firebase Firestore to enable cloud room/score sync."
            }
            return
        }
        if (firebaseAuth?.currentUser == null) {
            if (showStatus) {
                _cloudStatusMessage.value = "Supabase session active (${currentUser.value?.email ?: "Google"})."
            }
            return
        }
        val user = currentUser.value ?: return
        _isSyncingCloud.value = true
        try {
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
        val repo = cloudSyncRepo ?: run {
            _cloudStatusMessage.value = "Cloud restore requires Firebase Firestore configuration."
            return
        }
        if (currentUser.value == null) return
        if (firebaseAuth?.currentUser == null) {
            _cloudStatusMessage.value = "Supabase account connected (${currentUser.value?.email ?: "Google"}). Firestore backup is disabled without Firebase Auth."
            return
        }
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
        val repo = cloudSyncRepo ?: run {
            _cloudStatusMessage.value = "No Firebase cloud backup configured."
            return
        }
        if (currentUser.value == null) return
        if (firebaseAuth?.currentUser == null) {
            _cloudStatusMessage.value = "Signed in with Supabase. No Firebase cloud records to delete."
            return
        }
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
        val user = currentUser.value ?: run {
            _cloudStatusMessage.value = "Sign in with Google first to create a focus room."
            return
        }
        viewModelScope.launch {
            val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
            val createdRoom = roomSyncStore.createRoom(
                user = user,
                name = name,
                durationMinutes = durationMinutes,
                isPublic = isPublic,
                treeStage = stage
            )
            _selectedRoomId.value = createdRoom.roomId
            _cloudStatusMessage.value = "Room ${createdRoom.roomCode} created!"
            appContext?.let { TelemetryManager.trackEvent(it, "room_created", createdRoom.roomCode) }

            if (firebaseAuth?.currentUser != null && roomRepo != null) {
                roomRepo.createRoom(name, durationMinutes, isPublic, stage)
            }
        }
    }

    fun joinFocusRoom(roomId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
            val localRes = roomSyncStore.joinRoom(user, roomId, stage)
            if (localRes.isSuccess) {
                _selectedRoomId.value = roomId
                appContext?.let { TelemetryManager.trackEvent(it, "room_joined", roomId) }
            } else if (firebaseAuth?.currentUser != null && roomRepo != null) {
                roomRepo.joinRoom(roomId, stage).onSuccess {
                    _selectedRoomId.value = roomId
                    appContext?.let { TelemetryManager.trackEvent(it, "room_joined", roomId) }
                }.onFailure { err ->
                    _cloudStatusMessage.value = err.message ?: "Could not join room."
                }
            } else {
                _cloudStatusMessage.value = localRes.exceptionOrNull()?.message ?: "Could not join room."
            }
        }
    }

    fun joinFocusRoomByCode(code: String) {
        val user = currentUser.value ?: run {
            _cloudStatusMessage.value = "Sign in with Google first to join a focus room."
            return
        }
        viewModelScope.launch {
            val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
            val localRes = roomSyncStore.joinRoomByCode(user, code, stage)
            if (localRes.isSuccess) {
                val room = localRes.getOrThrow()
                _selectedRoomId.value = room.roomId
                _cloudStatusMessage.value = "Joined ${room.name}!"
                appContext?.let { TelemetryManager.trackEvent(it, "room_joined_by_code", room.roomCode) }
            } else if (firebaseAuth?.currentUser != null && roomRepo != null) {
                roomRepo.joinRoomByCode(code, stage).onSuccess { room ->
                    _selectedRoomId.value = room.roomId
                    _cloudStatusMessage.value = "Joined ${room.name}!"
                    appContext?.let { TelemetryManager.trackEvent(it, "room_joined_by_code", room.roomCode) }
                }.onFailure { err ->
                    _cloudStatusMessage.value = err.message ?: "Room code not found."
                }
            } else {
                _cloudStatusMessage.value = localRes.exceptionOrNull()?.message ?: "Room code not found."
            }
        }
    }

    fun setRoomStatus(roomId: String, status: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            roomSyncStore.updateRoomStatus(roomId, status).onFailure { err ->
                _cloudStatusMessage.value = err.message
            }
            if (firebaseAuth?.currentUser != null && roomRepo != null) {
                roomRepo.updateRoomStatus(roomId, status)
            }
            appContext?.let { TelemetryManager.trackEvent(it, "room_status_$status", "${user.uid}:$roomId") }
        }
    }

    fun leaveActiveRoom() {
        val user = currentUser.value
        val rid = _selectedRoomId.value ?: return
        _selectedRoomId.value = null
        viewModelScope.launch {
            if (user != null) {
                roomSyncStore.leaveRoom(user.uid, rid)
            }
            if (firebaseAuth?.currentUser != null && roomRepo != null) {
                roomRepo.leaveRoom(rid)
            }
        }
    }

    // --- Admin Dashboard Full Control Operations ---
    fun adminSetWhitelistOverride(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !isProTier.value) {
                repository.setProTier(true)
            }
            repository.setWhitelistModeEnabled(enabled)
            appContext?.let { TelemetryManager.trackEvent(it, "admin_whitelist_override", "enabled=$enabled") }
            _telemetryRefreshTick.value += 1
        }
    }

    fun adminForceStartSession(context: Context, durationMinutes: Int) {
        viewModelScope.launch {
            val readingApp = repository.getReadingAppPackageSync()?.takeIf { it.isNotBlank() }
                ?: designatedReadingApp.value?.takeIf { it.isNotBlank() }
                ?: installedApps.value.firstOrNull()?.packageName
                ?: context.packageName
            if (designatedReadingApp.value.isNullOrBlank() && readingApp != context.packageName) {
                repository.setReadingAppPackage(readingApp)
            }
            val isWhitelist = repository.isWhitelistModeEnabledSync()
            val blockedPkgs = repository.getBlockedAppsSync().filter { it.isBlocked }.map { it.packageName }
            val autoDnd = repository.isAutoDndEnabledSync()
            val sound = AmbientSoundType.fromId(repository.getAmbientSoundIdSync())

            FocusSessionManager.startSession(
                context = context,
                durationMinutes = durationMinutes.coerceIn(1, 180),
                readingAppPackage = readingApp,
                isWhitelist = isWhitelist,
                blockedPkgs = blockedPkgs,
                enableAutoDnd = autoDnd,
                ambientSound = sound
            )
            TelemetryManager.trackEvent(context, "admin_force_start_session", "${durationMinutes}m")
            _telemetryRefreshTick.value += 1
        }
    }

    fun adminForceStopSessionOrBreak(context: Context) {
        if (isSessionActive.value) {
            FocusSessionManager.stopSession(context, completed = false)
        }
        if (isBreakActive.value) {
            FocusSessionManager.stopBreak(context)
        }
        TelemetryManager.trackEvent(context, "admin_force_stop_session")
        _telemetryRefreshTick.value += 1
    }

    fun adminClearAllBlockedApps() {
        viewModelScope.launch {
            repository.clearBlockedApps()
            appContext?.let { TelemetryManager.trackEvent(it, "admin_clear_blocked_apps") }
            _telemetryRefreshTick.value += 1
        }
    }

    fun adminResetParentalLock() {
        viewModelScope.launch {
            repository.setParentalLockEnabled(false)
            repository.setParentalPinHash("")
            appContext?.let { TelemetryManager.trackEvent(it, "admin_reset_parental_lock") }
            _telemetryRefreshTick.value += 1
        }
    }

    fun adminResetSessionHistory() {
        viewModelScope.launch {
            repository.clearSessionLogs()
            appContext?.let { TelemetryManager.trackEvent(it, "admin_reset_session_history") }
            _telemetryRefreshTick.value += 1
        }
    }

    fun adminClearTelemetry(context: Context) {
        TelemetryManager.clearAllLogs(context)
        _telemetryRefreshTick.value += 1
    }

    fun adminSetRoomStatus(roomId: String, status: String) {
        viewModelScope.launch {
            roomSyncStore.updateRoomStatus(roomId, status)
            if (firebaseAuth?.currentUser != null && roomRepo != null) {
                roomRepo.updateRoomStatus(roomId, status)
            }
            appContext?.let { TelemetryManager.trackEvent(it, "admin_room_status", "$roomId->$status") }
            _telemetryRefreshTick.value += 1
        }
    }

    fun adminDeleteRoom(roomId: String) {
        viewModelScope.launch {
            if (_selectedRoomId.value == roomId) {
                _selectedRoomId.value = null
            }
            roomSyncStore.deleteRoom(roomId)
            appContext?.let { TelemetryManager.trackEvent(it, "admin_delete_room", roomId) }
            _telemetryRefreshTick.value += 1
        }
    }

    fun adminQuickCreateRoom(name: String, durationMinutes: Int, isPublic: Boolean) {
        val adminUser = currentUser.value ?: SupabaseUser(
            uid = "admin_host",
            email = "admin@focuslock.app",
            displayName = "Admin Host",
            avatarUrl = null,
            accessToken = "",
            refreshToken = null,
            expiresAtEpochSeconds = Long.MAX_VALUE
        )
        viewModelScope.launch {
            val stage = calculateTreeStage(statsSummary.value.totalFocusMinutes, statsSummary.value.completedSessionsCount)
            val created = roomSyncStore.createRoom(adminUser, name, durationMinutes, isPublic, stage)
            appContext?.let { TelemetryManager.trackEvent(it, "admin_create_room", created.roomCode) }
            _telemetryRefreshTick.value += 1
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
