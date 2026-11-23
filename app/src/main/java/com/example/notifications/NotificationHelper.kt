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
import com.example.R
import java.util.Calendar
import kotlin.math.min

object NotificationHelper {
    const val REMINDER_CHANNEL_ID = "readlock_daily_reminders"
    const val REMINDER_CHANNEL_ALARM_ID = "focuslock_reminders_alarm"
    const val REMINDER_CHANNEL_BELL_ID = "focuslock_reminders_bell"
    const val REMINDER_CHANNEL_SILENT_ID = "focuslock_reminders_silent"
    const val ALERT_CHANNEL_ID = "readlock_focus_alerts"
    const val BEDTIME_CHANNEL_ID = "focuslock_bedtime_channel"

    const val ACTION_SNOOZE = "com.example.notifications.ACTION_SNOOZE"
    const val ACTION_COMPLETE = "com.example.notifications.ACTION_COMPLETE"
    const val ACTION_DISMISS = "com.example.notifications.ACTION_DISMISS"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    @Volatile
    private var activePreviewRingtone: Ringtone? = null

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channels = listOf(
            NotificationChannel(REMINDER_CHANNEL_ID, "Reading Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Scheduled reading habit reminders"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttrs)
            },
            NotificationChannel(REMINDER_CHANNEL_ALARM_ID, "Reading Reminders (Alarm)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Gentle alarm style reading reminders"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttrs)
            },
            NotificationChannel(REMINDER_CHANNEL_BELL_ID, "Reading Reminders (Bell)", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Soft bell reading reminders"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), audioAttrs)
            },
            NotificationChannel(REMINDER_CHANNEL_SILENT_ID, "Reading Reminders (Silent)", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Silent reading reminders"
                setSound(null, null)
            },
            NotificationChannel(ALERT_CHANNEL_ID, "Focus Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Session completion and accountability alerts"
            },
            NotificationChannel(BEDTIME_CHANNEL_ID, "Bedtime Lock Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Bedtime lock reminders, activation, and morning wake-up alerts"
            }
        )
        nm.createNotificationChannels(channels)
    }

    fun resolveSoundUri(soundOption: ReminderSoundOption, customSoundUri: String?): Uri? {
        return when (soundOption) {
            ReminderSoundOption.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ReminderSoundOption.ALARM -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ReminderSoundOption.BELL -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ReminderSoundOption.SILENT -> null
            ReminderSoundOption.CUSTOM -> customSoundUri?.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun previewSound(context: Context, soundOption: ReminderSoundOption, customSoundUri: String?): Boolean {
        stopSoundPreview()
        if (soundOption == ReminderSoundOption.SILENT) return false
        val uri = resolveSoundUri(soundOption, customSoundUri) ?: return false
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

    fun notificationIdForReminder(reminderId: String): Int =
        2000 + (reminderId.hashCode() and 0x0FFF)

    fun showCustomReminderNotification(
        context: Context,
        reminder: ReadingReminderItem,
        prefs: ReminderPreferences,
        bypassQuietHours: Boolean = false
    ) {
        ensureChannels(context)
        if (!prefs.globalEnabled) return
        if (!bypassQuietHours && prefs.isWithinQuietHours()) {
            ReadingReminderStore.appendHistory(
                context = context,
                reminderId = reminder.id,
                title = reminder.title,
                message = "Suppressed during Quiet Hours (${prefs.formattedQuietWindow()})",
                status = NotificationHistoryStatus.QUIET_SUPPRESSED
            )
            return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notifId = notificationIdForReminder(reminder.id)
        val channelId = when (reminder.soundOption) {
            ReminderSoundOption.ALARM -> REMINDER_CHANNEL_ALARM_ID
            ReminderSoundOption.BELL -> REMINDER_CHANNEL_BELL_ID
            ReminderSoundOption.SILENT -> REMINDER_CHANNEL_SILENT_ID
            else -> REMINDER_CHANNEL_ID
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(reminder.title)
            .setContentText(reminder.note.ifBlank { "Time for your reading session (${reminder.formattedTime()})" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)

        resolveSoundUri(reminder.soundOption, reminder.customSoundUri)?.let { uri ->
            builder.setSound(uri)
        }
        if (prefs.vibrateEnabled && reminder.soundOption != ReminderSoundOption.SILENT) {
            builder.setVibrate(longArrayOf(0, 220, 120, 220))
        }

        if (prefs.showActionButtons) {
            fun actionPi(action: String, codeOffset: Int): PendingIntent {
                val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                    this.action = action
                    putExtra(EXTRA_REMINDER_ID, reminder.id)
                    putExtra(EXTRA_NOTIFICATION_ID, notifId)
                }
                return PendingIntent.getBroadcast(
                    context,
                    notifId * 10 + codeOffset,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            builder.addAction(0, "Snooze ${prefs.defaultSnoozeMinutes}m", actionPi(ACTION_SNOOZE, 1))
            builder.addAction(0, "Complete", actionPi(ACTION_COMPLETE, 2))
            builder.addAction(0, "Dismiss", actionPi(ACTION_DISMISS, 3))
        }

        runCatching { nm.notify(notifId, builder.build()) }
        ReadingReminderStore.appendHistory(
            context = context,
            reminderId = reminder.id,
            title = reminder.title,
            message = "Triggered at ${reminder.formattedTime()} (${reminder.effectiveSoundLabel()})",
            status = NotificationHistoryStatus.TRIGGERED
        )
    }

    fun showSessionCompletedNotification(context: Context, durationMinutes: Int, blockedAttempts: Int) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            3001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Focus Session Complete!")
            .setContentText("Read for ${durationMinutes}m • Blocked $blockedAttempts distractions")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { nm.notify(3001, notification) }
    }

    fun showAccountabilityQuitNotification(
        context: Context,
        partnerName: String,
        elapsedMinutes: Int,
        blockedAttempts: Int
    ) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val who = partnerName.ifBlank { "Accountability Partner" }
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Session Ended Early")
            .setContentText("Alert ready for $who • ${elapsedMinutes}m elapsed, $blockedAttempts blocks")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(3002, notification) }
    }

    fun showBedtimeApproachingNotification(context: Context, minutes: Int = 30) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            4001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, BEDTIME_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bedtime in $minutes Minutes 🌙")
            .setContentText("Wind down and get ready for rest. Bedtime Lock will engage soon.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { nm.notify(4001, notification) }
    }

    fun showBedtimeActivatedNotification(context: Context) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            4002,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, BEDTIME_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bedtime Lock Active 🌙")
            .setContentText("Distracting apps are locked. Rest well tonight!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { nm.notify(4002, notification) }
    }

    fun showBedtimeWakeUpNotification(context: Context) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            4003,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, BEDTIME_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Good Morning! ☀️")
            .setContentText("Bedtime Lock is disengaged. Have a restful, productive day!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { nm.notify(4003, notification) }
    }

    fun showFocusStartedNotification(context: Context, modeName: String, durationMinutes: Int) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            3010,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cleanMode = modeName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$cleanMode Session Started")
            .setContentText("Target: ${durationMinutes}m focus. Distractions are shielded.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { nm.notify(3010, notification) }
    }

    fun showFocusCompletedNotification(context: Context, modeName: String) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            3011,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cleanMode = modeName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$cleanMode Completed! 🎯")
            .setContentText("Great job staying on task. Time for a well-deserved break.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { nm.notify(3011, notification) }
    }

    fun showBreakStartedNotification(context: Context, breakMinutes: Int) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            3012,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Break Started ☕")
            .setContentText("Relax and recharge for ${breakMinutes}m. You earned it!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { nm.notify(3012, notification) }
    }

    fun showBreakCompletedNotification(context: Context) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            3013,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Break Time Over! 🔔")
            .setContentText("Ready to start your next focus block? Let's go!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { nm.notify(3013, notification) }
    }

    fun showDailyReminderNotification(context: Context) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Time to Read with FocusLock")
            .setContentText("Start a focused reading session to keep your streak alive.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(3003, notification) }
    }

    fun buildAccountabilityShareIntent(
        partnerName: String,
        partnerEmail: String,
        elapsedMinutes: Int,
        blockedAttempts: Int
    ): Intent {
        val greeting = if (partnerName.isNotBlank()) "Hi $partnerName," else "Hi,"
        val body = "$greeting I ended my FocusLock reading session early after ${elapsedMinutes}m (with $blockedAttempts distractions blocked). Keeping you posted for accountability!"
        return if (partnerEmail.isNotBlank()) {
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(partnerEmail)}")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "FocusLock Accountability Update")
                putExtra(Intent.EXTRA_TEXT, body)
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
    private const val REQUEST_CODE = 4401

    fun scheduleDailyReminder(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!enabled) {
            runCatching { am.cancel(pi) }
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
        runCatching {
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pi
            )
        }
    }

    fun computeNextTriggerTimeMillis(
        item: ReadingReminderItem,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (!item.enabled) return null
        val snoozed = item.snoozedUntilMs
        if (snoozed != null && snoozed > nowMillis) {
            return snoozed
        }
        return when (item.repeatMode) {
            ReminderRepeatMode.ONCE -> {
                val target = baseCalendar(item).timeInMillis
                if (target > nowMillis) target else null
            }
            ReminderRepeatMode.DAILY,
            ReminderRepeatMode.WEEKLY,
            ReminderRepeatMode.MONTHLY -> advanceCalendar(item, nowMillis).timeInMillis
        }
    }

    fun advanceCalendar(item: ReadingReminderItem, nowMillis: Long): Calendar {
        val cal = baseCalendar(item)
        if (cal.timeInMillis > nowMillis) return cal
        when (item.repeatMode) {
            ReminderRepeatMode.ONCE -> Unit
            ReminderRepeatMode.DAILY -> {
                while (cal.timeInMillis <= nowMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            ReminderRepeatMode.WEEKLY -> {
                while (cal.timeInMillis <= nowMillis) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            ReminderRepeatMode.MONTHLY -> {
                var monthOffset = 0
                val startYear = item.year
                val startMonthZero = (item.month - 1).coerceIn(0, 11)
                while (cal.timeInMillis <= nowMillis) {
                    monthOffset += 1
                    val totalMonths = startYear * 12 + startMonthZero + monthOffset
                    val targetYear = totalMonths / 12
                    val targetMonth = totalMonths % 12
                    cal.set(Calendar.YEAR, targetYear)
                    cal.set(Calendar.MONTH, targetMonth)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, min(item.dayOfMonth.coerceAtLeast(1), maxDay))
                    cal.set(Calendar.HOUR_OF_DAY, item.hour.coerceIn(0, 23))
                    cal.set(Calendar.MINUTE, item.minute.coerceIn(0, 59))
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
            }
        }
        return cal
    }

    private fun baseCalendar(item: ReadingReminderItem): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, item.year)
            set(Calendar.MONTH, (item.month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, min(item.dayOfMonth.coerceAtLeast(1), maxDay))
            set(Calendar.HOUR_OF_DAY, item.hour.coerceIn(0, 23))
            set(Calendar.MINUTE, item.minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun syncAllAlarms(context: Context) {
        ReadingReminderStore.ensureLoaded(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = ReadingReminderStore.preferences.value
        val now = System.currentTimeMillis()
        ReadingReminderStore.reminders.value.forEach { item ->
            val pi = pendingIntentForReminder(context, item.id)
            val triggerAt = if (prefs.globalEnabled) computeNextTriggerTimeMillis(item, now) else null
            if (triggerAt == null) {
                runCatching { am.cancel(pi) }
            } else {
                runCatching {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
        }
    }

    fun cancelReminderAlarm(context: Context, reminderId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching { am.cancel(pendingIntentForReminder(context, reminderId)) }
    }

    private fun pendingIntentForReminder(context: Context, reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            5000 + (reminderId.hashCode() and 0x0FFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReadingReminderStore.ensureLoaded(context)
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID)
        if (reminderId.isNullOrBlank()) {
            NotificationHelper.showDailyReminderNotification(context)
            return
        }
        val item = ReadingReminderStore.getReminderById(context, reminderId) ?: return
        val prefs = ReadingReminderStore.preferences.value
        NotificationHelper.showCustomReminderNotification(context, item, prefs, bypassQuietHours = false)
        if (item.repeatMode == ReminderRepeatMode.ONCE) {
            ReadingReminderStore.setReminderEnabled(context, item.id, false)
        } else {
            ReminderScheduler.syncAllAlarms(context)
        }
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        if (notifId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(notifId)
        }
        when (intent.action) {
            NotificationHelper.ACTION_SNOOZE -> ReadingReminderStore.snoozeReminder(context, reminderId)
            NotificationHelper.ACTION_COMPLETE -> ReadingReminderStore.markReminderCompleted(context, reminderId)
            NotificationHelper.ACTION_DISMISS -> ReadingReminderStore.dismissReminder(context, reminderId)
        }
    }
}
