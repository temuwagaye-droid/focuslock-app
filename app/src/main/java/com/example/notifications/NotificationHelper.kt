package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Calendar
import kotlin.math.abs

object NotificationHelper {

    const val REMINDER_CHANNEL_ID = "readlock_daily_reminders"
    const val REMINDER_CHANNEL_ALARM_ID = "focuslock_reminders_alarm"
    const val REMINDER_CHANNEL_BELL_ID = "focuslock_reminders_bell"
    const val REMINDER_CHANNEL_SILENT_ID = "focuslock_reminders_silent"
    const val ALERT_CHANNEL_ID = "readlock_focus_alerts"

    const val ACTION_SNOOZE = "com.example.notifications.ACTION_SNOOZE"
    const val ACTION_COMPLETE = "com.example.notifications.ACTION_COMPLETE"
    const val ACTION_DISMISS = "com.example.notifications.ACTION_DISMISS"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    private var activePreviewRingtone: Ringtone? = null

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Reading Reminders (Chime)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reading reminders with standard FocusLock notification chime"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttrs)
            }

            val alarmChannel = NotificationChannel(
                REMINDER_CHANNEL_ALARM_ID,
                "Reading Reminders (Gentle Wake)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reading reminders with alarm tone"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    audioAttrs
                )
            }

            val bellChannel = NotificationChannel(
                REMINDER_CHANNEL_BELL_ID,
                "Reading Reminders (Soft Bell)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reading reminders with soft ringtone bell"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    audioAttrs
                )
            }

            val silentChannel = NotificationChannel(
                REMINDER_CHANNEL_SILENT_ID,
                "Reading Reminders (Silent)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent visual-only reading reminders"
                enableVibration(false)
                setSound(null, null)
            }

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Focus & Accountability Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Session completion rewards and accountability partner alerts"
            }

            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(alarmChannel)
            manager.createNotificationChannel(bellChannel)
            manager.createNotificationChannel(silentChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    fun resolveSoundUri(option: ReminderSoundOption, customSoundUri: String?): Uri? {
        return when (option) {
            ReminderSoundOption.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ReminderSoundOption.ALARM -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ReminderSoundOption.BELL -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ReminderSoundOption.SILENT -> null
            ReminderSoundOption.CUSTOM -> customSoundUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun previewSound(context: Context, option: ReminderSoundOption, customSoundUri: String?): Boolean {
        stopSoundPreview()
        val uri = resolveSoundUri(option, customSoundUri) ?: return false
        return runCatching {
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return false
            activePreviewRingtone = ringtone
            ringtone.play()
            true
        }.getOrDefault(false)
    }

    fun stopSoundPreview() {
        runCatching {
            activePreviewRingtone?.stop()
        }
        activePreviewRingtone = null
    }

    private fun resolveCustomChannelId(
        context: Context,
        option: ReminderSoundOption,
        customSoundUri: String?,
        vibrate: Boolean
    ): String {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return REMINDER_CHANNEL_ID

        if (option == ReminderSoundOption.CUSTOM && !customSoundUri.isNullOrBlank()) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val hash = abs("${customSoundUri}_$vibrate".hashCode())
            val dynamicId = "focuslock_custom_sound_$hash"
            if (manager != null && manager.getNotificationChannel(dynamicId) == null) {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val channel = NotificationChannel(
                    dynamicId,
                    "Reading Reminders (Custom Tone)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Custom reading reminder notification sound"
                    enableVibration(vibrate)
                    setSound(Uri.parse(customSoundUri), audioAttrs)
                }
                manager.createNotificationChannel(channel)
            }
            return dynamicId
        }

        return when (option) {
            ReminderSoundOption.DEFAULT -> REMINDER_CHANNEL_ID
            ReminderSoundOption.ALARM -> REMINDER_CHANNEL_ALARM_ID
            ReminderSoundOption.BELL -> REMINDER_CHANNEL_BELL_ID
            ReminderSoundOption.SILENT -> REMINDER_CHANNEL_SILENT_ID
            ReminderSoundOption.CUSTOM -> REMINDER_CHANNEL_ID
        }
    }

    fun notificationIdForReminder(reminderId: String): Int =
        4000 + (abs(reminderId.hashCode()) % 50000)

    fun showCustomReminderNotification(
        context: Context,
        reminder: ReadingReminderItem,
        prefs: ReminderPreferences,
        bypassQuietHours: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!bypassQuietHours && prefs.isWithinQuietHours(now)) {
            ReadingReminderStore.appendHistory(
                context = context,
                reminderId = reminder.id,
                title = reminder.title,
                message = "Suppressed during quiet hours (${prefs.formattedQuietWindow()})",
                status = NotificationHistoryStatus.QUIET_SUPPRESSED
            )
            return
        }

        val effectiveSoundOption = if (reminder.soundOption == ReminderSoundOption.DEFAULT &&
            prefs.defaultSoundOption != ReminderSoundOption.DEFAULT
        ) {
            prefs.defaultSoundOption
        } else {
            reminder.soundOption
        }
        val effectiveCustomUri = reminder.customSoundUri ?: prefs.defaultCustomSoundUri

        val channelId = resolveCustomChannelId(
            context = context,
            option = effectiveSoundOption,
            customSoundUri = effectiveCustomUri,
            vibrate = prefs.vibrateEnabled
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = notificationIdForReminder(reminder.id)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("quick_action", "start_focus")
        }
        val openPi = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bodyText = reminder.note.takeIf { it.isNotBlank() }
            ?: "Time to open your reading app and lock in (${reminder.repeatMode.label})."

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("${reminder.title} 📚")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .setPriority(
                if (effectiveSoundOption == ReminderSoundOption.SILENT) {
                    NotificationCompat.PRIORITY_LOW
                } else {
                    NotificationCompat.PRIORITY_HIGH
                }
            )

        if (effectiveSoundOption == ReminderSoundOption.SILENT) {
            builder.setSilent(true)
        } else {
            resolveSoundUri(effectiveSoundOption, effectiveCustomUri)?.let { soundUri ->
                builder.setSound(soundUri)
            }
        }
        if (prefs.vibrateEnabled && effectiveSoundOption != ReminderSoundOption.SILENT) {
            builder.setVibrate(longArrayOf(0L, 220L, 120L, 220L))
        } else {
            builder.setVibrate(longArrayOf(0L))
        }

        if (prefs.showActionButtons) {
            val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            }
            val snoozePi = PendingIntent.getBroadcast(
                context,
                notifId + 1,
                snoozeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            }
            val completePi = PendingIntent.getBroadcast(
                context,
                notifId + 2,
                completeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val dismissIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            }
            val dismissPi = PendingIntent.getBroadcast(
                context,
                notifId + 3,
                dismissIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            builder.addAction(0, "Snooze ${prefs.defaultSnoozeMinutes}m", snoozePi)
            builder.addAction(0, "Complete", completePi)
            builder.addAction(0, "Dismiss", dismissPi)
        }

        try {
            manager.notify(notifId, builder.build())
        } catch (_: SecurityException) {
        }

        ReadingReminderStore.appendHistory(
            context = context,
            reminderId = reminder.id,
            title = reminder.title,
            message = "Fired at ${reminder.formattedTime()} (${reminder.effectiveSoundLabel()})",
            status = NotificationHistoryStatus.TRIGGERED
        )
    }

    fun showSessionCompletedNotification(context: Context, durationMinutes: Int, treeStage: Int) {
        ensureChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Focus Session Complete! 🌳")
            .setContentText("Awesome ${durationMinutes}m reading session! Your Focus Tree is now Stage $treeStage.")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            manager.notify(2001, notif)
        } catch (_: SecurityException) {
        }
    }

    fun showAccountabilityQuitNotification(
        context: Context,
        partnerName: String,
        elapsedMinutes: Int,
        blockedAttempts: Int
    ) {
        ensureChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val label = partnerName.ifBlank { "your accountability partner" }
        val notif = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Early Quit Logged — Accountability Alert")
            .setContentText("Ended after ${elapsedMinutes}m ($blockedAttempts blocked). Notify $label to stay accountable.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            manager.notify(2002, notif)
        } catch (_: SecurityException) {
        }
    }

    fun showDailyReminderNotification(context: Context) {
        ReadingReminderStore.ensureLoaded(context)
        val prefs = ReadingReminderStore.preferences.value
        val now = System.currentTimeMillis()
        if (prefs.isWithinQuietHours(now)) {
            ReadingReminderStore.appendHistory(
                context = context,
                reminderId = "daily_legacy",
                title = "Daily Reading Reminder",
                message = "Suppressed during quiet hours (${prefs.formattedQuietWindow()})",
                status = NotificationHistoryStatus.QUIET_SUPPRESSED
            )
            return
        }

        val channelId = resolveCustomChannelId(
            context = context,
            option = prefs.defaultSoundOption,
            customSoundUri = prefs.defaultCustomSoundUri,
            vibrate = prefs.vibrateEnabled
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("quick_action", "start_focus")
        }
        val pi = PendingIntent.getActivity(
            context,
            2003,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Time to Read & Lock In 📚")
            .setContentText("Start your FocusLock reading session now and grow your focus tree.")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            manager.notify(2003, notif)
        } catch (_: SecurityException) {
        }
        ReadingReminderStore.appendHistory(
            context = context,
            reminderId = "daily_legacy",
            title = "Daily Reading Reminder",
            message = "Delivered daily reading nudge",
            status = NotificationHistoryStatus.TRIGGERED
        )
    }

    fun buildAccountabilityShareIntent(
        partnerEmail: String,
        partnerName: String,
        elapsedMinutes: Int,
        blockedAttempts: Int
    ): Intent {
        val greeting = if (partnerName.isNotBlank()) "Hi $partnerName," else "Hi,"
        val body = "$greeting\n\nFocusLock Accountability Update: I ended my reading focus session early after $elapsedMinutes minute(s) (with $blockedAttempts distraction attempt(s) blocked).\n\nSending this update to stay honest with my focus goals!"
        return if (partnerEmail.isNotBlank()) {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(partnerEmail))
                putExtra(Intent.EXTRA_SUBJECT, "FocusLock Accountability Alert: Session Ended Early")
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "FocusLock Accountability Update")
                putExtra(Intent.EXTRA_TEXT, body)
            }
        }
    }
}

object ReminderScheduler {
    private const val REQUEST_CODE = 3005

    fun scheduleDailyReminder(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun computeNextTriggerTimeMillis(
        reminder: ReadingReminderItem,
        fromMs: Long = System.currentTimeMillis()
    ): Long? {
        if (!reminder.enabled) return null
        val snoozed = reminder.snoozedUntilMs
        if (snoozed != null && snoozed > fromMs) {
            return snoozed
        }
        val cal = baseCalendar(reminder)
        if (reminder.repeatMode == ReminderRepeatMode.ONCE) {
            return if (cal.timeInMillis > fromMs) cal.timeInMillis else null
        }
        if (cal.timeInMillis > fromMs) {
            return cal.timeInMillis
        }
        val advanced = advanceCalendar(reminder, fromMs)
        return advanced.timeInMillis
    }

    fun advanceCalendar(reminder: ReadingReminderItem, fromMs: Long): Calendar {
        val cal = baseCalendar(reminder)
        when (reminder.repeatMode) {
            ReminderRepeatMode.ONCE -> Unit
            ReminderRepeatMode.DAILY -> {
                while (cal.timeInMillis <= fromMs) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            ReminderRepeatMode.WEEKLY -> {
                while (cal.timeInMillis <= fromMs) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            ReminderRepeatMode.MONTHLY -> {
                val targetDay = reminder.dayOfMonth.coerceIn(1, 31)
                while (cal.timeInMillis <= fromMs) {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.add(Calendar.MONTH, 1)
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDay))
                }
            }
        }
        return cal
    }

    private fun baseCalendar(reminder: ReadingReminderItem): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, reminder.year)
            set(Calendar.MONTH, (reminder.month - 1).coerceIn(0, 11))
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, reminder.dayOfMonth.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, reminder.hour.coerceIn(0, 23))
            set(Calendar.MINUTE, reminder.minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun syncAllAlarms(context: Context) {
        ReadingReminderStore.ensureLoaded(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = ReadingReminderStore.preferences.value
        val items = ReadingReminderStore.reminders.value
        val now = System.currentTimeMillis()

        for (item in items) {
            val pi = pendingIntentForReminder(context, item.id)
            if (!prefs.globalEnabled || !item.enabled) {
                alarmManager.cancel(pi)
                continue
            }
            val triggerAt = computeNextTriggerTimeMillis(item, now)
            if (triggerAt == null) {
                alarmManager.cancel(pi)
                continue
            }
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pi
            )
        }
    }

    fun cancelReminderAlarm(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(pendingIntentForReminder(context, reminderId))
    }

    private fun pendingIntentForReminder(context: Context, reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
        }
        val reqCode = 10_000 + (abs(reminderId.hashCode()) % 50_000)
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReadingReminderStore.ensureLoaded(context)
        val prefs = ReadingReminderStore.preferences.value
        val reminderId = intent?.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID)

        if (reminderId.isNullOrBlank()) {
            NotificationHelper.showDailyReminderNotification(context)
            return
        }

        val item = ReadingReminderStore.getReminderById(context, reminderId) ?: return
        if (!prefs.globalEnabled || !item.enabled) return

        NotificationHelper.showCustomReminderNotification(context, item, prefs)

        // Advance next trigger date for repeating reminders, or turn off ONCE reminders after firing
        val updated = if (item.repeatMode == ReminderRepeatMode.ONCE) {
            item.copy(enabled = false, snoozedUntilMs = null)
        } else {
            val nextCal = ReminderScheduler.advanceCalendar(item, System.currentTimeMillis() + 1000L)
            item.copy(
                year = nextCal.get(Calendar.YEAR),
                month = nextCal.get(Calendar.MONTH) + 1,
                dayOfMonth = nextCal.get(Calendar.DAY_OF_MONTH),
                snoozedUntilMs = null
            )
        }
        ReadingReminderStore.updateReminder(context, updated)
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReadingReminderStore.ensureLoaded(context)
        val reminderId = intent?.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        if (notifId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(notifId)
        }

        when (intent.action) {
            NotificationHelper.ACTION_SNOOZE -> {
                ReadingReminderStore.snoozeReminder(context, reminderId)
            }
            NotificationHelper.ACTION_COMPLETE -> {
                ReadingReminderStore.markReminderCompleted(context, reminderId)
            }
            NotificationHelper.ACTION_DISMISS -> {
                ReadingReminderStore.dismissReminder(context, reminderId)
            }
        }
    }
}
