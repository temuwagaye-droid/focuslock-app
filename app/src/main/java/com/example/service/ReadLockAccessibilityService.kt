package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
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
        if (!FocusSessionManager.isSessionActive.value || FocusSessionManager.isBreakActive.value) return

        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (pkg == packageName || pkg == lastForegroundPackage) return
        lastForegroundPackage = pkg

        val readerApp = FocusSessionManager.designatedReadingApp ?: designatedReadingApp
        if (pkg == readerApp || standardAllowedPackages.contains(pkg) || isLauncherPackage(pkg)) {
            return
        }

        val whitelistActive = FocusSessionManager.isWhitelistMode || isWhitelistMode
        val blockedSet = (FocusSessionManager.blockedPackages + blockedPkgsList).toSet()
        val shouldBlock = if (whitelistActive) true else blockedSet.contains(pkg)

        if (shouldBlock) {
            FocusSessionManager.incrementBlockedAttempts(applicationContext, pkg)
            val blockIntent = Intent(this, BlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("blocked_package", pkg)
            }
            runCatching { startActivity(blockIntent) }
        }
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
