package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class FocusService : Service() {

    companion object {
        const val CHANNEL_ID = "readlock_focus_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SESSION = "com.example.service.action.START_SESSION"
        const val ACTION_START_BREAK = "com.example.service.action.START_BREAK"
        const val ACTION_STOP_SESSION = "com.example.service.action.STOP_SESSION"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SESSION -> {
                timerJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_BREAK, ACTION_START_SESSION, null -> {
                startForeground(NOTIFICATION_ID, buildNotification(FocusSessionManager.timeLeftSeconds.value))
                startTickerLoop()
            }
        }
        return START_STICKY
    }

    private fun startTickerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && (FocusSessionManager.isSessionActive.value || FocusSessionManager.isBreakActive.value)) {
                delay(1000L)
                val remaining = FocusSessionManager.timeLeftSeconds.value
                if (remaining > 1L) {
                    FocusSessionManager.tick()
                    updateNotification(remaining - 1L)
                } else {
                    FocusSessionManager.tick()
                    if (FocusSessionManager.isSessionActive.value) {
                        FocusSessionManager.stopSession(applicationContext, completed = true)
                    } else if (FocusSessionManager.isBreakActive.value) {
                        FocusSessionManager.stopBreak(applicationContext)
                    }
                    break
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(secondsLeft: Long): Notification {
        val mins = secondsLeft / 60L
        val secs = secondsLeft % 60L
        val formatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        val isBreak = FocusSessionManager.isBreakActive.value
        val title = if (isBreak) "FocusLock Break Timer" else "FocusLock Session Active"
        val text = if (isBreak) "Coffee break remaining: $formatted" else "Stay focused • $formatted remaining"

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
    }

    private fun updateNotification(secondsLeft: Long) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { nm.notify(NOTIFICATION_ID, buildNotification(secondsLeft)) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active Focus Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent countdown notification during focus and break sessions"
        }
        nm.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
