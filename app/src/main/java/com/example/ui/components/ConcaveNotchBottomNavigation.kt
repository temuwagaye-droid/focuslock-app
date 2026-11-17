package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DashboardTab
import com.example.ui.theme.BoldSurfaceWhite
import com.example.ui.theme.BoldTextPrimary
import com.example.ui.theme.LocalDarkTheme
import kotlin.math.roundToInt

/**
 * Concave Notch Bottom Navigation Bar supporting 5 tabs: Timer, Apps, Rooms, Stats, Settings.
 */
@Composable
fun ConcaveNotchBottomNavigationBar(
    currentTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val barTopYDp = 16.dp
    val notchRadiusDp = 34.dp
    val notchDepthDp = 30.dp
    val circleSizeDp = 46.dp
    val totalHeightDp = 72.dp

    val barTopYPx = with(density) { barTopYDp.toPx() }
    val notchRadiusPx = with(density) { notchRadiusDp.toPx() }
    val notchDepthPx = with(density) { notchDepthDp.toPx() }
    val circleCenterYPx = with(density) { 23.dp.toPx() }
    val circleSizePx = with(density) { circleSizeDp.toPx() }

    val isDark = LocalDarkTheme.current
    val barSurfaceColor = if (isDark) MaterialTheme.colorScheme.surface else BoldSurfaceWhite
    val hairlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val inactiveIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
    val activeCircleBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val activeIconTint = if (isDark) MaterialTheme.colorScheme.onSurface else BoldTextPrimary

    var circleBounced by remember { mutableStateOf(false) }
    LaunchedEffect(currentTab) {
        circleBounced = true
        kotlinx.coroutines.delay(180)
        circleBounced = false
    }

    val circleScale by animateFloatAsState(
        targetValue = if (circleBounced) 1.14f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "circle_scale"
    )

    val configuration = LocalConfiguration.current
    val fallbackTotalWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_navigation_bar")
    ) {
        val totalWidthPx = if (constraints.maxWidth > 0 && constraints.maxWidth != Int.MAX_VALUE) {
            constraints.maxWidth.toFloat()
        } else {
            fallbackTotalWidthPx
        }
        val tabCount = 5f
        val tabWidthPx = (totalWidthPx / tabCount).coerceAtLeast(1f)

        val selectedIndex = when (currentTab) {
            DashboardTab.TIMER -> 0
            DashboardTab.BLOCKLIST -> 1
            DashboardTab.SOCIAL -> 2
            DashboardTab.STATS -> 3
            DashboardTab.SETTINGS -> 4
        }

        val targetCenterX = tabWidthPx * (selectedIndex + 0.5f)

        val animatedCenterX by animateFloatAsState(
            targetValue = targetCenterX,
            animationSpec = spring(
                dampingRatio = 0.76f,
                stiffness = 380f
            ),
            label = "notch_center_x"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeightDp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = notchRadiusPx
                    val d = notchDepthPx
                    val x = animatedCenterX
                    val y0 = barTopYPx

                    val barPath = Path().apply {
                        moveTo(0f, y0)
                        lineTo(x - w, y0)
                        cubicTo(
                            x1 = x - w * 0.52f, y1 = y0,
                            x2 = x - w * 0.44f, y2 = y0 + d,
                            x3 = x, y3 = y0 + d
                        )
                        cubicTo(
                            x1 = x + w * 0.44f, y1 = y0 + d,
                            x2 = x + w * 0.52f, y2 = y0,
                            x3 = x + w, y3 = y0
                        )
                        lineTo(size.width, y0)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path = barPath, color = barSurfaceColor)

                    val topEdgePath = Path().apply {
                        moveTo(0f, y0)
                        lineTo(x - w, y0)
                        cubicTo(
                            x1 = x - w * 0.52f, y1 = y0,
                            x2 = x - w * 0.44f, y2 = y0 + d,
                            x3 = x, y3 = y0 + d
                        )
                        cubicTo(
                            x1 = x + w * 0.44f, y1 = y0 + d,
                            x2 = x + w * 0.52f, y2 = y0,
                            x3 = x + w, y3 = y0
                        )
                        lineTo(size.width, y0)
                    }
                    drawPath(
                        path = topEdgePath,
                        color = hairlineColor,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalHeightDp)
                        .padding(top = barTopYDp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConcaveNavItem(
                        icon = Icons.Default.Timer,
                        label = "Timer",
                        testTag = "nav_tab_timer",
                        selected = currentTab == DashboardTab.TIMER,
                        onClick = { onTabSelected(DashboardTab.TIMER) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                    ConcaveNavItem(
                        icon = Icons.Default.Block,
                        label = "Apps",
                        testTag = "nav_tab_blocklist",
                        selected = currentTab == DashboardTab.BLOCKLIST,
                        onClick = { onTabSelected(DashboardTab.BLOCKLIST) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                    ConcaveNavItem(
                        icon = Icons.Default.Groups,
                        label = "Rooms",
                        testTag = "nav_tab_social",
                        selected = currentTab == DashboardTab.SOCIAL,
                        onClick = { onTabSelected(DashboardTab.SOCIAL) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                    ConcaveNavItem(
                        icon = Icons.Default.History,
                        label = "Stats",
                        testTag = "nav_tab_stats",
                        selected = currentTab == DashboardTab.STATS,
                        onClick = { onTabSelected(DashboardTab.STATS) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                    ConcaveNavItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        testTag = "nav_tab_settings",
                        selected = currentTab == DashboardTab.SETTINGS,
                        onClick = { onTabSelected(DashboardTab.SETTINGS) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                val circleLeftPx = animatedCenterX - circleSizePx / 2f
                val circleTopPx = circleCenterYPx - circleSizePx / 2f

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = circleLeftPx.roundToInt(),
                                y = circleTopPx.roundToInt()
                            )
                        }
                        .size(circleSizeDp)
                        .scale(circleScale)
                        .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(activeCircleBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 26.dp),
                            onClick = { onTabSelected(currentTab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = currentTab,
                        animationSpec = tween(durationMillis = 200),
                        label = "active_tab_icon"
                    ) { tab ->
                        val activeIcon = when (tab) {
                            DashboardTab.TIMER -> Icons.Default.Timer
                            DashboardTab.BLOCKLIST -> Icons.Default.Block
                            DashboardTab.SOCIAL -> Icons.Default.Groups
                            DashboardTab.STATS -> Icons.Default.History
                            DashboardTab.SETTINGS -> Icons.Default.Settings
                        }
                        Icon(
                            imageVector = activeIcon,
                            contentDescription = tab.name,
                            tint = activeIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConcaveNavItem(
    icon: ImageVector,
    label: String,
    testTag: String,
    selected: Boolean,
    onClick: () -> Unit,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val itemAlpha by animateFloatAsState(
        targetValue = if (selected) 0f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "inactive_nav_item_alpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 28.dp),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(itemAlpha)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = inactiveColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = inactiveColor,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
