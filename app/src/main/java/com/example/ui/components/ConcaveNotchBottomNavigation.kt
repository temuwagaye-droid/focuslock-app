package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DashboardTab
import com.example.ui.theme.LocalDarkTheme
import kotlin.math.roundToInt

private data class NavTabMeta(
    val tab: DashboardTab,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

private val navTabs = listOf(
    NavTabMeta(DashboardTab.TIMER, "Timer", Icons.Filled.Timer, Icons.Outlined.Timer),
    NavTabMeta(DashboardTab.BLOCKLIST, "Apps", Icons.Filled.Security, Icons.Outlined.Security),
    NavTabMeta(DashboardTab.SOCIAL, "Rooms", Icons.Filled.Groups, Icons.Outlined.Groups),
    NavTabMeta(DashboardTab.STATS, "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    NavTabMeta(DashboardTab.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun ConcaveNotchBottomNavigationBar(
    selectedTab: DashboardTab,
    onSelectTab: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIndex = navTabs.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "notch_tab_index"
    )

    val isDark = LocalDarkTheme.current
    val barSurfaceColor = MaterialTheme.colorScheme.surface
    val barBorderColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pillBg = Color.Black
    val pillIconTint = Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(78.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val barTopOffsetDp = 14.dp
            val tabWidthPx = widthPx / navTabs.size
            val centerX = (animatedIndex + 0.5f) * tabWidthPx

            // Concave notched bar background
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
            ) {
                val w = size.width
                val h = size.height
                val notchHalfWidth = 34.dp.toPx()
                val notchDepth = 26.dp.toPx()
                val shoulder = 14.dp.toPx()
                val leftStart = (centerX - notchHalfWidth - shoulder).coerceAtLeast(0f)
                val rightEnd = (centerX + notchHalfWidth + shoulder).coerceAtMost(w)

                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(leftStart, 0f)
                    cubicTo(
                        leftStart + shoulder * 0.9f,
                        0f,
                        centerX - notchHalfWidth * 0.68f,
                        notchDepth,
                        centerX,
                        notchDepth
                    )
                    cubicTo(
                        centerX + notchHalfWidth * 0.68f,
                        notchDepth,
                        rightEnd - shoulder * 0.9f,
                        0f,
                        rightEnd,
                        0f
                    )
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = path, color = barSurfaceColor)
                drawPath(
                    path = path,
                    color = barBorderColor.copy(alpha = 0.85f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Floating active indicator circle
            val circleSize = 46.dp
            val circleSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { circleSize.toPx() }
            val activeMeta = navTabs[activeIndex]
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (centerX - circleSizePx / 2f).roundToInt(),
                            y = 0
                        )
                    }
                    .size(circleSize)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(
                        width = if (isDark) 1.5.dp else 2.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.55f) else primaryColor.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = activeMeta.activeIcon,
                    contentDescription = null,
                    tint = pillIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Interactive tab items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navTabs.forEach { item ->
                    val isSelected = item.tab == selectedTab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelectTab(item.tab) }
                            )
                            .padding(bottom = 6.dp)
                            .testTag("bottom_nav_${item.tab.name.lowercase()}")
                    ) {
                        if (!isSelected) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .then(
                                        if (isDark) {
                                            Modifier.border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                Icon(
                                    imageVector = item.inactiveIcon,
                                    contentDescription = item.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) primaryColor else inactiveLabelColor,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(barTopOffsetDp))
        }
    }
}
