package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.repository.FocusRepository
import com.example.notifications.NotificationHelper
import com.example.telemetry.TelemetryManager
import com.example.ui.FocusViewModel
import com.example.ui.FocusViewModelFactory
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.AccountabilityQuitDialog
import com.example.ui.components.AppLauncherIcon
import com.example.ui.components.ConcaveNotchBottomNavigationBar
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.CoffeeBreakScreen
import com.example.ui.screens.FocusSessionScreen
import com.example.ui.screens.HistoryStatsScreen
import com.example.ui.screens.ReadingAppSelectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SocialScreen
import com.example.ui.screens.TimerSetupScreen
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.MyApplicationTheme

object AppRoutes {
    const val TIMER = "timer"
    const val BLOCKLIST = "apps"
    const val SOCIAL = "rooms"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val READING_APP_PICKER = "reading_app_picker"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val FOCUS_SESSION = "focus_session"
    const val COFFEE_BREAK = "coffee_break"
    const val BEDTIME_LOCK = "bedtime_lock"
}

enum class DashboardTab(val route: String) {
    TIMER(AppRoutes.TIMER),
    BLOCKLIST(AppRoutes.BLOCKLIST),
    SOCIAL(AppRoutes.SOCIAL),
    STATS(AppRoutes.STATS),
    SETTINGS(AppRoutes.SETTINGS)
}

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { FocusRepository(database.focusDao()) }
    private val viewModel: FocusViewModel by viewModels {
        FocusViewModelFactory(repository, applicationContext)
    }

    private var launchActionState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        com.example.bedtime.BedtimeManager.init(this)
        com.example.bedtime.BedtimeReceiver.scheduleBedtimeAlarms(this)
        com.example.service.FocusSessionManager.recoverSessionIfActive(this)
        TelemetryManager.installCrashHandler(this)

        handleIncomingIntent(intent)

        setContent {
            val isDarkMode by viewModel.isDarkModeEnabled.collectAsState()
            val accentThemeId by viewModel.accentThemeId.collectAsState()
            val accentTheme = AppAccentTheme.fromId(accentThemeId)

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.TIMER

            val isSessionActive by viewModel.isSessionActive.collectAsState()
            val isBreakActive by viewModel.isBreakActive.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()
            val stats by viewModel.statsSummary.collectAsState()
            val isAuthBusy by viewModel.isAuthBusy.collectAsState()
            val cloudStatusMessage by viewModel.cloudStatusMessage.collectAsState()
            val pendingQuitAlert by viewModel.pendingQuitAlert.collectAsState()

            var showProfileDialog by remember { mutableStateOf(false) }
            var autoStartFromShortcut by remember { mutableStateOf(false) }
            var suggestedDurationMinutes by remember { mutableIntStateOf(0) }

            LaunchedEffect(launchActionState) {
                when (launchActionState) {
                    "start_focus" -> {
                        autoStartFromShortcut = true
                        launchActionState = null
                        if (currentRoute != AppRoutes.TIMER) {
                            navController.navigate(AppRoutes.TIMER) {
                                launchSingleTop = true
                            }
                        }
                    }
                    "open_rooms" -> {
                        launchActionState = null
                        navController.navigate(AppRoutes.SOCIAL) {
                            launchSingleTop = true
                        }
                    }
                }
            }

            LaunchedEffect(isSessionActive, isBreakActive) {
                if (isSessionActive && currentRoute != AppRoutes.FOCUS_SESSION) {
                    navController.navigate(AppRoutes.FOCUS_SESSION) {
                        launchSingleTop = true
                    }
                } else if (isBreakActive && currentRoute != AppRoutes.COFFEE_BREAK) {
                    navController.navigate(AppRoutes.COFFEE_BREAK) {
                        launchSingleTop = true
                    }
                } else if (!isSessionActive && !isBreakActive &&
                    (currentRoute == AppRoutes.FOCUS_SESSION || currentRoute == AppRoutes.COFFEE_BREAK)
                ) {
                    navController.navigate(AppRoutes.TIMER) {
                        popUpTo(AppRoutes.TIMER) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            fun navigateToTab(tab: DashboardTab) {
                if (currentRoute == tab.route) return
                navController.navigate(tab.route) {
                    popUpTo(AppRoutes.TIMER) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            val showDashboardChrome = currentRoute in setOf(
                AppRoutes.TIMER,
                AppRoutes.BLOCKLIST,
                AppRoutes.SOCIAL,
                AppRoutes.STATS,
                AppRoutes.SETTINGS
            )

            val selectedTab = when (currentRoute) {
                AppRoutes.BLOCKLIST -> DashboardTab.BLOCKLIST
                AppRoutes.SOCIAL -> DashboardTab.SOCIAL
                AppRoutes.STATS -> DashboardTab.STATS
                AppRoutes.SETTINGS -> DashboardTab.SETTINGS
                else -> DashboardTab.TIMER
            }

            MyApplicationTheme(darkTheme = isDarkMode, accentTheme = accentTheme) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        if (showDashboardChrome) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AppLauncherIcon(
                                        packageName = packageName,
                                        contentDescription = "FocusLock logo",
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = 1.dp,
                                                color = if (isDarkMode) Color.White.copy(alpha = 0.28f) else Color.Black,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    )
                                    Text(
                                        text = "FocusLock",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Profile Avatar Button beside dark-mode toggle
                                    val initial = (currentUser?.displayName ?: currentUser?.email ?: "")
                                        .trim()
                                        .firstOrNull()
                                        ?.uppercase()
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .clickable { showProfileDialog = true }
                                            .testTag("top_bar_profile_button")
                                    ) {
                                        if (initial != null) {
                                            Text(
                                                text = initial,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Account Profile",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Dark / Light mode toggle - small circular matching account icon, border removed
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .clickable { viewModel.toggleDarkMode(!isDarkMode) }
                                            .testTag("theme_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (showDashboardChrome) {
                            ConcaveNotchBottomNavigationBar(
                                selectedTab = selectedTab,
                                onSelectTab = { navigateToTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.TIMER,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(AppRoutes.TIMER) {
                            LaunchedEffect(suggestedDurationMinutes) {
                                if (suggestedDurationMinutes > 0) {
                                    getSharedPreferences("focuslock_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putInt("saved_session_minutes", suggestedDurationMinutes)
                                        .apply()
                                    suggestedDurationMinutes = 0
                                }
                            }
                            TimerSetupScreen(
                                viewModel = viewModel,
                                onStartSession = {
                                    navController.navigate(AppRoutes.FOCUS_SESSION) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToBlocklist = { navigateToTab(DashboardTab.BLOCKLIST) },
                                onNavigateToReadingAppPicker = {
                                    navController.navigate(AppRoutes.READING_APP_PICKER) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToStats = { navigateToTab(DashboardTab.STATS) },
                                onNavigateToRooms = { navigateToTab(DashboardTab.SOCIAL) },
                                onNavigateToBedtime = {
                                    navController.navigate(AppRoutes.BEDTIME_LOCK) {
                                        launchSingleTop = true
                                    }
                                },
                                autoStartFromShortcut = autoStartFromShortcut,
                                onAutoStartConsumed = { autoStartFromShortcut = false }
                            )
                        }

                        composable(AppRoutes.BLOCKLIST) {
                            AppSelectionScreen(
                                viewModel = viewModel,
                                onNavigateToTimer = { navigateToTab(DashboardTab.TIMER) }
                            )
                        }

                        composable(AppRoutes.SOCIAL) {
                            SocialScreen(
                                viewModel = viewModel,
                                onApplySuggestedDuration = { mins ->
                                    suggestedDurationMinutes = mins
                                    getSharedPreferences("focuslock_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putInt("saved_session_minutes", mins)
                                        .apply()
                                },
                                onNavigateToSettings = { navigateToTab(DashboardTab.SETTINGS) },
                                onStartFocusSession = {
                                    val duration = getSharedPreferences("focuslock_prefs", MODE_PRIVATE)
                                        .getInt("saved_session_minutes", 25)
                                    viewModel.startFocusSession(this@MainActivity, duration)
                                    navController.navigate(AppRoutes.TIMER) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(AppRoutes.STATS) {
                            HistoryStatsScreen(
                                viewModel = viewModel,
                                onNavigateToTimer = { navigateToTab(DashboardTab.TIMER) }
                            )
                        }

                        composable(AppRoutes.SETTINGS) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onOpenAdminDashboard = {
                                    navController.navigate(AppRoutes.ADMIN_DASHBOARD) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(AppRoutes.READING_APP_PICKER) {
                            ReadingAppSelectionScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onReadingAppSelected = {
                                    navigateToTab(DashboardTab.BLOCKLIST)
                                }
                            )
                        }

                        composable(AppRoutes.ADMIN_DASHBOARD) {
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                onNavigateToTimer = { navigateToTab(DashboardTab.TIMER) },
                                onNavigateToBlocklist = { navigateToTab(DashboardTab.BLOCKLIST) },
                                onNavigateToRooms = { navigateToTab(DashboardTab.SOCIAL) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppRoutes.FOCUS_SESSION) {
                            FocusSessionScreen(
                                viewModel = viewModel,
                                onStopSession = {
                                    navController.navigate(AppRoutes.TIMER) {
                                        popUpTo(AppRoutes.TIMER) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(AppRoutes.COFFEE_BREAK) {
                            CoffeeBreakScreen(
                                viewModel = viewModel,
                                onSkipBreak = {
                                    viewModel.stopBreakSession(this@MainActivity)
                                    navController.navigate(AppRoutes.TIMER) {
                                        popUpTo(AppRoutes.TIMER) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(AppRoutes.BEDTIME_LOCK) {
                            com.example.ui.screens.BedtimeScreen(
                                onBack = { navController.popBackStack() },
                                onOpenEmergencyAccess = {
                                    com.example.bedtime.BedtimeManager.triggerEmergencyOverride(this@MainActivity, 5)
                                }
                            )
                        }
                    }
                }

                if (showProfileDialog) {
                    AccountProfileDialog(
                        currentUser = currentUser,
                        stats = stats,
                        isAuthBusy = isAuthBusy,
                        statusMessage = cloudStatusMessage,
                        onSignInWithGoogle = { viewModel.signInWithGoogle(this@MainActivity) },
                        onSignOut = {
                            viewModel.signOut(this@MainActivity)
                            showProfileDialog = false
                        },
                        onOpenSettings = {
                            showProfileDialog = false
                            navigateToTab(DashboardTab.SETTINGS)
                        },
                        onOpenAdminDashboard = {
                            showProfileDialog = false
                            navController.navigate(AppRoutes.ADMIN_DASHBOARD) {
                                launchSingleTop = true
                            }
                        },
                        onDismiss = { showProfileDialog = false }
                    )
                }

                pendingQuitAlert?.let { alert ->
                    AccountabilityQuitDialog(
                        alert = alert,
                        onDismiss = { viewModel.clearPendingQuitAlert() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incoming: Intent?) {
        val data = incoming?.data
        if (data != null && data.scheme == "readlock" && data.host == "auth") {
            viewModel.handleAuthCallbackIntent(this, incoming)
            // Clear consumed auth URI so configuration changes never replay it
            incoming.data = null
            setIntent(incoming)
        }
        val quickAction = incoming?.getStringExtra("quick_action")
        if (!quickAction.isNullOrBlank()) {
            launchActionState = quickAction
            incoming.removeExtra("quick_action")
        }
    }
}
