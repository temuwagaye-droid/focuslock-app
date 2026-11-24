package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.example.bedtime.BedtimeManager
import com.example.data.local.AppDatabase
import com.example.data.repository.FocusRepository
import com.example.ui.BlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReadLockAccessibilityService : AccessibilityService() {

    companion object {
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: FocusRepository
    private var isWhitelistMode: Boolean = false
    private var blockedPkgsList: List<String> = emptyList()
    private var designatedReadingApp: String? = null
    private var lastForegroundPackage: String? = null

    private val standardAllowedPackages = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller"
    )

    private val emergencyDialerPackages = setOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.apps.messaging",
        "com.android.mms"
    )

    override fun onCreate() {
        super.onCreate()
        repository = FocusRepository(AppDatabase.getDatabase(applicationContext).focusDao())
        serviceScope.launch {
            repository.isWhitelistModeEnabled().collect { isWhitelistMode = it }
        }
        serviceScope.launch {
            repository.getReadingAppPackage().collect { designatedReadingApp = it }
        }
        serviceScope.launch {
            repository.allBlockedApps.collect { list ->
                blockedPkgsList = list.filter { it.isBlocked }.map { it.packageName }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val isBedtimeActive = BedtimeManager.isBedtimeLockActive.value
        val isFocusActive = FocusSessionManager.isSessionActive.value && !FocusSessionManager.isBreakActive.value

        if (!isFocusActive && !isBedtimeActive) return

        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (pkg == packageName || pkg == lastForegroundPackage) return
        lastForegroundPackage = pkg

        if (standardAllowedPackages.contains(pkg) || isLauncherPackage(pkg) || isEmergencyOrDialerPackage(pkg)) {
            return
        }

        if (isBedtimeActive) {
            val blockedSet = (FocusSessionManager.blockedPackages + blockedPkgsList).toSet()
            val shouldBlock = if (blockedSet.isNotEmpty()) blockedSet.contains(pkg) else !isEssentialSystemApp(pkg)
            if (shouldBlock) {
                val blockIntent = Intent(this, BlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("blocked_package", pkg)
                    putExtra("is_bedtime", true)
                }
                runCatching { startActivity(blockIntent) }
            }
            return
        }

        val mode = FocusSessionManager.getCurrentFocusMode()
        val readerApp = FocusSessionManager.designatedReadingApp ?: designatedReadingApp
        val whitelistActive = FocusSessionManager.isWhitelistMode || isWhitelistMode
        val blockedSet = (FocusSessionManager.blockedPackages + blockedPkgsList).toSet()

        val shouldBlock = when (mode) {
            FocusSessionManager.FocusMode.DEEP_WORK -> {
                // Deep Work: full distraction shield. Allow only essential system apps or designated reader
                if (pkg == readerApp) false
                else if (isEssentialSystemApp(pkg)) false
                else true
            }
            FocusSessionManager.FocusMode.STRICT_FOCUS -> {
                // Strict Focus: maximum lockdown shield, no external distraction allowed
                if (pkg == readerApp) false
                else if (isEssentialSystemApp(pkg)) false
                else true
            }
            FocusSessionManager.FocusMode.RELAX_RECHARGE -> {
                // Relax & Recharge: keep user inside app for guided breathing and recovery
                if (isEssentialSystemApp(pkg)) false
                else true
            }
            FocusSessionManager.FocusMode.READING -> {
                // Reading mode: designated reader app only
                if (pkg == readerApp) false
                else if (isEssentialSystemApp(pkg)) false
                else true
            }
            FocusSessionManager.FocusMode.STUDY -> {
                // Study mode: block social/games/entertainment and blockedSet; allow study/notes/browser research
                if (pkg == readerApp) false
                else if (blockedSet.contains(pkg)) true
                else isSocialOrEntertainmentApp(pkg)
            }
            FocusSessionManager.FocusMode.POMODORO,
            FocusSessionManager.FocusMode.QUICK_FOCUS,
            FocusSessionManager.FocusMode.CUSTOM -> {
                if (pkg == readerApp) false
                else if (whitelistActive) true
                else blockedSet.contains(pkg)
            }
        }

        if (shouldBlock) {
            FocusSessionManager.incrementBlockedAttempts(applicationContext, pkg)
            val blockIntent = Intent(this, BlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("blocked_package", pkg)
                putExtra("is_bedtime", false)
            }
            runCatching { startActivity(blockIntent) }
        }
    }

    private fun isSocialOrEntertainmentApp(pkg: String): Boolean {
        val lower = pkg.lowercase()
        val keywords = listOf(
            "facebook", "instagram", "tiktok", "snapchat", "twitter", "x.com", "reddit",
            "youtube", "netflix", "twitch", "discord", "game", "gaming", "playstation", "xbox",
            "pinterest", "tumblr"
        )
        return keywords.any { lower.contains(it) }
    }

    private fun isEmergencyOrDialerPackage(pkg: String): Boolean {
        if (emergencyDialerPackages.contains(pkg)) return true
        val lower = pkg.lowercase()
        return lower.contains("dialer") || lower.contains("telecom") || lower.contains("emergency")
    }

    private fun isEssentialSystemApp(pkg: String): Boolean {
        if (standardAllowedPackages.contains(pkg) || isLauncherPackage(pkg) || isEmergencyOrDialerPackage(pkg)) return true
        return pkg.startsWith("com.android.system") || pkg.startsWith("com.google.android.inputmethod")
    }

    private fun isLauncherPackage(pkg: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == pkg
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        _isServiceConnected.value = false
        return super.onUnbind(intent)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        _isServiceConnected.value = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
