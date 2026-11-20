package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.data.model.SessionLogEntity
import com.example.data.remote.SupabaseUser
import com.example.ui.FocusStatsSummary
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.GamifiedYellow
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import java.util.Calendar
import java.util.TimeZone

/**
 * Duolingo-style compact HUD header with animated streak flame, blocked shields pill, and XP bar.
 * Borderless rounded card with soft elevation shadow.
 */
@Composable
fun TimerXpAndStreakCard(
    stats: FocusStatsSummary,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val level = (stats.focusScore / 200) + 1
    val xpInLevel = stats.focusScore % 200
    val xpProgress by animateFloatAsState(
        targetValue = (xpInLevel / 200f).coerceIn(0.05f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "xp_bar_progress"
    )
    val pointsToNext = 200 - xpInLevel
    val rank = readerRankTitle(level)

    val infiniteTransition = rememberInfiniteTransition(label = "flame_pulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    val flameChipBg = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFF5DB)
    val flameChipContent = if (isDark) GamifiedYellow else Color(0xFF8A5A00)
    val shieldChipBg = if (isDark) Color(0xFF143326) else Color(0xFFE3FAEF)
    val shieldChipContent = if (isDark) GamifiedMint else Color(0xFF0A6B43)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenStats)
            .testTag("timer_xp_streak_card")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Text(
                            text = "L$level",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = rank,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${stats.focusScore} XP • $pointsToNext to next",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Streak Flame pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(flameChipBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("timer_streak_flame_pill")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak Flame",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(13.dp)
                                    .scale(flameScale)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.currentStreak}d",
                            color = flameChipContent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Shielded Attempts pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(shieldChipBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("timer_blocked_attempts_pill")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Distractions Blocked",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.totalBlockedAttempts}",
                            color = shieldChipContent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smooth Duolingo-style XP bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(xpProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GamifiedPurple)
                )
            }
        }
    }
}

/**
 * Interactive Apple-Fitness-style rotatable minute dial with real-time touch drag rotation,
 * minute ticks, and smooth spring sweep.
 */
@Composable
fun FitnessDurationRing(
    sessionMinutes: Int,
    onMinutesChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    val currentOnMinutesChange by rememberUpdatedState(onMinutesChange)

    val targetFraction = (sessionMinutes / 120f).coerceIn(5f / 120f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (isDragging) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "duration_ring_sweep"
    )
    val fraction = if (isDragging) targetFraction else animatedFraction

    val trackColor = MaterialTheme.colorScheme.surface
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val isDark = LocalDarkTheme.current
    val ringColor = if (isDark) Color(0xFF8B73FF) else GamifiedPurple

    fun updateFromPoint(offset: Offset, width: Float, height: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val dx = offset.x - cx
        val dy = offset.y - cy
        // Angle clockwise from 12 o'clock (0..2*PI)
        var theta = atan2(dx.toDouble(), -dy.toDouble())
        if (theta < 0.0) theta += 2.0 * PI
        val rawMins = ((theta / (2.0 * PI)) * 120.0).roundToInt().coerceIn(5, 120)
        currentOnMinutesChange?.invoke(rawMins)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(134.dp)
            .then(
                if (onMinutesChange != null) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { pos ->
                                updateFromPoint(pos, size.width.toFloat(), size.height.toFloat())
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { pos ->
                                    isDragging = true
                                    updateFromPoint(pos, size.width.toFloat(), size.height.toFloat())
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    updateFromPoint(change.position, size.width.toFloat(), size.height.toFloat())
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            )
                        }
                } else {
                    Modifier
                }
            )
            .testTag("duration_rotatable_dial")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = 10.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer minute dial ticks (24 ticks = 5m intervals)
            for (i in 0 until 24) {
                val angleRad = (-90.0 + i * 15.0) * (PI / 180.0)
                val isMajor = i % 3 == 0
                val innerR = radius + stroke * 0.42f
                val outerR = innerR + if (isMajor) 5.dp.toPx() else 2.5.dp.toPx()
                drawLine(
                    color = if (isMajor) ringColor.copy(alpha = 0.55f) else tickColor.copy(alpha = 0.55f),
                    start = Offset(
                        center.x + (innerR * cos(angleRad)).toFloat(),
                        center.y + (innerR * sin(angleRad)).toFloat()
                    ),
                    end = Offset(
                        center.x + (outerR * cos(angleRad)).toFloat(),
                        center.y + (outerR * sin(angleRad)).toFloat()
                    ),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Rotating dial knob at tip of progress arc
            val tipAngleRad = (-90.0 + 360.0 * fraction) * (PI / 180.0)
            val tipCenter = Offset(
                center.x + (radius * cos(tipAngleRad)).toFloat(),
                center.y + (radius * sin(tipAngleRad)).toFloat()
            )
            drawCircle(
                color = Color.White,
                radius = stroke * 0.62f,
                center = tipCenter
            )
            drawCircle(
                color = ringColor,
                radius = stroke * 0.36f,
                center = tipCenter
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$sessionMinutes",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.testTag("session_minutes_display")
            )
            Text(
                text = if (onMinutesChange != null) "min • rotate" else "min",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 60fps real-time rotating focus countdown ring with continuous orbital second/minute sweep
 * while the session timer is running.
 */
@Composable
fun RealTimeFocusCountdownRing(
    timeLeftSeconds: Int,
    totalDurationSeconds: Int,
    formattedTime: String,
    potentialSessionXp: Int,
    modifier: Modifier = Modifier
) {
    val primaryTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val accentActiveColor = MaterialTheme.colorScheme.primary

    var frameFraction by remember { mutableFloatStateOf(0f) }
    var lastSecondsSnapshot by remember { mutableStateOf(timeLeftSeconds) }
    var lastTickNano by remember { mutableStateOf(System.nanoTime()) }

    LaunchedEffect(timeLeftSeconds) {
        lastSecondsSnapshot = timeLeftSeconds
        lastTickNano = System.nanoTime()
    }

    LaunchedEffect(totalDurationSeconds) {
        while (true) {
            withFrameNanos { now ->
                val elapsedSinceTick = ((now - lastTickNano) / 1_000_000_000f).coerceIn(0f, 0.999f)
                val smoothRemaining = (lastSecondsSnapshot - elapsedSinceTick).coerceAtLeast(0f)
                frameFraction = if (totalDurationSeconds > 0) {
                    (smoothRemaining / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }
            }
        }
    }

    // Continuous 60-second real-time orbital rotation angle
    val minuteSweepAngle = remember(timeLeftSeconds, frameFraction, totalDurationSeconds) {
        val exactRemainingSeconds = if (totalDurationSeconds > 0) frameFraction * totalDurationSeconds else timeLeftSeconds.toFloat()
        val subMinuteSeconds = exactRemainingSeconds % 60f
        -90f + ((60f - subMinuteSeconds) / 60f) * 360f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(242.dp)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = primaryTrackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accentActiveColor,
                startAngle = -90f,
                sweepAngle = 360f * frameFraction,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Real-time rotating orbital indicator moving every frame around the minute ring
            val orbitRad = minuteSweepAngle * (PI / 180.0)
            val orbitCenter = Offset(
                center.x + (radius * cos(orbitRad)).toFloat(),
                center.y + (radius * sin(orbitRad)).toFloat()
            )
            drawCircle(
                color = GamifiedMint.copy(alpha = 0.35f),
                radius = stroke * 0.95f,
                center = orbitCenter
            )
            drawCircle(
                color = GamifiedMint,
                radius = stroke * 0.48f,
                center = orbitCenter
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "STAY FOCUSED",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedTime,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.testTag("countdown_timer_text")
            )
            Spacer(modifier = Modifier.height(6.dp))
            XpRewardChip(text = "+$potentialSessionXp XP")
        }
    }
}

/**
 * Apple Fitness-inspired Daily Quests with three concentric/side-by-side activity rings.
 */
@Composable
fun DailyQuestsRingsCard(
    sessionLogs: List<SessionLogEntity>,
    stats: FocusStatsSummary,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayMetrics = remember(sessionLogs) {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val todayLogs = sessionLogs.filter { it.timestamp >= startOfDay && it.isCompleted }
        val minsToday = (todayLogs.sumOf { it.durationSeconds } / 60L).toInt()
        val sessionsToday = todayLogs.size
        val pureToday = todayLogs.count { it.blockedAttempts == 0 }
        Triple(minsToday, sessionsToday, pureToday)
    }

    val (minsToday, sessionsToday, pureToday) = todayMetrics
    val isDark = LocalDarkTheme.current
    val yellowRewardBg = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFF5DB)
    val yellowRewardText = if (isDark) GamifiedYellow else Color(0xFF8A5A00)

    val questsDone = listOf(
        minsToday >= 25,
        sessionsToday >= 1,
        pureToday >= 1 || stats.currentStreak >= 3
    ).count { it }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenStats)
            .testTag("daily_quests_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY QUESTS",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (questsDone == 3) GamifiedMint.copy(alpha = 0.2f) else yellowRewardBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Icon(
                            imageVector = if (questsDone == 3) Icons.Default.Check else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$questsDone/3 DONE",
                        color = if (questsDone == 3) GamifiedMint else yellowRewardText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuestProgressRingItem(
                    title = "Daily Read",
                    subtitle = "${minsToday.coerceAtMost(25)}/25m",
                    progress = minsToday / 25f,
                    ringColor = GamifiedPurple,
                    modifier = Modifier.weight(1f)
                )
                QuestProgressRingItem(
                    title = "1 Session",
                    subtitle = "${sessionsToday.coerceAtMost(1)}/1 done",
                    progress = sessionsToday.toFloat(),
                    ringColor = GamifiedYellow,
                    modifier = Modifier.weight(1f)
                )
                QuestProgressRingItem(
                    title = "Pure Focus",
                    subtitle = if (pureToday >= 1) "1/1 clear" else "${stats.currentStreak.coerceAtMost(3)}/3d streak",
                    progress = if (pureToday >= 1) 1f else stats.currentStreak / 3f,
                    ringColor = GamifiedMint,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuestProgressRingItem(
    title: String,
    subtitle: String,
    progress: Float,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val completed = clamped >= 1f
    val activeColor = if (completed) GamifiedMint else ringColor
    val animatedSweep by animateFloatAsState(
        targetValue = clamped.coerceAtLeast(0.06f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "quest_ring_$title"
    )
    val trackColor = MaterialTheme.colorScheme.surface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                val stroke = 5.dp.toPx()
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedSweep,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            if (completed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "$title completed",
                    tint = GamifiedMint,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "${(clamped * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

/**
 * Compact minimal Badges strip celebrating unlocked milestones in Duolingo style.
 */
@Composable
fun TimerBadgesStripCard(
    stats: FocusStatsSummary,
    sessionLogs: List<SessionLogEntity>,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val badges = remember(stats, sessionLogs) {
        listOf(
            Triple("First Lock", "🎯", stats.completedSessionsCount >= 1),
            Triple("3d Streak", "🔥", stats.currentStreak >= 3),
            Triple("100m Club", "🧠", stats.totalFocusMinutes >= 100),
            Triple(
                "Pure Shield",
                "🛡️",
                stats.completedSessionsCount > 0 && sessionLogs.any { it.isCompleted && it.blockedAttempts == 0 }
            )
        )
    }
    val unlockedCount = badges.count { it.third }
    val unlockedBg = if (isDark) Color(0xFF143326) else Color(0xFFE6FBF1)
    val lockedBg = MaterialTheme.colorScheme.surface

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenStats)
            .testTag("timer_badges_strip_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BADGES",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$unlockedCount/${badges.size}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEach { (name, emoji, unlocked) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (unlocked) unlockedBg else lockedBg)
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = name,
                            color = if (unlocked) {
                                if (isDark) GamifiedMint else Color(0xFF0A6B43)
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            fontSize = 9.sp,
                            fontWeight = if (unlocked) FontWeight.Black else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern gamified Duolingo x Apple Fitness side-by-side Loadout cards for Reader & Shield.
 * Medium borderless rounded cards with soft shadow, status chips, and animated progress bars.
 */
@Composable
fun ReaderAndShieldLoadoutRow(
    hasReadingApp: Boolean,
    designatedReadingApp: String?,
    selectedReadingAppLabel: String,
    hasBlockedAppsConfig: Boolean,
    isProTier: Boolean,
    isWhitelistMode: Boolean,
    blockedAppsCount: Int,
    onReaderClick: () -> Unit,
    onShieldClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val readyBg = if (isDark) Color(0xFF143326) else Color(0xFFE6FBF1)
    val readyText = if (isDark) GamifiedMint else Color(0xFF0A6B43)
    val setupBg = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFF5DB)
    val setupText = if (isDark) GamifiedYellow else Color(0xFF8A5A00)
    val purpleTintBg = if (isDark) Color(0xFF28204B) else Color(0xFFEEEAFF)

    val readerMeter by animateFloatAsState(
        targetValue = if (hasReadingApp) 1f else 0.18f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "reader_loadout_meter"
    )
    val shieldFraction = when {
        isProTier && isWhitelistMode -> 1f
        blockedAppsCount > 0 -> (blockedAppsCount / 5f).coerceIn(0.35f, 1f)
        else -> 0.18f
    }
    val shieldMeter by animateFloatAsState(
        targetValue = shieldFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "shield_loadout_meter"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Reader Selection Gamified Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(22.dp),
            border = null,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onReaderClick)
                .testTag("open_reading_app_picker_row")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 132.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        val pkg = designatedReadingApp
                        if (!pkg.isNullOrBlank()) {
                            AppLauncherIcon(
                                packageName = pkg,
                                contentDescription = selectedReadingAppLabel,
                                size = 24.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Select Reading App",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (hasReadingApp) readyBg else setupBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (hasReadingApp) "READY" else "PICK",
                            color = if (hasReadingApp) readyText else setupText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(
                        text = "READER",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hasReadingApp) selectedReadingAppLabel else "Choose App",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("selected_reading_app_label")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(readerMeter)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (hasReadingApp) GamifiedPurple else GamifiedYellow)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Shield Selection Gamified Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(22.dp),
            border = null,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .testTag("blocked_apps_status_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 132.dp)
                    .clickable(onClick = onShieldClick)
                    .padding(14.dp)
                    .testTag("configure_blocked_apps_row"),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "App Shield",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (hasBlockedAppsConfig) readyBg else setupBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (hasBlockedAppsConfig) "ARMED" else "SETUP",
                            color = if (hasBlockedAppsConfig) readyText else setupText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(
                        text = "SHIELD",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            isProTier && isWhitelistMode -> "Whitelist"
                            blockedAppsCount > 0 -> "$blockedAppsCount Blocked"
                            else -> "Block Apps"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(shieldMeter)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (hasBlockedAppsConfig) GamifiedMint else GamifiedYellow)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean minimal Account & Profile modal reachable from the top bar beside the theme toggle.
 */
@Composable
fun AccountProfileDialog(
    currentUser: SupabaseUser?,
    stats: FocusStatsSummary,
    isAuthBusy: Boolean,
    statusMessage: String?,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminDashboard: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val mintBg = if (isDark) Color(0xFF143326) else Color(0xFFE6FBF1)
    val mintText = if (isDark) GamifiedMint else Color(0xFF0A6B43)
    val level = (stats.focusScore / 200) + 1
    val rank = readerRankTitle(level)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(26.dp),
            border = null,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_profile_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentUser == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Account Sign-In",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Your Focus Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Sign in with Google to save your XP, streaks & join study rooms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )

                    statusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GoogleSignInButton(
                        onClick = onSignInWithGoogle,
                        isLoading = isAuthBusy,
                        modifier = Modifier.testTag("dialog_google_sign_in_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onOpenSettings,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ACCOUNT SETTINGS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onOpenAdminDashboard,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dialog_admin_dashboard_button")
                    ) {
                        Text("ADMIN DASHBOARD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    val initial = (currentUser.displayName ?: currentUser.email ?: "R")
                        .trim()
                        .firstOrNull()
                        ?.uppercaseChar()
                        ?: 'R'

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Text(
                            text = initial.toString(),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(mintBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "GOOGLE CONNECTED",
                            color = mintText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.7.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Reader",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    currentUser.email?.let { mail ->
                        Text(
                            text = mail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LVL $level • $rank",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "🔥 ${stats.currentStreak}d streak • ${stats.totalFocusMinutes}m focus",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        XpRewardChip(text = "${stats.focusScore} XP")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenSettings,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SETTINGS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = onSignOut,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dialog_sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIGN OUT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onOpenAdminDashboard,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dialog_admin_dashboard_button")
                    ) {
                        Text("ADMIN DASHBOARD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

