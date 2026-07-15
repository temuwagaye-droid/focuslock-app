package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.data.local.AppDatabase
import com.example.data.repository.FocusRepository
import com.example.ui.FocusViewModel
import com.example.ui.FocusViewModelFactory
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.FocusSessionScreen
import com.example.ui.screens.HistoryStatsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimerSetupScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { FocusRepository(database.focusDao()) }
    private val viewModel: FocusViewModel by viewModels { FocusViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isSessionActive by viewModel.isSessionActive.collectAsState()
                var currentTab by remember { mutableStateOf(DashboardTab.TIMER) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    bottomBar = {
                        // Hide bottom navigation if session is active to keep focus absolute
                        if (!isSessionActive) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.testTag("main_navigation_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == DashboardTab.TIMER,
                                    onClick = { currentTab = DashboardTab.TIMER },
                                    icon = { Icon(Icons.Default.Timer, contentDescription = "Timer") },
                                    label = { Text("Timer") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.secondary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_tab_timer")
                                )
                                NavigationBarItem(
                                    selected = currentTab == DashboardTab.BLOCKLIST,
                                    onClick = { currentTab = DashboardTab.BLOCKLIST },
                                    icon = { Icon(Icons.Default.Block, contentDescription = "Blocklist") },
                                    label = { Text("Blocklist") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.secondary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_tab_blocklist")
                                )
                                NavigationBarItem(
                                    selected = currentTab == DashboardTab.STATS,
                                    onClick = { currentTab = DashboardTab.STATS },
                                    icon = { Icon(Icons.Default.History, contentDescription = "Stats") },
                                    label = { Text("Stats") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.secondary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_tab_stats")
                                )
                                NavigationBarItem(
                                    selected = currentTab == DashboardTab.SETTINGS,
                                    onClick = { currentTab = DashboardTab.SETTINGS },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.secondary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
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
                        if (isSessionActive) {
                            FocusSessionScreen(
                                viewModel = viewModel,
                                onStopSession = { viewModel.stopFocusSession(this@MainActivity, completed = false) }
                            )
                        } else {
                            when (currentTab) {
                                DashboardTab.TIMER -> TimerSetupScreen(
                                    viewModel = viewModel,
                                    onStartSession = {}
                                )
                                DashboardTab.BLOCKLIST -> AppSelectionScreen(
                                    viewModel = viewModel
                                )
                                DashboardTab.STATS -> HistoryStatsScreen(
                                    viewModel = viewModel
                                )
                                DashboardTab.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class DashboardTab {
    TIMER,
    BLOCKLIST,
    STATS,
    SETTINGS
}
