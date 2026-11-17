package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

class FocusQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun requestWidgetUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FocusQuickWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE)
            val totalMinutes = prefs.getLong("widget_total_minutes", 0L)
            val streak = prefs.getInt("widget_streak", 0)
            val blocked = prefs.getInt("widget_blocked_attempts", 0)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("quick_action", "start_focus")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 4000,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val views = RemoteViews(context.packageName, R.layout.widget_focus_lock).apply {
                setTextViewText(R.id.widget_stats_text, "${totalMinutes}m focus • ${streak}d streak • $blocked blocked")
                setOnClickPendingIntent(R.id.widget_start_button, pendingIntent)
                setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

object ShortcutHelper {
    fun pinFocusShortcut(context: Context): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("quick_action", "start_focus")
        }
        val shortcut = ShortcutInfoCompat.Builder(context, "quick_start_focus_pinned")
            .setShortLabel("Start Focus")
            .setLongLabel("Start Reading Focus Session")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(shortcutIntent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
