package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Calendar

object NotificationHelper {

    const val REMINDER_CHANNEL_ID = "readlock_daily_reminders"
    const val ALERT_CHANNEL_ID = "readlock_focus_alerts"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Daily Reading Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to start your distraction-free reading session"
            }
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Focus & Accountability Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Session completion rewards and accountability partner alerts"
            }
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(alertChannel)
        }
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
        ensureChannels(context)
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
        val notif = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
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
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.showDailyReminderNotification(context)
    }
}
