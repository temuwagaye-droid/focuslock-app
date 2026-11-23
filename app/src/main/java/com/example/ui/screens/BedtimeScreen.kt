package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientSoundPlayer
import com.example.audio.AmbientSoundType
import com.example.bedtime.BedtimeManager
import com.example.bedtime.BedtimeReceiver
import com.example.bedtime.BedtimeSchedule
import com.example.ui.components.AppSwitch
import com.example.ui.components.CircularIconBadge
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

@Composable
fun BedtimeScreen(
    onBack: () -> Unit,
    onOpenEmergencyAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val schedule by BedtimeManager.schedule.collectAsState()
    val isLockActive by BedtimeManager.isBedtimeLockActive.collectAsState()
    val stats by BedtimeManager.stats.collectAsState()
    val emergencyActive by BedtimeManager.emergencyUnlockActive.collectAsState()

    var showEarlyExitConfirm by remember { mutableStateOf(false) }
    var showEmergencyConfirm by remember { mutableStateOf(false) }

    var secondsCounter by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            secondsCounter += 1f
        }
    }

    val nextBedtimeSeconds = remember(secondsCounter, schedule) {
        BedtimeManager.getTimeUntilNextBedtimeSeconds()
    }
    val wakeUpSeconds = remember(secondsCounter, schedule) {
        BedtimeManager.getTimeUntilWakeUpSeconds()
    }

    val nightBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF060913),
            Color(0xFF0B1120),
            Color(0xFF0F172A)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(nightBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bedtime Lock",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Nighttime rest & digital curfew",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLockActive) Color(0xFF9070FF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                    .border(1.dp, if (isLockActive) Color(0xFF9070FF) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (emergencyActive) "EMERGENCY 5M" else if (isLockActive) "ACTIVE" else "SCHEDULED",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = if (emergencyActive) Color(0xFFFFB74D) else if (isLockActive) Color(0xFFC084FC) else Color(0xFF94A3B8)
                )
            }
        }

        // Nighttime Status Card with Moon & Stars Visual
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF818CF8).copy(alpha = 0.35f), Color(0xFF1E1B4B))
                            )
                        )
                        .border(1.5.dp, Color(0xFF818CF8).copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Moon",
                        tint = Color(0xFFDDD6FE),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isLockActive) "Good Night • Rest Mode Active" else "Ready to Wind Down",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedCountdown = if (isLockActive) {
                    val h = wakeUpSeconds / 3600L
                    val m = (wakeUpSeconds % 3600L) / 60L
                    val s = wakeUpSeconds % 60L
                    String.format(Locale.getDefault(), "Wake up in %02dh %02dm %02ds", h, m, s)
                } else {
                    val h = nextBedtimeSeconds / 3600L
                    val m = (nextBedtimeSeconds % 3600L) / 60L
                    val s = nextBedtimeSeconds % 60L
                    String.format(Locale.getDefault(), "Bedtime in %02dh %02dm %02ds", h, m, s)
                }

                Text(
                    text = formattedCountdown,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Bedtime: ${formatTime(schedule.bedtimeHour, schedule.bedtimeMinute)}  •  Wake-up: ${formatTime(schedule.wakeUpHour, schedule.wakeUpMinute)}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Schedule Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ENABLE BEDTIME LOCK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF818CF8)
                        )
                        Text(
                            text = "Automatically restrict distracting apps nightly",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    AppSwitch(
                        checked = schedule.isEnabled,
                        onCheckedChange = { checked ->
                            val updated = schedule.copy(isEnabled = checked)
                            BedtimeManager.updateSchedule(context, updated)
                            BedtimeReceiver.scheduleBedtimeAlarms(context)
                        }
                    )
                }

                // Bedtime Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "BEDTIME (NIGHT)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = formatTime(schedule.bedtimeHour, schedule.bedtimeMinute),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFA78BFA)
                        )
                    }
                    Slider(
                        value = schedule.bedtimeHour.toFloat(),
                        onValueChange = { hour ->
                            val updated = schedule.copy(bedtimeHour = hour.toInt())
                            BedtimeManager.updateSchedule(context, updated)
                            BedtimeReceiver.scheduleBedtimeAlarms(context)
                        },
                        valueRange = 18f..24f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFA78BFA),
                            activeTrackColor = Color(0xFF818CF8),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }

                // Wake-up Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "WAKE-UP TIME (MORNING)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = formatTime(schedule.wakeUpHour, schedule.wakeUpMinute),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Slider(
                        value = schedule.wakeUpHour.toFloat(),
                        onValueChange = { hour ->
                            val updated = schedule.copy(wakeUpHour = hour.toInt())
                            BedtimeManager.updateSchedule(context, updated)
                            BedtimeReceiver.scheduleBedtimeAlarms(context)
                        },
                        valueRange = 4f..10f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF0284C7),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }

                // Active Days Chips
                Column {
                    Text(
                        text = "ACTIVE DAYS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val days = listOf(
                        Calendar.MONDAY to "M",
                        Calendar.TUESDAY to "T",
                        Calendar.WEDNESDAY to "W",
                        Calendar.THURSDAY to "T",
                        Calendar.FRIDAY to "F",
                        Calendar.SATURDAY to "S",
                        Calendar.SUNDAY to "S"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { (dayInt, label) ->
                            val isSelected = schedule.activeDays.contains(dayInt)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B))
                                    .clickable {
                                        val newDays = if (isSelected) {
                                            if (schedule.activeDays.size > 1) schedule.activeDays - dayInt else schedule.activeDays
                                        } else {
                                            schedule.activeDays + dayInt
                                        }
                                        val updated = schedule.copy(activeDays = newDays)
                                        BedtimeManager.updateSchedule(context, updated)
                                        BedtimeReceiver.scheduleBedtimeAlarms(context)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // Advance Reminder Selection
                Column {
                    Text(
                        text = "ADVANCE REMINDER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60).forEach { mins ->
                            val isSel = schedule.advanceReminderMinutes == mins
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    val updated = schedule.copy(advanceReminderMinutes = mins)
                                    BedtimeManager.updateSchedule(context, updated)
                                },
                                label = { Text("${mins}m before") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4F46E5),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Night Relaxation & Guided Breathing
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NIGHTTIME BREATHING GUIDE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF38BDF8)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Smooth Gentle Breathing Animation (Inhale / Hold / Exhale)
                val transition = rememberInfiniteTransition(label = "breath")
                val breathScale by transition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.22f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(breathScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF0C4A6E).copy(alpha = 0.15f))
                            )
                        )
                        .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (breathScale > 1.05f) "Inhale" else "Exhale",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Breathe with the circle: 4s gentle inhale, 4s slow exhale to calm your nervous system.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Sleep Consistency Streak & Stats Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.currentStreakDays} DAYS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24)
                    )
                    Text(
                        text = "Sleep Streak",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.weeklyAdherencePercent}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF34D399)
                    )
                    Text(
                        text = "Weekly Adherence",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.earlyExitCount}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF87171)
                    )
                    Text(
                        text = "Early Exits",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Emergency & Early-Exit Controls
        if (isLockActive) {
            Button(
                onClick = { showEmergencyConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C2D12)),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("5-MINUTE EMERGENCY OVERRIDE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { showEarlyExitConfirm = true },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("EARLY EXIT BEDTIME LOCK", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Emergency Override Dialog
    if (showEmergencyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmergencyConfirm = false },
            title = { Text("Emergency 5-Minute Access", fontWeight = FontWeight.Bold) },
            text = { Text("Temporary 5-minute unlock for urgent needs or calls. Distraction shield will automatically re-engage after 5 minutes.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyConfirm = false
                        BedtimeManager.triggerEmergencyOverride(context, 5)
                        onOpenEmergencyAccess()
                    }
                ) {
                    Text("Unlock 5 Minutes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEmergencyConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Early Exit Confirmation Dialog
    if (showEarlyExitConfirm) {
        AlertDialog(
            onDismissRequest = { showEarlyExitConfirm = false },
            title = { Text("Exit Bedtime Lock Early?", fontWeight = FontWeight.Bold) },
            text = { Text("Exiting early will mark this night's sleep schedule as interrupted. Are you sure you want to end tonight's bedtime lock?") },
            confirmButton = {
                Button(
                    onClick = {
                        showEarlyExitConfirm = false
                        BedtimeManager.earlyExitBedtimeLock(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Yes, Exit Early", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEarlyExitConfirm = false }) {
                    Text("Stay in Bedtime")
                }
            }
        )
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0 || hour == 24) 12 else if (hour > 12) hour - 12 else hour
    val ampm = if (hour >= 12 && hour < 24) "PM" else "AM"
    return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, ampm)
}
