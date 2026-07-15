package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.repository.FocusRepository
import com.example.ui.BlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReadLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var repository: FocusRepository

    // Cached configurations for fast lookups
    private var isWhitelistMode = false
    private var blockedPkgsList = emptyList<String>()
    private var designatedReadingApp: String? = null

    // System/Emergency packages that are ALWAYS allowed
    private val standardAllowedPackages = setOf(
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.dialer",
        "com.android.contacts",
        "android",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.inputmethod.latin",
        "com.sec.android.inputmethod",
        "com.swiftkey.swiftkeykeyboard"
    )

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = FocusRepository(db.focusDao())

        // Reactively observe changes to configurations
        serviceScope.launch {
            repository.isWhitelistModeEnabled().collectLatest {
                isWhitelistMode = it
            }
        }
        serviceScope.launch {
            repository.allBlockedApps.collectLatest { apps ->
                blockedPkgsList = apps.filter { it.isBlocked }.map { it.packageName }
            }
        }
        serviceScope.launch {
            repository.getReadingAppPackage().collectLatest {
                designatedReadingApp = if (it.isNullOrEmpty()) null else it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return

        // 1. Don't block our own app or common system tasks
        if (foregroundPackage == packageName || standardAllowedPackages.contains(foregroundPackage)) {
            return
        }

        val sessionActive = FocusSessionManager.isSessionActive.value

        if (sessionActive) {
            val isWhitelist = FocusSessionManager.isWhitelistMode
            val readingApp = FocusSessionManager.designatedReadingApp
            val blockedPkgs = FocusSessionManager.blockedPackages

            val shouldBlock = if (isWhitelist) {
                // In Whitelist Mode, block everything EXCEPT ReadLock and the designated reading app
                foregroundPackage != readingApp
            } else {
                // In Blacklist Mode, block apps in the blocked list
                blockedPkgs.contains(foregroundPackage)
            }

            if (shouldBlock) {
                // Prevent infinite redirect loops if the launcher tries to open
                if (isLauncherPackage(foregroundPackage)) {
                    return
                }

                FocusSessionManager.incrementBlockedAttempts()

                // Boot the user back out and open our Block screen
                val intent = Intent(this, BlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(BlockActivity.EXTRA_BLOCKED_PACKAGE, foregroundPackage)
                }
                startActivity(intent)
            }
        } else {
            // Auto-start session if user opens their designated reading app
            val readingAppPkg = FocusSessionManager.designatedReadingApp ?: designatedReadingApp
            if (readingAppPkg != null && foregroundPackage == readingAppPkg) {
                // Auto-start for 25 minutes as a default Pomodoro style
                FocusSessionManager.startSession(
                    context = applicationContext,
                    durationMinutes = 25,
                    readingAppPackage = readingAppPkg,
                    isWhitelist = isWhitelistMode,
                    blockedPkgs = blockedPkgsList
                )
            }
        }
    }

    private fun isLauncherPackage(pkg: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        for (info in resolveInfos) {
            if (info.activityInfo?.packageName == pkg) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        // Required method implementation
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
