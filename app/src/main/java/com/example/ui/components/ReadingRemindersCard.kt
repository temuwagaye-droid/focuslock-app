package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.notifications.NotificationHelper
import com.example.notifications.NotificationHistoryEntry
import com.example.notifications.NotificationHistoryStatus
import com.example.notifications.ReadingReminderItem
import com.example.notifications.ReminderPreferences
import com.example.notifications.ReminderSoundOption
import com.example.ui.FocusViewModel
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.GamifiedYellow
import com.example.ui.theme.LocalDarkTheme
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
    val isDark = LocalDarkTheme.current

    val reminders by viewModel.readingReminders.collectAsState()
    val prefs by viewModel.reminderPreferences.collectAsState()
    val history by viewModel.notificationHistory.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Reminders, 1 = Prefs & Quiet Hours, 2 = History
    var showEditor by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ReadingReminderItem?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateReminderPreferences(context, prefs.copy(globalEnabled = true))
            Toast.makeText(context, "Reading reminders enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied. Enable in Settings.", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestNotifPermissionIfNeeded(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onGranted()
        }
    }

    val activeCount = remember(reminders) { reminders.count { it.enabled } }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("reading_reminders_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header + Global Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CircularIconBadge(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        containerSize = 34.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "READING REMINDERS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = buildString {
                                append("$activeCount active")
                                if (prefs.quietHoursEnabled) {
                                    append(" • Quiet ${prefs.formattedQuietWindow()}")
                                } else {
                                    append(" • ${prefs.defaultSoundOption.label}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = prefs.globalEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            requestNotifPermissionIfNeeded {
                                viewModel.updateReminderPreferences(context, prefs.copy(globalEnabled = true))
                            }
                        } else {
                            viewModel.updateReminderPreferences(context, prefs.copy(globalEnabled = false))
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GamifiedPurple,
                        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("daily_reminder_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-Navigation Pills inside the card: Reminders | Quiet & Prefs | History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(0, "Reminders (${reminders.size})", Icons.Default.Notifications),
                    Triple(1, "Prefs", Icons.Default.Tune),
                    Triple(2, "History (${history.size})", Icons.Default.History)
                ).forEach { (tabIndex, label, icon) ->
                    val selected = selectedTab == tabIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (selected) GamifiedPurple else Color.Transparent)
                            .clickable { selectedTab = tabIndex }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("reminder_card_tab_$tabIndex")
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Tab 0: Create + Manage Reminders List
                    Button(
                        onClick = {
                            requestNotifPermissionIfNeeded {
                                editingItem = null
                                showEditor = true
                            }
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_reminder_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ADD READING REMINDER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (reminders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "No custom reading reminders yet. Tap above to schedule daily, weekly, monthly, or custom-date reading nudges.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            reminders.forEach { item ->
                                ReminderListRow(
                                    item = item,
                                    snoozeMins = prefs.defaultSnoozeMinutes,
                                    isDark = isDark,
                                    onToggleEnabled = { enabled ->
                                        viewModel.toggleReadingReminder(context, item.id, enabled)
                                    },
                                    onSnooze = {
                                        viewModel.snoozeReadingReminder(context, item.id)
                                        Toast.makeText(context, "Snoozed for ${prefs.defaultSnoozeMinutes}m", Toast.LENGTH_SHORT).show()
                                    },
                                    onComplete = {
                                        viewModel.completeReadingReminder(context, item.id)
                                        Toast.makeText(context, "Marked completed ✓", Toast.LENGTH_SHORT).show()
                                    },
                                    onDismiss = {
                                        viewModel.dismissReadingReminder(context, item.id)
                                        Toast.makeText(context, "Reminder dismissed", Toast.LENGTH_SHORT).show()
                                    },
                                    onEdit = {
                                        editingItem = item
                                        showEditor = true
                                    },
                                    onDelete = {
                                        viewModel.deleteReadingReminder(context, item.id)
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Tab 1: Quiet Hours & Notification Preferences
                    ReminderPreferencesSection(
                        prefs = prefs,
                        onUpdatePrefs = { updated -> viewModel.updateReminderPreferences(context, updated) },
                        onSendTestNotification = {
                            requestNotifPermissionIfNeeded {
                                viewModel.sendTestReminderNotification(context)
                                Toast.makeText(context, "Test notification triggered (or logged if Quiet Hours active)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                else -> {
                    // Tab 2: Notification History
                    ReminderHistorySection(
                        history = history,
                        isDark = isDark,
                        onClearHistory = { viewModel.clearReminderHistory(context) }
                    )
                }
            }
        }
    }

    if (showEditor) {
        ReminderEditorDialog(
            initialReminder = editingItem,
            defaultSoundOption = prefs.defaultSoundOption,
            defaultCustomSoundUri = prefs.defaultCustomSoundUri,
            defaultCustomSoundLabel = prefs.defaultCustomSoundLabel,
            onSave = { title, note, y, m, d, hr, min, repeat, soundOpt, customUri, customLbl ->
                val existing = editingItem
                if (existing == null) {
                    viewModel.createReadingReminder(
                        context = context,
                        title = title,
                        note = note,
                        year = y,
                        month = m,
                        dayOfMonth = d,
                        hour = hr,
                        minute = min,
                        repeatMode = repeat,
                        soundOption = soundOpt,
                        customSoundUri = customUri,
                        customSoundLabel = customLbl
                    )
                } else {
                    viewModel.updateReadingReminder(
                        context,
                        existing.copy(
                            title = title,
                            note = note,
                            year = y,
                            month = m,
                            dayOfMonth = d,
                            hour = hr,
                            minute = min,
                            repeatMode = repeat,
                            soundOption = soundOpt,
                            customSoundUri = customUri,
                            customSoundLabel = customLbl
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
private fun ReminderListRow(
    item: ReadingReminderItem,
    snoozeMins: Int,
    isDark: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSnooze: () -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val purpleBg = if (isDark) Color(0xFF28204B) else Color(0xFFEEEAFF)
    val mintBg = if (isDark) Color(0xFF143326) else Color(0xFFE6FBF1)
    val mintText = if (isDark) GamifiedMint else Color(0xFF0A6B43)
    val yellowBg = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFF5DB)
    val yellowText = if (isDark) GamifiedYellow else Color(0xFF8A5A00)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .testTag("reminder_row_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.note.isNotBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Switch(
                checked = item.enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GamifiedPurple,
                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("reminder_enable_switch_${item.id}")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Schedule & Sound Metadata Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallStatusChip(
                text = "${item.formattedDate()} • ${item.formattedTime()}",
                bgColor = purpleBg,
                textColor = GamifiedPurple
            )
            SmallStatusChip(
                text = item.repeatMode.label,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurface
            )
            SmallStatusChip(
                text = item.effectiveSoundLabel(),
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.secondary
            )
            item.snoozedUntilMs?.takeIf { it > System.currentTimeMillis() }?.let { until ->
                val hhmm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(until))
                SmallStatusChip(
                    text = "Snoozed $hhmm",
                    bgColor = yellowBg,
                    textColor = yellowText
                )
            }
            item.completedAtMs?.let {
                SmallStatusChip(
                    text = "Done ✓",
                    bgColor = mintBg,
                    textColor = mintText
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Snooze / Complete / Dismiss / Edit / Delete Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReminderActionPill(
                icon = Icons.Default.Snooze,
                label = "Snooze ${snoozeMins}m",
                bgColor = yellowBg,
                contentColor = yellowText,
                onClick = onSnooze,
                tag = "snooze_reminder_${item.id}"
            )
            ReminderActionPill(
                icon = Icons.Default.Check,
                label = "Done",
                bgColor = mintBg,
                contentColor = mintText,
                onClick = onComplete,
                tag = "complete_reminder_${item.id}"
            )
            ReminderActionPill(
                icon = Icons.Default.Close,
                label = "Dismiss",
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.secondary,
                onClick = onDismiss,
                tag = "dismiss_reminder_${item.id}"
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("edit_reminder_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${item.title}",
                    tint = GamifiedPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_reminder_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${item.title}",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ReminderPreferencesSection(
    prefs: ReminderPreferences,
    onUpdatePrefs: (ReminderPreferences) -> Unit,
    onSendTestNotification: () -> Unit
) {
    val context = LocalContext.current

    val defaultPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                res.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                res.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val rTitle = runCatching {
                    RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Device Tone"
                onUpdatePrefs(
                    prefs.copy(
                        defaultSoundOption = ReminderSoundOption.CUSTOM,
                        defaultCustomSoundUri = uri.toString(),
                        defaultCustomSoundLabel = rTitle
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quiet Hours Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = GamifiedPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Quiet Hours",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mute notifications during ${prefs.formattedQuietWindow()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Switch(
                checked = prefs.quietHoursEnabled,
                onCheckedChange = { onUpdatePrefs(prefs.copy(quietHoursEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GamifiedPurple
                ),
                modifier = Modifier.testTag("quiet_hours_switch")
            )
        }

        AnimatedVisibility(visible = prefs.quietHoursEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, h, m -> onUpdatePrefs(prefs.copy(quietStartHour = h, quietStartMinute = m)) },
                                prefs.quietStartHour,
                                prefs.quietStartMinute,
                                true
                            ).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "From: ${String.format(Locale.getDefault(), "%02d:%02d", prefs.quietStartHour, prefs.quietStartMinute)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, h, m -> onUpdatePrefs(prefs.copy(quietEndHour = h, quietEndMinute = m)) },
                                prefs.quietEndHour,
                                prefs.quietEndMinute,
                                true
                            ).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "Until: ${String.format(Locale.getDefault(), "%02d:%02d", prefs.quietEndHour, prefs.quietEndMinute)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Vibration & Notification Action Buttons toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Vibration with alerts", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Switch(
                checked = prefs.vibrateEnabled,
                onCheckedChange = { onUpdatePrefs(prefs.copy(vibrateEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GamifiedPurple)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Snooze / Done / Dismiss buttons", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Switch(
                checked = prefs.showActionButtons,
                onCheckedChange = { onUpdatePrefs(prefs.copy(showActionButtons = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GamifiedPurple)
            )
        }

        // Snooze Length Selector
        Text(
            text = "DEFAULT SNOOZE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.secondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(5, 10, 15, 30).forEach { mins ->
                val selected = prefs.defaultSnoozeMinutes == mins
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) GamifiedPurple else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onUpdatePrefs(prefs.copy(defaultSnoozeMinutes = mins)) }
                        .padding(vertical = 7.dp)
                ) {
                    Text(
                        text = "${mins}m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Default Custom Notification Sound
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DEFAULT SOUND",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "▶ Preview",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = GamifiedPurple,
                modifier = Modifier.clickable {
                    NotificationHelper.previewSound(context, prefs.defaultSoundOption, prefs.defaultCustomSoundUri)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReminderSoundOption.entries.forEach { opt ->
                val selected = prefs.defaultSoundOption == opt
                val label = if (opt == ReminderSoundOption.CUSTOM && !prefs.defaultCustomSoundLabel.isNullOrBlank()) {
                    prefs.defaultCustomSoundLabel
                } else {
                    opt.label
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) GamifiedMint else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            if (opt == ReminderSoundOption.CUSTOM) {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                }
                                runCatching { defaultPickerLauncher.launch(intent) }
                            } else {
                                onUpdatePrefs(prefs.copy(defaultSoundOption = opt))
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Button(
            onClick = onSendTestNotification,
            colors = appBlackButtonColors(),
            border = appBlackButtonBorder(),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("test_reminder_notification_btn")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("TEST REMINDER NOTIFICATION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ReminderHistorySection(
    history: List<NotificationHistoryEntry>,
    isDark: Boolean,
    onClearHistory: () -> Unit
) {
    val purpleBg = if (isDark) Color(0xFF28204B) else Color(0xFFEEEAFF)
    val mintBg = if (isDark) Color(0xFF143326) else Color(0xFFE6FBF1)
    val mintText = if (isDark) GamifiedMint else Color(0xFF0A6B43)
    val yellowBg = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFF5DB)
    val yellowText = if (isDark) GamifiedYellow else Color(0xFF8A5A00)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NOTIFICATION LOG (${history.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
            if (history.isNotEmpty()) {
                Text(
                    text = "CLEAR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable(onClick = onClearHistory)
                        .testTag("clear_reminder_history_btn")
                )
            }
        }

        if (history.isEmpty()) {
            Text(
                text = "No reminder notifications recorded yet. Triggered, snoozed, completed, dismissed, and quiet-hours events appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            history.take(15).forEach { entry ->
                val (badgeBg, badgeColor) = when (entry.status) {
                    NotificationHistoryStatus.COMPLETED -> mintBg to mintText
                    NotificationHistoryStatus.SNOOZED -> yellowBg to yellowText
                    NotificationHistoryStatus.TRIGGERED -> purpleBg to GamifiedPurple
                    NotificationHistoryStatus.DISMISSED,
                    NotificationHistoryStatus.QUIET_SUPPRESSED ->
                        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.secondary
                }
                val ts = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(entry.timestampMs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${entry.message} • $ts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    SmallStatusChip(
                        text = entry.status.label,
                        bgColor = badgeBg,
                        textColor = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallStatusChip(
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun ReminderActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = contentColor,
            maxLines = 1
        )
    }
}
