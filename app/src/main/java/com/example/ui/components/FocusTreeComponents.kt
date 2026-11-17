package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FocusStatsSummary
import com.example.ui.theme.LocalDarkTheme

fun calculateTreeStage(totalFocusMinutes: Long, completedSessions: Int): Int {
    val score = totalFocusMinutes + (completedSessions * 15L)
    return when {
        score >= 300 -> 5
        score >= 150 -> 4
        score >= 60 -> 3
        score >= 20 -> 2
        else -> 1
    }
}

fun treeStageTitle(stage: Int): String = when (stage) {
    1 -> "Tiny Seedling"
    2 -> "Green Sprout"
    3 -> "Young Sapling"
    4 -> "Canopy Guardian"
    else -> "Golden Ancient Oak"
}

/**
 * Animated Focus Tree reward visualization.
 * Grows from seedling to golden blooming oak with lifetime progress or live session completion.
 */
@Composable
fun GrowingTreeCanvas(
    stage: Int,
    growthFraction: Float,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "tree_sway")
    val swayOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    val clampedGrowth = growthFraction.coerceIn(0.1f, 1f)
    val effectiveStage = if (growthFraction > 0.75f && stage < 5) stage + 1 else stage

    val soilColor = if (isDark) Color(0xFF4E342E) else Color(0xFF6D4C41)
    val trunkColor = if (isDark) Color(0xFF8D6E63) else Color(0xFF5D4037)
    val leafPrimary = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val leafSecondary = if (isDark) Color(0xFFA5D6A7) else Color(0xFF66BB6A)
    val blossomColor = if (isDark) Color(0xFFFFD54F) else Color(0xFFFFB300)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val baseY = h * 0.88f
        val centerX = w / 2f

        // Soil mound
        drawOval(
            color = soilColor,
            topLeft = Offset(centerX - w * 0.28f, baseY - h * 0.05f),
            size = Size(w * 0.56f, h * 0.13f)
        )

        val trunkHeight = h * (0.24f + 0.09f * effectiveStage) * clampedGrowth
        val trunkTopY = (baseY - trunkHeight).coerceAtLeast(h * 0.22f)
        val topX = centerX + swayOffset * (0.4f + effectiveStage * 0.12f)

        // Trunk
        val trunkPath = Path().apply {
            moveTo(centerX - 6f - effectiveStage * 1.5f, baseY)
            quadraticTo(centerX, (baseY + trunkTopY) / 2f, topX - 3f, trunkTopY)
            lineTo(topX + 3f, trunkTopY)
            quadraticTo(centerX + 4f, (baseY + trunkTopY) / 2f, centerX + 6f + effectiveStage * 1.5f, baseY)
            close()
        }
        drawPath(trunkPath, color = trunkColor)

        if (effectiveStage >= 2) {
            // Left branch
            drawLine(
                color = trunkColor,
                start = Offset((centerX + topX) / 2f, baseY - trunkHeight * 0.52f),
                end = Offset(topX - w * 0.14f, trunkTopY + h * 0.08f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Right branch
            drawLine(
                color = trunkColor,
                start = Offset((centerX + topX) / 2f, baseY - trunkHeight * 0.62f),
                end = Offset(topX + w * 0.14f, trunkTopY + h * 0.06f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Canopy clusters scale with stage & growth
        val baseCanopyRadius = w * (0.09f + effectiveStage * 0.032f) * (0.7f + 0.3f * clampedGrowth)
        drawCircle(
            color = leafPrimary,
            radius = baseCanopyRadius,
            center = Offset(topX, trunkTopY)
        )

        if (effectiveStage >= 2) {
            drawCircle(
                color = leafSecondary.copy(alpha = 0.9f),
                radius = baseCanopyRadius * 0.72f,
                center = Offset(topX - baseCanopyRadius * 0.85f, trunkTopY + baseCanopyRadius * 0.35f)
            )
            drawCircle(
                color = leafSecondary.copy(alpha = 0.9f),
                radius = baseCanopyRadius * 0.72f,
                center = Offset(topX + baseCanopyRadius * 0.85f, trunkTopY + baseCanopyRadius * 0.35f)
            )
        }

        if (effectiveStage >= 4) {
            drawCircle(
                color = leafPrimary.copy(alpha = 0.95f),
                radius = baseCanopyRadius * 0.68f,
                center = Offset(topX, trunkTopY - baseCanopyRadius * 0.65f)
            )
        }

        // Blossoms / Golden fruits on higher stages or completed progress
        if (effectiveStage >= 3 || clampedGrowth > 0.8f) {
            val dots = listOf(
                Offset(topX - baseCanopyRadius * 0.45f, trunkTopY - baseCanopyRadius * 0.25f),
                Offset(topX + baseCanopyRadius * 0.5f, trunkTopY + baseCanopyRadius * 0.1f),
                Offset(topX + baseCanopyRadius * 0.1f, trunkTopY - baseCanopyRadius * 0.65f)
            )
            dots.forEach { pt ->
                drawCircle(color = blossomColor, radius = 4.dp.toPx(), center = pt)
            }
        }

        // Subtle sparkle ring for Stage 5
        if (effectiveStage == 5) {
            drawCircle(
                color = blossomColor.copy(alpha = 0.35f),
                radius = baseCanopyRadius * 1.45f,
                center = Offset(topX, trunkTopY),
                style = Stroke(width = 1.5.dp.toPx())
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
    val isDark = LocalDarkTheme.current
    val stage = calculateTreeStage(stats.totalFocusMinutes, stats.completedSessionsCount)
    val stageName = treeStageTitle(stage)
    val cardBg = if (isDark) Color(0xFF132619) else Color(0xFFE8F5E9)
    val cardBorder = if (isDark) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFF81C784)
    val accentText = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)

    val xpScore = stats.totalFocusMinutes + (stats.completedSessionsCount * 15L)
    val nextMilestone = when (stage) {
        1 -> 20L
        2 -> 60L
        3 -> 150L
        4 -> 300L
        else -> 500L
    }
    val growthFraction = liveSessionProgress ?: (xpScore.toFloat() / nextMilestone.toFloat()).coerceIn(0.15f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("virtual_focus_tree_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(94.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .border(1.dp, cardBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                GrowingTreeCanvas(
                    stage = stage,
                    growthFraction = growthFraction,
                    modifier = Modifier.size(86.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (liveSessionProgress != null) "GROWING NOW" else "VIRTUAL FOCUS TREE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accentText,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Stage $stage/5",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentText
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "🌳 $stageName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (liveSessionProgress != null) {
                        "Your tree grows while you stay locked into reading!"
                    } else {
                        "${stats.completedSessionsCount} Trees Grown • ${stats.totalFocusMinutes}m Rooted"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { growthFraction.coerceIn(0f, 1f) },
                    color = accentText,
                    trackColor = cardBorder.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun GamifiedLevelHudCard(
    stats: FocusStatsSummary,
    modifier: Modifier = Modifier
) {
    val level = (stats.focusScore / 200) + 1
    val levelProgress = ((stats.focusScore % 200) / 200f).coerceIn(0.04f, 1f)
    val pointsToNext = 200 - (stats.focusScore % 200)
    val rank = readerRankTitle(level)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Reader Level",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LVL $level • ${rank.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${stats.focusScore} XP Total",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                XpRewardChip(text = "$pointsToNext XP to Lvl ${level + 1}")
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { levelProgress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

