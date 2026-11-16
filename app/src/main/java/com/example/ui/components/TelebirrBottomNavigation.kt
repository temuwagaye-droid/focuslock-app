package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.DashboardTab

/**
 * Backward compatibility alias directing to [ConcaveNotchBottomNavigationBar].
 */
@Composable
fun TelebirrBottomNavigationBar(
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
