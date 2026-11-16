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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.ui.components.ConcaveNotchBottomNavigationBar
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.CoffeeBreakScreen
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
            val isDarkModeEnabled by viewModel.isDarkModeEnabled.collectAsState()
            MyApplicationTheme(darkTheme = isDarkModeEnabled) {
                val isSessionActive by viewModel.isSessionActive.collectAsState()
                val isBreakActive by viewModel.isBreakActive.collectAsState()
                var currentTab by remember { mutableStateOf(DashboardTab.TIMER) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    bottomBar = {
                        // Hide bottom navigation if session or break is active to keep focus absolute
                        if (!isSessionActive && !isBreakActive) {
                            ConcaveNotchBottomNavigationBar(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it }
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
                        if (isSessionActive) {
                            FocusSessionScreen(
                                viewModel = viewModel,
                                onStopSession = { viewModel.stopFocusSession(this@MainActivity, completed = false) }
                            )
                        } else if (isBreakActive) {
                            CoffeeBreakScreen(
                                viewModel = viewModel,
                                onSkipBreak = { viewModel.stopBreakSession(this@MainActivity) }
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
