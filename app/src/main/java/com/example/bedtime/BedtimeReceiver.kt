package com.example.bedtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notifications.NotificationHelper
import java.util.Calendar

class BedtimeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BEDTIME_START = "com.example.bedtime.ACTION_BEDTIME_START"
        const val ACTION_BEDTIME_WAKEUP = "com.example.bedtime.ACTION_BEDTIME_WAKEUP"
        const val ACTION_BEDTIME_REMINDER = "com.example.bedtime.ACTION_BEDTIME_REMINDER"

        fun scheduleBedtimeAlarms(context: Context) {
            val s = BedtimeManager.schedule.value
            if (!s.isEnabled) return
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val now = Calendar.getInstance()

            // Schedule Bedtime Start
            val bedtimeCal = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, s.bedtimeHour)
                set(Calendar.MINUTE, s.bedtimeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val startIntent = Intent(context, BedtimeReceiver::class.java).apply {
                action = ACTION_BEDTIME_START
            }
            val startPi = PendingIntent.getBroadcast(
                context,
                2001,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, bedtimeCal.timeInMillis, startPi)
            }

            // Schedule Wakeup
            val wakeupCal = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, s.wakeUpHour)
                set(Calendar.MINUTE, s.wakeUpMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val wakeupIntent = Intent(context, BedtimeReceiver::class.java).apply {
                action = ACTION_BEDTIME_WAKEUP
            }
            val wakeupPi = PendingIntent.getBroadcast(
                context,
                2002,
                wakeupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeupCal.timeInMillis, wakeupPi)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        BedtimeManager.init(context)

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                BedtimeManager.evaluateCurrentStatus(context)
                scheduleBedtimeAlarms(context)
            }
            ACTION_BEDTIME_START -> {
                BedtimeManager.evaluateCurrentStatus(context)
                if (BedtimeManager.isBedtimeLockActive.value) {
                    NotificationHelper.showBedtimeActivatedNotification(context)
                }
                scheduleBedtimeAlarms(context)
            }
            ACTION_BEDTIME_WAKEUP -> {
                BedtimeManager.setLockActive(context, false)
                NotificationHelper.showBedtimeWakeUpNotification(context)
                scheduleBedtimeAlarms(context)
            }
            ACTION_BEDTIME_REMINDER -> {
                NotificationHelper.showBedtimeApproachingNotification(context, 30)
            }
        }
    }
}
