package com.example.telemetry

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TelemetryLogEntry(
    val id: String,
    val kind: String,
    val name: String,
    val detail: String,
    val timestampMs: Long
) {
    fun formattedTime(): String {
        return SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
    }
}

/**
 * Local-first, privacy-respecting Analytics & Uncaught Crash Reporting manager.
 * Stores bounded diagnostic buffers locally and honors user opt-in settings.
 */
object TelemetryManager {

    private const val ANALYTICS_FILE = "analytics_events.json"
    private const val CRASH_FILE = "crash_reports.json"
    private const val MAX_EVENTS = 100
    private const val MAX_CRASHES = 20

    @Volatile
    private var crashHandlerInstalled = false

    @Volatile
    var isAnalyticsOptIn: Boolean = true

    @Volatile
    var isCrashReportingOptIn: Boolean = true

    fun installCrashHandler(context: Context) {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true

        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isCrashReportingOptIn) {
                try {
                    val stack = throwable.stackTraceToString().take(1200)
                    val entry = TelemetryLogEntry(
                        id = "crash_${System.currentTimeMillis()}",
                        kind = "crash",
                        name = throwable::class.java.simpleName.ifBlank { "Exception" },
                        detail = "Thread=${thread.name}: ${throwable.message ?: "No message"}\n$stack",
                        timestampMs = System.currentTimeMillis()
                    )
                    appendEntry(File(appContext.filesDir, CRASH_FILE), entry, MAX_CRASHES)
                } catch (_: Throwable) {
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun trackEvent(context: Context, name: String, detail: String = "") {
        if (!isAnalyticsOptIn) return
        try {
            val entry = TelemetryLogEntry(
                id = "evt_${System.currentTimeMillis()}_${name.hashCode()}",
                kind = "analytics",
                name = name,
                detail = detail.take(300),
                timestampMs = System.currentTimeMillis()
            )
            appendEntry(File(context.applicationContext.filesDir, ANALYTICS_FILE), entry, MAX_EVENTS)
        } catch (_: Throwable) {
        }
    }

    fun getAnalyticsEvents(context: Context): List<TelemetryLogEntry> {
        return readEntries(File(context.applicationContext.filesDir, ANALYTICS_FILE)).reversed()
    }

    fun getCrashReports(context: Context): List<TelemetryLogEntry> {
        return readEntries(File(context.applicationContext.filesDir, CRASH_FILE)).reversed()
    }

    fun clearAllLogs(context: Context) {
        val dir = context.applicationContext.filesDir
        File(dir, ANALYTICS_FILE).delete()
        File(dir, CRASH_FILE).delete()
    }

    private fun appendEntry(file: File, entry: TelemetryLogEntry, maxCount: Int) {
        synchronized(this) {
            val current = readEntries(file).toMutableList()
            current.add(entry)
            val trimmed = if (current.size > maxCount) current.takeLast(maxCount) else current
            val array = JSONArray()
            for (item in trimmed) {
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("kind", item.kind)
                        put("name", item.name)
                        put("detail", item.detail)
                        put("timestampMs", item.timestampMs)
                    }
                )
            }
            file.writeText(array.toString())
        }
    }

    private fun readEntries(file: File): List<TelemetryLogEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            val array = JSONArray(text)
            val list = ArrayList<TelemetryLogEntry>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TelemetryLogEntry(
                        id = obj.optString("id"),
                        kind = obj.optString("kind"),
                        name = obj.optString("name"),
                        detail = obj.optString("detail"),
                        timestampMs = obj.optLong("timestampMs")
                    )
                )
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
