package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.example.ui.components.AccountabilityQuitDialog
import com.example.ui.components.ConcaveNotchBottomNavigationBar
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.CoffeeBreakScreen
import com.example.ui.screens.FocusSessionScreen
import com.example.ui.screens.HistoryStatsScreen
import com.example.ui.screens.ReadingAppSelectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SocialScreen
import com.example.ui.screens.TimerSetupScreen
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppRoutes {
    const val TIMER = "timer"
    const val BLOCKLIST = "apps"
    const val SOCIAL = "rooms"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val READING_APP_PICKER = "reading_app_picker"
    const val FOCUS_SESSION = "focus_session"
    const val COFFEE_BREAK = "coffee_break"
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
        TelemetryManager.installCrashHandler(applicationContext)
        NotificationHelper.ensureChannels(this)
        launchActionState = intent?.getStringExtra("quick_action")
        viewModel.handleAuthCallbackIntent(this, intent)

        setContent {
            val isDarkModeEnabled by viewModel.isDarkModeEnabled.collectAsState()
            val accentThemeId by viewModel.accentThemeId.collectAsState()
            val accentTheme = AppAccentTheme.fromId(accentThemeId)

            MyApplicationTheme(darkTheme = isDarkModeEnabled, accentTheme = accentTheme) {
                val isSessionActive by viewModel.isSessionActive.collectAsState()
                val isBreakActive by viewModel.isBreakActive.collectAsState()
                val pendingQuitAlert by viewModel.pendingQuitAlert.collectAsState()

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.TIMER

                var autoTriggerStart by remember { mutableStateOf(false) }
                var barsVisible by remember { mutableStateOf(true) }
                var showLauncherIntro by remember { mutableStateOf(true) }

                // Smooth launcher entrance animation on app open
                val appEntranceAlpha = remember { Animatable(0f) }
                val appEntranceScale = remember { Animatable(0.95f) }
                LaunchedEffect(Unit) {
                    launch {
                        appEntranceAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                        )
                    }
                    launch {
                        appEntranceScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                    delay(380)
                    showLauncherIntro = false
                }

                // Always reveal bars when switching destinations
                LaunchedEffect(currentRoute) {
                    barsVisible = true
                }

                fun navigateToTab(tab: DashboardTab) {
                    val targetRoute = when (tab) {
                        DashboardTab.TIMER -> AppRoutes.TIMER
                        DashboardTab.BLOCKLIST -> AppRoutes.BLOCKLIST
                        DashboardTab.SOCIAL -> AppRoutes.SOCIAL
                        DashboardTab.STATS -> AppRoutes.STATS
                        DashboardTab.SETTINGS -> AppRoutes.SETTINGS
                    }
                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                // Handle launcher/widget quick actions
                LaunchedEffect(launchActionState) {
                    when (launchActionState) {
                        "open_rooms" -> {
                            navigateToTab(DashboardTab.SOCIAL)
                            launchActionState = null
                        }
                        "start_focus" -> {
                            navigateToTab(DashboardTab.TIMER)
                            autoTriggerStart = true
                            launchActionState = null
                        }
                    }
                }

                // Sync active session / break screens into NavHost hierarchy
                LaunchedEffect(isSessionActive, isBreakActive) {
                    when {
                        isSessionActive -> {
                            if (currentRoute != AppRoutes.FOCUS_SESSION) {
                                navController.navigate(AppRoutes.FOCUS_SESSION) {
                                    launchSingleTop = true
                                }
                            }
                        }
                        isBreakActive -> {
                            if (currentRoute != AppRoutes.COFFEE_BREAK) {
                                navController.navigate(AppRoutes.COFFEE_BREAK) {
                                    popUpTo(AppRoutes.FOCUS_SESSION) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        else -> {
                            if (currentRoute == AppRoutes.FOCUS_SESSION || currentRoute == AppRoutes.COFFEE_BREAK) {
                                navController.navigate(AppRoutes.TIMER) {
                                    popUpTo(AppRoutes.TIMER) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }

                // Hide bars when scrolling up (reading down); show bars when scrolling down
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (available.y < -10f) {
                                barsVisible = false
                            } else if (available.y > 10f) {
                                barsVisible = true
                            }
                            return Offset.Zero
                        }
                    }
                }

                val currentTab = when (currentRoute) {
                    AppRoutes.BLOCKLIST -> DashboardTab.BLOCKLIST
                    AppRoutes.SOCIAL -> DashboardTab.SOCIAL
                    AppRoutes.STATS -> DashboardTab.STATS
                    AppRoutes.SETTINGS -> DashboardTab.SETTINGS
                    else -> DashboardTab.TIMER
                }

                val isImmersiveRoute = isSessionActive || isBreakActive ||
                    currentRoute == AppRoutes.FOCUS_SESSION || currentRoute == AppRoutes.COFFEE_BREAK
                val isChildRoute = currentRoute == AppRoutes.READING_APP_PICKER

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .graphicsLayer {
                            alpha = appEntranceAlpha.value
                            scaleX = appEntranceScale.value
                            scaleY = appEntranceScale.value
                        }
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                        topBar = {
                            AnimatedVisibility(
                                visible = !isImmersiveRoute && (barsVisible || isChildRoute),
                                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(220)),
                                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(180))
                            ) {
                                FocusLockTopAppBar(
                                    currentRoute = currentRoute,
                                    isDarkModeEnabled = isDarkModeEnabled,
                                    onToggleDarkMode = { viewModel.toggleDarkMode(!isDarkModeEnabled) },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        },
                        bottomBar = {
                            AnimatedVisibility(
                                visible = !isImmersiveRoute && !isChildRoute && barsVisible,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(220)),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(180))
                            ) {
                                ConcaveNotchBottomNavigationBar(
                                    currentTab = currentTab,
                                    onTabSelected = { navigateToTab(it) }
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets.systemBars
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = AppRoutes.TIMER,
                                enterTransition = { fadeIn(tween(240)) },
                                exitTransition = { fadeOut(tween(200)) },
                                popEnterTransition = { fadeIn(tween(240)) },
                                popExitTransition = { fadeOut(tween(200)) }
                            ) {
                                composable(AppRoutes.TIMER) {
                                    TimerSetupScreen(
                                        viewModel = viewModel,
                                        onStartSession = {},
                                        onNavigateToBlocklist = { navigateToTab(DashboardTab.BLOCKLIST) },
                                        onNavigateToReadingAppPicker = {
                                            navController.navigate(AppRoutes.READING_APP_PICKER) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onNavigateToStats = { navigateToTab(DashboardTab.STATS) },
                                        onNavigateToRooms = { navigateToTab(DashboardTab.SOCIAL) },
                                        autoStartFromShortcut = autoTriggerStart,
                                        onAutoStartConsumed = { autoTriggerStart = false }
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
                                        onStartGroupSession = { mins ->
                                            getSharedPreferences("focuslock_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putInt("saved_session_minutes", mins)
                                                .apply()
                                            viewModel.startFocusSession(this@MainActivity, mins)
                                        },
                                        onNavigateToStats = { navigateToTab(DashboardTab.STATS) },
                                        onNavigateToTimer = { navigateToTab(DashboardTab.TIMER) }
                                    )
                                }
                                composable(AppRoutes.STATS) {
                                    HistoryStatsScreen(
                                        viewModel = viewModel,
                                        onNavigateToTimer = { navigateToTab(DashboardTab.TIMER) }
                                    )
                                }
                                composable(AppRoutes.SETTINGS) {
                                    SettingsScreen(viewModel = viewModel)
                                }
                                composable(
                                    route = AppRoutes.READING_APP_PICKER,
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn(tween(260)) },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut(tween(200)) }
                                ) {
                                    ReadingAppSelectionScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(AppRoutes.FOCUS_SESSION) {
                                    FocusSessionScreen(
                                        viewModel = viewModel,
                                        onStopSession = { viewModel.stopFocusSession(this@MainActivity, completed = false) }
                                    )
                                }
                                composable(AppRoutes.COFFEE_BREAK) {
                                    CoffeeBreakScreen(
                                        viewModel = viewModel,
                                        onSkipBreak = { viewModel.stopBreakSession(this@MainActivity) }
                                    )
                                }
                            }
                        }
                    }

                    // Brief smooth launcher brand reveal overlay
                    AnimatedVisibility(
                        visible = showLauncherIntro,
                        exit = fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing))
                    ) {
                        LauncherSplashIntroOverlay()
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchActionState = intent.getStringExtra("quick_action")
        viewModel.handleAuthCallbackIntent(this, intent)
    }
}

@Composable
private fun FocusLockTopAppBar(
    currentRoute: String,
    isDarkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    onBackClick: () -> Unit
) {
    val sectionLabel = when (currentRoute) {
        AppRoutes.TIMER -> "Timer"
        AppRoutes.BLOCKLIST -> "Apps"
        AppRoutes.SOCIAL -> "Rooms"
        AppRoutes.STATS -> "Stats"
        AppRoutes.SETTINGS -> "Settings"
        AppRoutes.READING_APP_PICKER -> "Select Reading App"
        else -> "FocusLock"
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_top_app_bar")
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentRoute == AppRoutes.READING_APP_PICKER) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("topbar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Timer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = sectionLabel,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon),
                            contentDescription = "FocusLock Logo",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "FocusLock",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = sectionLabel.uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(
                            1.dp,
                            if (isDarkModeEnabled) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                        .testTag("quick_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = if (isDarkModeEnabled) "Switch to light mode" else "Switch to dark mode",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun LauncherSplashIntroOverlay() {
    val logoScale = remember { Animatable(0.84f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = logoScale.value
                scaleY = logoScale.value
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = "FocusLock Launcher Icon",
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "FocusLock",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Deep Reading & Focus",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

enum class DashboardTab {
    TIMER,
    BLOCKLIST,
    SOCIAL,
    STATS,
    SETTINGS
}
