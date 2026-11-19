package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FocusStatsSummary
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.GamifiedYellow
import com.example.ui.theme.LocalDarkTheme

fun calculateTreeStage(totalMinutes: Long, completedSessions: Int): Int = when {
    totalMinutes >= 300 || completedSessions >= 20 -> 5
    totalMinutes >= 150 || completedSessions >= 10 -> 4
    totalMinutes >= 60 || completedSessions >= 4 -> 3
    totalMinutes >= 15 || completedSessions >= 1 -> 2
    else -> 1
}

fun treeStageTitle(stage: Int): String = when (stage.coerceIn(1, 5)) {
    1 -> "Sprouting Seed"
    2 -> "Young Sapling"
    3 -> "Steady Oak"
    4 -> "Blossom Canopy"
    else -> "Ancient Wisdom Tree"
}

fun readerRankTitle(level: Int): String = when {
    level >= 15 -> "Grand Archivist"
    level >= 10 -> "Focus Sage"
    level >= 6 -> "Deep Scholar"
    level >= 3 -> "Page Turner"
    else -> "Novice Reader"
}

@Composable
fun XpRewardChip(
    text: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val bg = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFF5DB)
    val fg = if (isDark) GamifiedYellow else Color(0xFF8A5A00)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun GrowingTreeCanvas(
    stage: Int,
    liveProgress: Float = 1f,
    modifier: Modifier = Modifier.size(64.dp)
) {
    val clampedStage = stage.coerceIn(1, 5)
    val scale = (0.55f + liveProgress.coerceIn(0f, 1f) * 0.45f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.88f

        // Ground mound
        drawLine(
            color = Color(0xFF6D4C41),
            start = Offset(w * 0.25f, groundY),
            end = Offset(w * 0.75f, groundY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Trunk
        val trunkHeight = h * (0.18f + clampedStage * 0.08f) * scale
        drawLine(
            color = Color(0xFF795548),
            start = Offset(w * 0.5f, groundY),
            end = Offset(w * 0.5f, groundY - trunkHeight),
            strokeWidth = (3 + clampedStage).dp.toPx(),
            cap = StrokeCap.Round
        )

        val topY = groundY - trunkHeight
        val leafRadius = (w * (0.12f + clampedStage * 0.045f)) * scale
        drawCircle(
            color = GamifiedMint,
            radius = leafRadius,
            center = Offset(w * 0.5f, topY)
        )
        if (clampedStage >= 3) {
            drawCircle(
                color = Color(0xFF28B779),
                radius = leafRadius * 0.72f,
                center = Offset(w * 0.35f, topY + leafRadius * 0.25f)
            )
            drawCircle(
                color = Color(0xFF28B779),
                radius = leafRadius * 0.72f,
                center = Offset(w * 0.65f, topY + leafRadius * 0.25f)
            )
        }
        if (clampedStage >= 4) {
            drawCircle(
                color = GamifiedYellow,
                radius = 3.dp.toPx(),
                center = Offset(w * 0.44f, topY - leafRadius * 0.35f)
            )
            drawCircle(
                color = GamifiedPurple,
                radius = 3.dp.toPx(),
                center = Offset(w * 0.58f, topY - leafRadius * 0.2f)
            )
        }
    }
}

@Composable
fun FocusForestRewardCard(
    stats: FocusStatsSummary,
    liveSessionProgress: Float? = null,
    modifier: Modifier = Modifier
) {
    val stage = calculateTreeStage(stats.totalFocusMinutes, stats.completedSessionsCount)
    val title = treeStageTitle(stage)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("focus_forest_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                GrowingTreeCanvas(
                    stage = stage,
                    liveProgress = liveSessionProgress ?: 1f,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "FOCUS TREE • STAGE $stage/5",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GamifiedMint
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (liveSessionProgress != null) {
                            "Growing live with your current focus session"
                        } else {
                            "${stats.totalFocusMinutes}m total focus • ${stats.completedSessionsCount} sessions"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            XpRewardChip(text = "${stats.focusScore} XP")
        }
    }
}

@Composable
fun GamifiedLevelHudCard(
    stats: FocusStatsSummary,
    modifier: Modifier = Modifier
) {
    val level = (stats.focusScore / 200) + 1
    val xpInLevel = stats.focusScore % 200
    val rank = readerRankTitle(level)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LEVEL $level • $rank",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${200 - xpInLevel} XP to Level ${level + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                XpRewardChip(text = "${stats.focusScore} XP")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((xpInLevel / 200f).coerceIn(0.05f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GamifiedPurple)
                )
            }
        }
    }
}
