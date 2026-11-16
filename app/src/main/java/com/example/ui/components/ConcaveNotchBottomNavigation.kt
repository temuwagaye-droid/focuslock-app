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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DashboardTab
import com.example.ui.theme.BoldPrimary
import com.example.ui.theme.BoldSurfaceWhite
import com.example.ui.theme.BoldTextPrimary
import kotlin.math.roundToInt

/**
 * Concave Notch Bottom Navigation Bar.
 *
 * Implements the continuous concave curved cutout (notch/dip) tracking the active tab,
 * with a floating circular tab button cradled inside the concave scoop, as shown in the
 * reference design.
 *
 * - Active tab sits inside the concave scoop within an elevated circular white badge
 * - Notch scoop smoothly animates its horizontal center X to the selected tab with a spring
 * - Inactive tabs display along the flat baseline with muted icons and labels
 * - Full edge-to-edge support with navigationBarsPadding()
 */
@Composable
fun ConcaveNotchBottomNavigationBar(
    currentTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Geometry constants
    val barTopYDp = 16.dp
    val notchRadiusDp = 42.dp
    val notchDepthDp = 34.dp
    val circleSizeDp = 50.dp
    val totalHeightDp = 74.dp

    val barTopYPx = with(density) { barTopYDp.toPx() }
    val notchRadiusPx = with(density) { notchRadiusDp.toPx() }
    val notchDepthPx = with(density) { notchDepthDp.toPx() }
    val circleCenterYPx = with(density) { 24.dp.toPx() }
    val circleSizePx = with(density) { circleSizeDp.toPx() }

    // Colors matching the reference design:
    // Reference has rich purple background dipping down, white bar surface, and white floating circle
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val barSurfaceColor = if (isDark) MaterialTheme.colorScheme.surface else BoldSurfaceWhite
    val cradleAccentColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else BoldPrimary
    val hairlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val inactiveIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val activeCircleBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val activeIconTint = if (isDark) MaterialTheme.colorScheme.primary else BoldTextPrimary

    // Micro bounce scale for circular badge on tab switch
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

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
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
        val tabWidthPx = (totalWidthPx / 4f).coerceAtLeast(1f)

        val selectedIndex = when (currentTab) {
            DashboardTab.TIMER -> 0
            DashboardTab.BLOCKLIST -> 1
            DashboardTab.STATS -> 2
            DashboardTab.SETTINGS -> 3
        }

        val targetCenterX = tabWidthPx * (selectedIndex + 0.5f)

        // Smooth spring physics for sliding concave scoop
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
                // 1. Canvas rendering the Concave Notch cutout and cradle accent
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = notchRadiusPx
                    val d = notchDepthPx
                    val x = animatedCenterX
                    val y0 = barTopYPx

                    // A. Draw the colored concave cradle scoop (vibrant purple accent from reference)
                    val cradlePath = Path().apply {
                        moveTo(x - w, 0f)
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
                        lineTo(x + w, 0f)
                        close()
                    }
                    drawPath(
                        path = cradlePath,
                        color = cradleAccentColor
                    )

                    // B. Draw the solid white bar body with the concave scoop cutout
                    val barPath = Path().apply {
                        moveTo(0f, y0)
                        lineTo(x - w, y0)
                        // Smooth left shoulder down into the scoop
                        cubicTo(
                            x1 = x - w * 0.52f, y1 = y0,
                            x2 = x - w * 0.44f, y2 = y0 + d,
                            x3 = x, y3 = y0 + d
                        )
                        // Smooth right shoulder up out of the scoop
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
                    drawPath(
                        path = barPath,
                        color = barSurfaceColor
                    )

                    // C. Subtle hairline along flat top edges outside the notch
                    if (x - w > 0f) {
                        drawLine(
                            color = hairlineColor,
                            start = androidx.compose.ui.geometry.Offset(0f, y0),
                            end = androidx.compose.ui.geometry.Offset(x - w, y0),
                            strokeWidth = 1f
                        )
                    }
                    if (x + w < size.width) {
                        drawLine(
                            color = hairlineColor,
                            start = androidx.compose.ui.geometry.Offset(x + w, y0),
                            end = androidx.compose.ui.geometry.Offset(size.width, y0),
                            strokeWidth = 1f
                        )
                    }
                }

                // 2. Inactive Tab Items Row along the flat bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalHeightDp)
                        .padding(top = barTopYDp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConcaveNavItem(
                        tab = DashboardTab.TIMER,
                        icon = Icons.Default.Timer,
                        label = "Timer",
                        testTag = "nav_tab_timer",
                        selected = currentTab == DashboardTab.TIMER,
                        onClick = { onTabSelected(DashboardTab.TIMER) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                    ConcaveNavItem(
                        tab = DashboardTab.BLOCKLIST,
                        icon = Icons.Default.Block,
                        label = "Blocklist",
                        testTag = "nav_tab_blocklist",
                        selected = currentTab == DashboardTab.BLOCKLIST,
                        onClick = { onTabSelected(DashboardTab.BLOCKLIST) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                    ConcaveNavItem(
                        tab = DashboardTab.STATS,
                        icon = Icons.Default.History,
                        label = "Stats",
                        testTag = "nav_tab_stats",
                        selected = currentTab == DashboardTab.STATS,
                        onClick = { onTabSelected(DashboardTab.STATS) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                    ConcaveNavItem(
                        tab = DashboardTab.SETTINGS,
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        testTag = "nav_tab_settings",
                        selected = currentTab == DashboardTab.SETTINGS,
                        onClick = { onTabSelected(DashboardTab.SETTINGS) },
                        inactiveColor = inactiveIconColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Floating elevated circular button sitting inside the concave scoop
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
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            clip = false
                        )
                        .clip(CircleShape)
                        .background(activeCircleBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 28.dp),
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
                            DashboardTab.STATS -> Icons.Default.History
                            DashboardTab.SETTINGS -> Icons.Default.Settings
                        }
                        Icon(
                            imageVector = activeIcon,
                            contentDescription = tab.name,
                            tint = activeIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConcaveNavItem(
    tab: DashboardTab,
    icon: ImageVector,
    label: String,
    testTag: String,
    selected: Boolean,
    onClick: () -> Unit,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    // Fade out inactive icon & label when this tab is active, because the floating circular badge occupies it
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
                indication = ripple(bounded = true, radius = 32.dp),
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
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                color = inactiveColor,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
