package com.example.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AppSwitch
import com.example.notifications.NotificationHistoryStatus
import com.example.notifications.ReadingReminderItem
import com.example.notifications.ReminderSoundOption
import com.example.ui.FocusViewModel
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedYellow
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReadingRemindersCard(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reminders by viewModel.readingReminders.collectAsState()
    val prefs by viewModel.reminderPreferences.collectAsState()
    val history by viewModel.notificationHistory.collectAsState()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0=Reminders, 1=Prefs, 2=History
    var showEditor by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ReadingReminderItem?>(null) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notification permission denied. Reminders may stay silent.", Toast.LENGTH_SHORT).show()
        }
    }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val timeFmt = remember { SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("reading_reminders_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CircularIconBadge(
                        imageVector = if (prefs.globalEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        containerSize = 32.dp,
                        iconSize = 17.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "READING REMINDERS",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${reminders.count { it.enabled }} active • Quiet ${if (prefs.quietHoursEnabled) prefs.formattedQuietWindow() else "Off"}",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Button(
                    onClick = {
                        ensureNotificationPermission()
                        editingItem = null
                        showEditor = true
                    },
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("add_reminder_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Reminder",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NEW", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Tab Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    "Reminders (${reminders.size})",
                    "Preferences",
                    "History (${history.size})"
                )
                tabs.forEachIndexed { index, label ->
                    val active = selectedSubTab == index
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { selectedSubTab = index }
                            .testTag("reminder_tab_$index")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedSubTab) {
                0 -> {
                    if (reminders.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No custom reading reminders yet.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap NEW to schedule daily, weekly, monthly, or one-time reminders with custom sounds.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            reminders.forEach { item ->
                                ReminderRowCard(
                                    item = item,
                                    onToggle = { enabled ->
                                        if (enabled) ensureNotificationPermission()
                                        viewModel.toggleReadingReminder(context, item.id, enabled)
                                    },
                                    onEdit = {
                                        editingItem = item
                                        showEditor = true
                                    },
                                    onDelete = {
                                        viewModel.deleteReadingReminder(context, item.id)
                                    },
                                    onSnooze = {
                                        viewModel.snoozeReadingReminder(context, item.id)
                                        Toast.makeText(context, "Snoozed ${prefs.defaultSnoozeMinutes}m", Toast.LENGTH_SHORT).show()
                                    },
                                    onComplete = {
                                        viewModel.completeReadingReminder(context, item.id)
                                        Toast.makeText(context, "Marked completed", Toast.LENGTH_SHORT).show()
                                    },
                                    onDismissReminder = {
                                        viewModel.dismissReadingReminder(context, item.id)
                                        Toast.makeText(context, "Reminder dismissed", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PreferenceSwitchRow(
                                title = "Enable All Reading Notifications",
                                checked = prefs.globalEnabled,
                                onCheckedChange = {
                                    if (it) ensureNotificationPermission()
                                    viewModel.updateReminderPreferences(context, prefs.copy(globalEnabled = it))
                                }
                            )
                            PreferenceSwitchRow(
                                title = "Vibration Enabled",
                                checked = prefs.vibrateEnabled,
                                onCheckedChange = {
                                    viewModel.updateReminderPreferences(context, prefs.copy(vibrateEnabled = it))
                                }
                            )
                            PreferenceSwitchRow(
                                title = "Quick Snooze / Complete Buttons",
                                checked = prefs.showActionButtons,
                                onCheckedChange = {
                                    viewModel.updateReminderPreferences(context, prefs.copy(showActionButtons = it))
                                }
                            )
                            PreferenceSwitchRow(
                                title = "Quiet Hours (Pause Alerts)",
                                checked = prefs.quietHoursEnabled,
                                onCheckedChange = {
                                    viewModel.updateReminderPreferences(context, prefs.copy(quietHoursEnabled = it))
                                }
                            )

                            if (prefs.quietHoursEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    viewModel.updateReminderPreferences(
                                                        context,
                                                        prefs.copy(quietStartHour = h, quietStartMinute = m)
                                                    )
                                                },
                                                prefs.quietStartHour,
                                                prefs.quietStartMinute,
                                                true
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "Start %02d:%02d", prefs.quietStartHour, prefs.quietStartMinute),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    viewModel.updateReminderPreferences(
                                                        context,
                                                        prefs.copy(quietEndHour = h, quietEndMinute = m)
                                                    )
                                                },
                                                prefs.quietEndHour,
                                                prefs.quietEndMinute,
                                                true
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "End %02d:%02d", prefs.quietEndHour, prefs.quietEndMinute),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "DEFAULT SNOOZE DURATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5, 10, 15, 30).forEach { mins ->
                                    FilterChip(
                                        selected = prefs.defaultSnoozeMinutes == mins,
                                        onClick = {
                                            viewModel.updateReminderPreferences(
                                                context,
                                                prefs.copy(defaultSnoozeMinutes = mins)
                                            )
                                        },
                                        label = { Text("${mins}m", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            Text(
                                text = "DEFAULT NOTIFICATION SOUND",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    ReminderSoundOption.DEFAULT,
                                    ReminderSoundOption.ALARM,
                                    ReminderSoundOption.BELL,
                                    ReminderSoundOption.SILENT
                                ).forEach { opt ->
                                    FilterChip(
                                        selected = prefs.defaultSoundOption == opt,
                                        onClick = {
                                            viewModel.updateReminderPreferences(
                                                context,
                                                prefs.copy(defaultSoundOption = opt)
                                            )
                                        },
                                        label = { Text(opt.label.split(" ").first(), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    ensureNotificationPermission()
                                    viewModel.sendTestReminderNotification(context)
                                    Toast.makeText(context, "Test reminder notification dispatched", Toast.LENGTH_SHORT).show()
                                },
                                colors = appBlackButtonColors(),
                                border = appBlackButtonBorder(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("test_reminder_notification_btn")
                            ) {
                                Text("SEND TEST NOTIFICATION", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }

                2 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECENT ALERTS (${history.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (history.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearReminderHistory(context) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (history.isEmpty()) {
                                Text(
                                    text = "No reminder history yet. Trigger, snooze, complete, or dismiss a reminder to see activity here.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    history.take(20).forEach { entry ->
                                        val statusColor = when (entry.status) {
                                            NotificationHistoryStatus.COMPLETED -> GamifiedMint
                                            NotificationHistoryStatus.SNOOZED -> GamifiedYellow
                                            NotificationHistoryStatus.QUIET_SUPPRESSED -> MaterialTheme.colorScheme.secondary
                                            NotificationHistoryStatus.DISMISSED -> MaterialTheme.colorScheme.error
                                            NotificationHistoryStatus.TRIGGERED -> MaterialTheme.colorScheme.primary
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${entry.message} • ${timeFmt.format(Date(entry.timestampMs))}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            Text(
                                                text = entry.status.label.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = statusColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ReminderEditorDialog(
            existingReminder = editingItem,
            defaultSoundOption = prefs.defaultSoundOption,
            defaultCustomUri = prefs.defaultCustomSoundUri,
            defaultCustomLabel = prefs.defaultCustomSoundLabel,
            onSave = { title, note, year, month, day, hour, minute, repeat, sound, cUri, cLabel ->
                val existing = editingItem
                if (existing == null) {
                    viewModel.createReadingReminder(
                        context = context,
                        title = title,
                        note = note,
                        year = year,
                        month = month,
                        dayOfMonth = day,
                        hour = hour,
                        minute = minute,
                        repeatMode = repeat,
                        soundOption = sound,
                        customSoundUri = cUri,
                        customSoundLabel = cLabel
                    )
                } else {
                    viewModel.updateReadingReminder(
                        context = context,
                        item = existing.copy(
                            title = title.trim().ifBlank { existing.title },
                            note = note.trim(),
                            year = year,
                            month = month,
                            dayOfMonth = day,
                            hour = hour,
                            minute = minute,
                            repeatMode = repeat,
                            soundOption = sound,
                            customSoundUri = cUri,
                            customSoundLabel = cLabel
                        )
                    )
                }
                showEditor = false
                editingItem = null
            },
            onDismiss = {
                showEditor = false
                editingItem = null
            }
        )
    }
}

@Composable
private fun ReminderRowCard(
    item: ReadingReminderItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: () -> Unit,
    onComplete: () -> Unit,
    onDismissReminder: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (item.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_row_${item.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.formattedDate()} • ${item.formattedTime()} • ${item.repeatMode.label} • ${item.effectiveSoundLabel()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (item.note.isNotBlank()) {
                        Text(
                            text = item.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.snoozedUntilMs != null && item.snoozedUntilMs > System.currentTimeMillis()) {
                        val snoozeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.snoozedUntilMs))
                        Text(
                            text = "Snoozed until $snoozeStr",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                AppSwitch(
                    checked = item.enabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onComplete, modifier = Modifier.size(48.dp)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark Completed",
                            containerSize = 34.dp,
                            iconSize = 17.dp
                        )
                    }
                    IconButton(onClick = onSnooze, modifier = Modifier.size(48.dp)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Snooze Reminder",
                            containerSize = 34.dp,
                            iconSize = 17.dp
                        )
                    }
                    IconButton(onClick = onDismissReminder, modifier = Modifier.size(48.dp)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Reminder",
                            containerSize = 34.dp,
                            iconSize = 17.dp
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Reminder",
                            containerSize = 34.dp,
                            iconSize = 17.dp
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Reminder",
                            containerSize = 34.dp,
                            iconSize = 17.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
