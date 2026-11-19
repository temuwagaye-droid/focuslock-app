package com.example.ui.components

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.notifications.NotificationHelper
import com.example.notifications.ReadingReminderItem
import com.example.notifications.ReminderRepeatMode
import com.example.notifications.ReminderSoundOption
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorDialog(
    existingReminder: ReadingReminderItem?,
    defaultSoundOption: ReminderSoundOption,
    defaultCustomUri: String?,
    defaultCustomLabel: String?,
    onSave: (
        title: String,
        note: String,
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        repeatMode: ReminderRepeatMode,
        soundOption: ReminderSoundOption,
        customUri: String?,
        customLabel: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val nowCal = remember { Calendar.getInstance() }

    var title by remember { mutableStateOf(existingReminder?.title ?: "Daily Reading Lock") }
    var note by remember { mutableStateOf(existingReminder?.note ?: "") }
    var year by remember { mutableIntStateOf(existingReminder?.year ?: nowCal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(existingReminder?.month ?: (nowCal.get(Calendar.MONTH) + 1)) }
    var dayOfMonth by remember { mutableIntStateOf(existingReminder?.dayOfMonth ?: nowCal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(existingReminder?.hour ?: 20) }
    var minute by remember { mutableIntStateOf(existingReminder?.minute ?: 0) }
    var repeatMode by remember { mutableStateOf(existingReminder?.repeatMode ?: ReminderRepeatMode.DAILY) }
    var soundOption by remember { mutableStateOf(existingReminder?.soundOption ?: defaultSoundOption) }
    var customUri by remember { mutableStateOf(existingReminder?.customSoundUri ?: defaultCustomUri) }
    var customLabel by remember { mutableStateOf(existingReminder?.customSoundLabel ?: defaultCustomLabel) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                soundOption = ReminderSoundOption.CUSTOM
                customUri = pickedUri.toString()
                val ringtoneTitle = runCatching {
                    RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                }.getOrNull()
                customLabel = ringtoneTitle?.takeIf { it.isNotBlank() } ?: "Device Ringtone"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            NotificationHelper.stopSoundPreview()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reminder_editor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularIconBadge(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        containerSize = 38.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (existingReminder == null) "New Reading Reminder" else "Edit Reading Reminder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_note_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "DATE & TIME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, mZero, d ->
                                    year = y
                                    month = mZero + 1
                                    dayOfMonth = d
                                },
                                year,
                                (month - 1).coerceIn(0, 11),
                                dayOfMonth.coerceIn(1, 31)
                            ).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_reminder_date_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick date",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, dayOfMonth),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    hour = h
                                    minute = m
                                },
                                hour,
                                minute,
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_reminder_time_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Pick time",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "REPETITION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReminderRepeatMode.entries.forEach { mode ->
                        FilterChip(
                            selected = repeatMode == mode,
                            onClick = { repeatMode = mode },
                            label = { Text(mode.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("repeat_mode_${mode.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOTIFICATION SOUND",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    IconButton(
                        onClick = { NotificationHelper.previewSound(context, soundOption, customUri) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview Notification Sound",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReminderSoundOption.entries.forEach { option ->
                        val chipLabel = if (option == ReminderSoundOption.CUSTOM && !customLabel.isNullOrBlank()) {
                            customLabel!!
                        } else {
                            option.label
                        }
                        FilterChip(
                            selected = soundOption == option,
                            onClick = {
                                if (option == ReminderSoundOption.CUSTOM) {
                                    val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                        customUri?.let {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                        }
                                    }
                                    runCatching { ringtonePickerLauncher.launch(pickerIntent) }
                                    soundOption = ReminderSoundOption.CUSTOM
                                } else {
                                    soundOption = option
                                }
                            },
                            leadingIcon = if (option == ReminderSoundOption.CUSTOM) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            label = { Text(chipLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("sound_option_${option.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onSave(
                                title,
                                note,
                                year,
                                month,
                                dayOfMonth,
                                hour,
                                minute,
                                repeatMode,
                                soundOption,
                                customUri,
                                customLabel
                            )
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_reminder_btn")
                    ) {
                        Text("SAVE REMINDER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
