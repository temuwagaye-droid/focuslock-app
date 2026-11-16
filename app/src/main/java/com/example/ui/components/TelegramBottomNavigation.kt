package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.DashboardTab

/**
 * Backward compatibility alias directing to [ConcaveNotchBottomNavigationBar].
 */
@Composable
fun TelegramBottomNavigationBar(
    currentTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ConcaveNotchBottomNavigationBar(
        currentTab = currentTab,
        onTabSelected = onTabSelected,
        modifier = modifier
    )
}
