package com.example.ui.components

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.notifications.NotificationHelper
import com.example.notifications.ReadingReminderItem
import com.example.notifications.ReminderRepeatMode
import com.example.notifications.ReminderSoundOption
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import java.util.Calendar
import java.util.Locale

@Composable
fun ReminderEditorDialog(
    initialReminder: ReadingReminderItem?,
    defaultSoundOption: ReminderSoundOption,
    defaultCustomSoundUri: String?,
    defaultCustomSoundLabel: String?,
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
        customSoundUri: String?,
        customSoundLabel: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val nowCal = remember { Calendar.getInstance() }

    var title by remember { mutableStateOf(initialReminder?.title ?: "Daily Reading Lock") }
    var note by remember { mutableStateOf(initialReminder?.note ?: "") }
    var year by remember { mutableIntStateOf(initialReminder?.year ?: nowCal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(initialReminder?.month ?: (nowCal.get(Calendar.MONTH) + 1)) }
    var dayOfMonth by remember { mutableIntStateOf(initialReminder?.dayOfMonth ?: nowCal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(initialReminder?.hour ?: 20) }
    var minute by remember { mutableIntStateOf(initialReminder?.minute ?: 0) }
    var repeatMode by remember { mutableStateOf(initialReminder?.repeatMode ?: ReminderRepeatMode.DAILY) }
    var soundOption by remember { mutableStateOf(initialReminder?.soundOption ?: defaultSoundOption) }
    var customSoundUri by remember { mutableStateOf(initialReminder?.customSoundUri ?: defaultCustomSoundUri) }
    var customSoundLabel by remember { mutableStateOf(initialReminder?.customSoundLabel ?: defaultCustomSoundLabel) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
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
                soundOption = ReminderSoundOption.CUSTOM
                customSoundUri = uri.toString()
                customSoundLabel = rTitle
            } else {
                soundOption = ReminderSoundOption.SILENT
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { NotificationHelper.stopSoundPreview() }
    }

    val formattedDate = remember(year, month, dayOfMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            val maxD = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxD))
        }
        val mLabel = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "M$month"
        "$mLabel ${cal.get(Calendar.DAY_OF_MONTH)}, $year"
    }
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = null,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reminder_editor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialReminder == null) "New Reading Reminder" else "Edit Reading Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(48) },
                    label = { Text("Reminder Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GamifiedPurple,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(90) },
                    label = { Text("Goal / Note (optional)") },
                    placeholder = { Text("e.g., 25m chapter before bed") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GamifiedPurple,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_note_input")
                )

                // Custom Date & Custom Time Pickers
                Text(
                    text = "DATE & TIME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, mZero, d ->
                                        year = y
                                        month = mZero + 1
                                        dayOfMonth = d
                                    },
                                    year,
                                    (month - 1).coerceIn(0, 11),
                                    dayOfMonth
                                ).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .testTag("reminder_date_picker_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Choose Date",
                            tint = GamifiedPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(0.85f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        hour = h
                                        minute = min
                                    },
                                    hour,
                                    minute,
                                    true
                                ).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .testTag("reminder_time_picker_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Choose Time",
                            tint = GamifiedPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedTime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Repetition Frequency (Once / Daily / Weekly / Monthly)
                Text(
                    text = "REPETITION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReminderRepeatMode.entries.forEach { mode ->
                        val selected = repeatMode == mode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) GamifiedPurple else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { repeatMode = mode }
                                .padding(vertical = 9.dp)
                                .testTag("repeat_mode_${mode.id}")
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Custom Notification Sound Selector + Preview
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val played = NotificationHelper.previewSound(context, soundOption, customSoundUri)
                                if (!played && soundOption == ReminderSoundOption.SILENT) {
                                    Toast.makeText(context, "Silent mode selected", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview Sound",
                            tint = GamifiedPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Preview",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = GamifiedPurple
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderSoundOption.entries.forEach { opt ->
                        val selected = soundOption == opt
                        val chipText = if (opt == ReminderSoundOption.CUSTOM && !customSoundLabel.isNullOrBlank()) {
                            customSoundLabel!!
                        } else {
                            opt.label
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) GamifiedMint else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (opt == ReminderSoundOption.CUSTOM) {
                                        val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                            customSoundUri?.let {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                            }
                                        }
                                        runCatching { ringtonePickerLauncher.launch(pickerIntent) }
                                            .onFailure {
                                                soundOption = ReminderSoundOption.CUSTOM
                                                Toast.makeText(context, "Using system notification tone", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        soundOption = opt
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("sound_option_${opt.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (selected) Color.Black else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = chipText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_reminder_editor_btn")
                    ) {
                        Text("CANCEL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
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
                                customSoundUri,
                                customSoundLabel
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
                        Text("SAVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
