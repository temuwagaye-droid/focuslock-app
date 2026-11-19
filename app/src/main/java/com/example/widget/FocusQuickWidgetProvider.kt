package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class FocusQuickWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE)
        val totalMins = prefs.getLong("widget_total_minutes", 0L)
        val streak = prefs.getInt("widget_streak_days", 0)
        val blocked = prefs.getInt("widget_total_blocked", 0)
        val statsSummary = "${totalMins}m focus • ${streak}d streak • $blocked blocked"

        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_focus_lock)
            views.setTextViewText(R.id.widget_stats_text, statsSummary)

            val startIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("quick_action", "start_focus")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val startPi = PendingIntent.getActivity(
                context,
                widgetId,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_start_button, startPi)
            views.setOnClickPendingIntent(R.id.widget_root_container, startPi)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    companion object {
        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = android.content.ComponentName(context, FocusQuickWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, FocusQuickWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}

object ShortcutHelper {
    fun pinFocusShortcut(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!sm.isRequestPinShortcutSupported) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("quick_action", "start_focus")
        }
        val shortcut = ShortcutInfo.Builder(context, "pinned_start_focus")
            .setShortLabel("Start Focus")
            .setLongLabel("Start Reading Focus Session")
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
        return runCatching { sm.requestPinShortcut(shortcut, null) }.getOrDefault(false)
    }
}
