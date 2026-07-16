package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class FocusService : Service() {

    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                startForeground(NOTIFICATION_ID, buildNotification("Focus Session Started", "00:00 remaining"))
                startTimer()
            }
            ACTION_START_BREAK -> {
                startForeground(NOTIFICATION_ID, buildNotification("Coffee Break ☕ Enjoy!", "05:00 remaining"))
                startTimer()
            }
            ACTION_STOP_SESSION -> {
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (FocusSessionManager.isSessionActive.value || FocusSessionManager.isBreakActive.value) {
                val remainingSeconds = FocusSessionManager.timeLeftSeconds.value
                if (remainingSeconds <= 0) {
                    if (FocusSessionManager.isSessionActive.value) {
                        FocusSessionManager.stopSession(this@FocusService, completed = true)
                    } else {
                        FocusSessionManager.stopBreak(this@FocusService)
                    }
                    break
                }
                updateNotification(remainingSeconds)
                delay(1000)
                FocusSessionManager.tick()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateNotification(seconds: Long) {
        val isBreak = FocusSessionManager.isBreakActive.value
        val title = if (isBreak) "Coffee Break ☕ Enjoy!" else "Locked In — Reading Focus"
        val minutesPart = seconds / 60
        val secondsPart = seconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d remaining", minutesPart, secondsPart)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, timeString))
    }

    private fun buildNotification(title: String, contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play) // Standard system play icon as fallback
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ReadLock Focus Session Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active reading focus sessions"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "readlock_focus_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SESSION = "com.example.service.action.START_SESSION"
        const val ACTION_START_BREAK = "com.example.service.action.START_BREAK"
        const val ACTION_STOP_SESSION = "com.example.service.action.STOP_SESSION"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
    }
}
