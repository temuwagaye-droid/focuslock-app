package com.example.telemetry

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TelemetryLogEntry(
    val id: String,
    val kind: String,
    val name: String,
    val detail: String,
    val timestampMs: Long
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}

object TelemetryManager {
    private const val ANALYTICS_FILE = "focuslock_analytics.json"
    private const val CRASH_FILE = "focuslock_crashes.json"
    private const val MAX_EVENTS = 100
    private const val MAX_CRASHES = 50

    @Volatile
    private var crashHandlerInstalled = false

    @Volatile
    var isAnalyticsOptIn: Boolean = true

    @Volatile
    var isCrashReportingOptIn: Boolean = true

    fun installCrashHandler(context: Context) {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true
        val appCtx = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isCrashReportingOptIn) {
                runCatching {
                    val entry = TelemetryLogEntry(
                        id = UUID.randomUUID().toString(),
                        kind = "crash",
                        name = throwable::class.java.simpleName.ifBlank { "Exception" },
                        detail = "${throwable.message ?: "No message"} (${thread.name})",
                        timestampMs = System.currentTimeMillis()
                    )
                    appendEntry(File(appCtx.filesDir, CRASH_FILE), entry, MAX_CRASHES)
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun trackEvent(context: Context, name: String, detail: String = "") {
        if (!isAnalyticsOptIn) return
        val appCtx = context.applicationContext
        val entry = TelemetryLogEntry(
            id = UUID.randomUUID().toString(),
            kind = "event",
            name = name,
            detail = detail,
            timestampMs = System.currentTimeMillis()
        )
        runCatching {
            appendEntry(File(appCtx.filesDir, ANALYTICS_FILE), entry, MAX_EVENTS)
        }
    }

    fun getAnalyticsEvents(context: Context): List<TelemetryLogEntry> =
        readEntries(File(context.applicationContext.filesDir, ANALYTICS_FILE))

    fun getCrashReports(context: Context): List<TelemetryLogEntry> =
        readEntries(File(context.applicationContext.filesDir, CRASH_FILE))

    fun clearAllLogs(context: Context) {
        val dir = context.applicationContext.filesDir
        runCatching { File(dir, ANALYTICS_FILE).delete() }
        runCatching { File(dir, CRASH_FILE).delete() }
    }

    @Synchronized
    private fun appendEntry(file: File, entry: TelemetryLogEntry, maxCount: Int) {
        val existing = readEntries(file).toMutableList()
        existing.add(0, entry)
        val trimmed = existing.take(maxCount)
        val arr = JSONArray()
        trimmed.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("kind", item.kind)
                    put("name", item.name)
                    put("detail", item.detail)
                    put("timestampMs", item.timestampMs)
                }
            )
        }
        file.writeText(arr.toString())
    }

    private fun readEntries(file: File): List<TelemetryLogEntry> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val raw = file.readText()
            if (raw.isBlank()) return emptyList()
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        TelemetryLogEntry(
                            id = obj.optString("id"),
                            kind = obj.optString("kind", "event"),
                            name = obj.optString("name"),
                            detail = obj.optString("detail"),
                            timestampMs = obj.optLong("timestampMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
