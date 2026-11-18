package com.example.notifications

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

/**
 * Persistent reactive store for Reading Reminders, Quiet Hours & Notification Preferences,
 * and Notification History. Shared by UI ViewModel and BroadcastReceivers.
 */
object ReadingReminderStore {

    private const val PREFS_NAME = "focuslock_reading_reminders"
    private const val KEY_REMINDERS_JSON = "reminders_json"
    private const val KEY_PREFS_JSON = "reminder_prefs_json"
    private const val KEY_HISTORY_JSON = "notification_history_json"
    private const val MAX_HISTORY_ENTRIES = 60

    private val lock = Any()

    @Volatile
    private var loadedFromPrefs = false

    private val _reminders = MutableStateFlow<List<ReadingReminderItem>>(emptyList())
    val reminders: StateFlow<List<ReadingReminderItem>> = _reminders.asStateFlow()

    private val _preferences = MutableStateFlow(ReminderPreferences())
    val preferences: StateFlow<ReminderPreferences> = _preferences.asStateFlow()

    private val _history = MutableStateFlow<List<NotificationHistoryEntry>>(emptyList())
    val history: StateFlow<List<NotificationHistoryEntry>> = _history.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (loadedFromPrefs) return
        synchronized(lock) {
            if (loadedFromPrefs) return
            val prefs = prefs(context)
            loadRemindersLocked(prefs)
            loadPreferencesLocked(prefs)
            loadHistoryLocked(prefs)
            loadedFromPrefs = true
        }
    }

    fun reloadFromPrefs(context: Context) {
        synchronized(lock) {
            val prefs = prefs(context)
            loadRemindersLocked(prefs)
            loadPreferencesLocked(prefs)
            loadHistoryLocked(prefs)
            loadedFromPrefs = true
        }
    }

    fun createReminder(
        context: Context,
        title: String,
        note: String,
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        repeatMode: ReminderRepeatMode,
        soundOption: ReminderSoundOption,
        customSoundUri: String? = null,
        customSoundLabel: String? = null
    ): ReadingReminderItem {
        ensureLoaded(context)
        val item = ReadingReminderItem(
            id = "rem_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            title = title.trim().ifBlank { "Reading Session" },
            note = note.trim(),
            year = year,
            month = month.coerceIn(1, 12),
            dayOfMonth = dayOfMonth.coerceIn(1, 31),
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            repeatMode = repeatMode,
            enabled = true,
            completedAtMs = null,
            snoozedUntilMs = null,
            soundOption = soundOption,
            customSoundUri = customSoundUri,
            customSoundLabel = customSoundLabel,
            createdAtMs = System.currentTimeMillis()
        )
        synchronized(lock) {
            _reminders.value = listOf(item) + _reminders.value
            persistRemindersLocked(prefs(context))
        }
        ReminderScheduler.syncAllAlarms(context)
        return item
    }

    fun updateReminder(context: Context, updated: ReadingReminderItem) {
        ensureLoaded(context)
        synchronized(lock) {
            val list = _reminders.value.toMutableList()
            val idx = list.indexOfFirst { it.id == updated.id }
            if (idx >= 0) {
                list[idx] = updated.copy(
                    title = updated.title.trim().ifBlank { "Reading Session" },
                    note = updated.note.trim(),
                    snoozedUntilMs = null,
                    completedAtMs = null
                )
            } else {
                list.add(0, updated)
            }
            _reminders.value = list
            persistRemindersLocked(prefs(context))
        }
        ReminderScheduler.syncAllAlarms(context)
    }

    fun deleteReminder(context: Context, reminderId: String) {
        ensureLoaded(context)
        synchronized(lock) {
            _reminders.value = _reminders.value.filterNot { it.id == reminderId }
            persistRemindersLocked(prefs(context))
        }
        ReminderScheduler.cancelReminderAlarm(context, reminderId)
        ReminderScheduler.syncAllAlarms(context)
    }

    fun setReminderEnabled(context: Context, reminderId: String, enabled: Boolean) {
        ensureLoaded(context)
        synchronized(lock) {
            _reminders.value = _reminders.value.map {
                if (it.id == reminderId) {
                    it.copy(
                        enabled = enabled,
                        completedAtMs = if (enabled && it.repeatMode == ReminderRepeatMode.ONCE) null else it.completedAtMs
                    )
                } else it
            }
            persistRemindersLocked(prefs(context))
        }
        ReminderScheduler.syncAllAlarms(context)
    }

    fun snoozeReminder(context: Context, reminderId: String, snoozeMinutes: Int? = null): ReadingReminderItem? {
        ensureLoaded(context)
        var target: ReadingReminderItem? = null
        val mins = (snoozeMinutes ?: _preferences.value.defaultSnoozeMinutes).coerceIn(1, 180)
        val snoozeUntil = System.currentTimeMillis() + mins * 60_000L
        synchronized(lock) {
            _reminders.value = _reminders.value.map {
                if (it.id == reminderId) {
                    val upd = it.copy(
                        enabled = true,
                        snoozedUntilMs = snoozeUntil,
                        completedAtMs = null
                    )
                    target = upd
                    upd
                } else it
            }
            persistRemindersLocked(prefs(context))
        }
        target?.let {
            appendHistory(
                context = context,
                reminderId = it.id,
                title = it.title,
                message = "Snoozed for ${mins}m",
                status = NotificationHistoryStatus.SNOOZED
            )
            ReminderScheduler.syncAllAlarms(context)
        }
        return target
    }

    fun markReminderCompleted(context: Context, reminderId: String): ReadingReminderItem? {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        var target: ReadingReminderItem? = null
        synchronized(lock) {
            _reminders.value = _reminders.value.map { item ->
                if (item.id == reminderId) {
                    val upd = if (item.repeatMode == ReminderRepeatMode.ONCE) {
                        item.copy(completedAtMs = now, snoozedUntilMs = null, enabled = false)
                    } else {
                        val nextCal = ReminderScheduler.advanceCalendar(item, now)
                        item.copy(
                            year = nextCal.get(Calendar.YEAR),
                            month = nextCal.get(Calendar.MONTH) + 1,
                            dayOfMonth = nextCal.get(Calendar.DAY_OF_MONTH),
                            completedAtMs = now,
                            snoozedUntilMs = null
                        )
                    }
                    target = upd
                    upd
                } else item
            }
            persistRemindersLocked(prefs(context))
        }
        target?.let {
            appendHistory(
                context = context,
                reminderId = it.id,
                title = it.title,
                message = if (it.repeatMode == ReminderRepeatMode.ONCE) "Marked completed" else "Completed (${it.repeatMode.label} rescheduled)",
                status = NotificationHistoryStatus.COMPLETED
            )
            ReminderScheduler.syncAllAlarms(context)
        }
        return target
    }

    fun dismissReminder(context: Context, reminderId: String): ReadingReminderItem? {
        ensureLoaded(context)
        val now = System.currentTimeMillis()
        var target: ReadingReminderItem? = null
        synchronized(lock) {
            _reminders.value = _reminders.value.map { item ->
                if (item.id == reminderId) {
                    val upd = if (item.repeatMode == ReminderRepeatMode.ONCE) {
                        item.copy(snoozedUntilMs = null, enabled = false)
                    } else {
                        val nextCal = ReminderScheduler.advanceCalendar(item, now)
                        item.copy(
                            year = nextCal.get(Calendar.YEAR),
                            month = nextCal.get(Calendar.MONTH) + 1,
                            dayOfMonth = nextCal.get(Calendar.DAY_OF_MONTH),
                            snoozedUntilMs = null
                        )
                    }
                    target = upd
                    upd
                } else item
            }
            persistRemindersLocked(prefs(context))
        }
        target?.let {
            appendHistory(
                context = context,
                reminderId = it.id,
                title = it.title,
                message = "Dismissed notification",
                status = NotificationHistoryStatus.DISMISSED
            )
            ReminderScheduler.syncAllAlarms(context)
        }
        return target
    }

    fun updatePreferences(context: Context, updated: ReminderPreferences) {
        ensureLoaded(context)
        synchronized(lock) {
            _preferences.value = updated
            persistPreferencesLocked(prefs(context))
        }
        ReminderScheduler.syncAllAlarms(context)
    }

    fun appendHistory(
        context: Context,
        reminderId: String,
        title: String,
        message: String,
        status: NotificationHistoryStatus
    ) {
        ensureLoaded(context)
        val entry = NotificationHistoryEntry(
            id = "hist_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            reminderId = reminderId,
            title = title,
            message = message,
            status = status,
            timestampMs = System.currentTimeMillis()
        )
        synchronized(lock) {
            val updated = (listOf(entry) + _history.value).take(MAX_HISTORY_ENTRIES)
            _history.value = updated
            persistHistoryLocked(prefs(context))
        }
    }

    fun clearHistory(context: Context) {
        ensureLoaded(context)
        synchronized(lock) {
            _history.value = emptyList()
            persistHistoryLocked(prefs(context))
        }
    }

    fun getReminderById(context: Context, reminderId: String): ReadingReminderItem? {
        ensureLoaded(context)
        return _reminders.value.firstOrNull { it.id == reminderId }
    }

    fun clearAllForTesting(context: Context) {
        synchronized(lock) {
            _reminders.value = emptyList()
            _preferences.value = ReminderPreferences()
            _history.value = emptyList()
            prefs(context).edit().clear().apply()
            loadedFromPrefs = true
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadRemindersLocked(sp: SharedPreferences) {
        val raw = sp.getString(KEY_REMINDERS_JSON, null) ?: run {
            _reminders.value = emptyList()
            return
        }
        runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<ReadingReminderItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ReadingReminderItem(
                        id = o.optString("id"),
                        title = o.optString("title", "Reading Session"),
                        note = o.optString("note", ""),
                        year = o.optInt("year", 2026),
                        month = o.optInt("month", 1),
                        dayOfMonth = o.optInt("dayOfMonth", 1),
                        hour = o.optInt("hour", 20),
                        minute = o.optInt("minute", 0),
                        repeatMode = ReminderRepeatMode.fromId(o.optString("repeatMode")),
                        enabled = o.optBoolean("enabled", true),
                        completedAtMs = if (o.has("completedAtMs") && !o.isNull("completedAtMs")) o.optLong("completedAtMs") else null,
                        snoozedUntilMs = if (o.has("snoozedUntilMs") && !o.isNull("snoozedUntilMs")) o.optLong("snoozedUntilMs") else null,
                        soundOption = ReminderSoundOption.fromId(o.optString("soundOption")),
                        customSoundUri = o.optString("customSoundUri").takeIf { it.isNotBlank() },
                        customSoundLabel = o.optString("customSoundLabel").takeIf { it.isNotBlank() },
                        createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis())
                    )
                )
            }
            _reminders.value = list
        }
    }

    private fun persistRemindersLocked(sp: SharedPreferences) {
        val arr = JSONArray()
        for (item in _reminders.value) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("note", item.note)
                    put("year", item.year)
                    put("month", item.month)
                    put("dayOfMonth", item.dayOfMonth)
                    put("hour", item.hour)
                    put("minute", item.minute)
                    put("repeatMode", item.repeatMode.id)
                    put("enabled", item.enabled)
                    if (item.completedAtMs != null) put("completedAtMs", item.completedAtMs) else put("completedAtMs", JSONObject.NULL)
                    if (item.snoozedUntilMs != null) put("snoozedUntilMs", item.snoozedUntilMs) else put("snoozedUntilMs", JSONObject.NULL)
                    put("soundOption", item.soundOption.id)
                    put("customSoundUri", item.customSoundUri ?: "")
                    put("customSoundLabel", item.customSoundLabel ?: "")
                    put("createdAtMs", item.createdAtMs)
                }
            )
        }
        sp.edit().putString(KEY_REMINDERS_JSON, arr.toString()).apply()
    }

    private fun loadPreferencesLocked(sp: SharedPreferences) {
        val raw = sp.getString(KEY_PREFS_JSON, null) ?: return
        runCatching {
            val o = JSONObject(raw)
            _preferences.value = ReminderPreferences(
                globalEnabled = o.optBoolean("globalEnabled", true),
                quietHoursEnabled = o.optBoolean("quietHoursEnabled", false),
                quietStartHour = o.optInt("quietStartHour", 22),
                quietStartMinute = o.optInt("quietStartMinute", 0),
                quietEndHour = o.optInt("quietEndHour", 7),
                quietEndMinute = o.optInt("quietEndMinute", 0),
                vibrateEnabled = o.optBoolean("vibrateEnabled", true),
                showActionButtons = o.optBoolean("showActionButtons", true),
                defaultSnoozeMinutes = o.optInt("defaultSnoozeMinutes", 10),
                defaultSoundOption = ReminderSoundOption.fromId(o.optString("defaultSoundOption")),
                defaultCustomSoundUri = o.optString("defaultCustomSoundUri").takeIf { it.isNotBlank() },
                defaultCustomSoundLabel = o.optString("defaultCustomSoundLabel").takeIf { it.isNotBlank() }
            )
        }
    }

    private fun persistPreferencesLocked(sp: SharedPreferences) {
        val p = _preferences.value
        val o = JSONObject().apply {
            put("globalEnabled", p.globalEnabled)
            put("quietHoursEnabled", p.quietHoursEnabled)
            put("quietStartHour", p.quietStartHour)
            put("quietStartMinute", p.quietStartMinute)
            put("quietEndHour", p.quietEndHour)
            put("quietEndMinute", p.quietEndMinute)
            put("vibrateEnabled", p.vibrateEnabled)
            put("showActionButtons", p.showActionButtons)
            put("defaultSnoozeMinutes", p.defaultSnoozeMinutes)
            put("defaultSoundOption", p.defaultSoundOption.id)
            put("defaultCustomSoundUri", p.defaultCustomSoundUri ?: "")
            put("defaultCustomSoundLabel", p.defaultCustomSoundLabel ?: "")
        }
        sp.edit().putString(KEY_PREFS_JSON, o.toString()).apply()
    }

    private fun loadHistoryLocked(sp: SharedPreferences) {
        val raw = sp.getString(KEY_HISTORY_JSON, null) ?: run {
            _history.value = emptyList()
            return
        }
        runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<NotificationHistoryEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    NotificationHistoryEntry(
                        id = o.optString("id"),
                        reminderId = o.optString("reminderId"),
                        title = o.optString("title"),
                        message = o.optString("message"),
                        status = NotificationHistoryStatus.fromId(o.optString("status")),
                        timestampMs = o.optLong("timestampMs", 0L)
                    )
                )
            }
            _history.value = list
        }
    }

    private fun persistHistoryLocked(sp: SharedPreferences) {
        val arr = JSONArray()
        for (entry in _history.value) {
            arr.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("reminderId", entry.reminderId)
                    put("title", entry.title)
                    put("message", entry.message)
                    put("status", entry.status.id)
                    put("timestampMs", entry.timestampMs)
                }
            )
        }
        sp.edit().putString(KEY_HISTORY_JSON, arr.toString()).apply()
    }
}
