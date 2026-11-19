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

object ReadingReminderStore {
    private const val PREFS_NAME = "focuslock_reading_reminders"
    private const val KEY_REMINDERS = "reminders_json"
    private const val KEY_PREFS = "preferences_json"
    private const val KEY_HISTORY = "history_json"
    private const val MAX_HISTORY = 100

    private val lock = Any()
    @Volatile
    private var loaded = false

    private val _reminders = MutableStateFlow<List<ReadingReminderItem>>(emptyList())
    val reminders: StateFlow<List<ReadingReminderItem>> = _reminders.asStateFlow()

    private val _preferences = MutableStateFlow(ReminderPreferences())
    val preferences: StateFlow<ReminderPreferences> = _preferences.asStateFlow()

    private val _history = MutableStateFlow<List<NotificationHistoryEntry>>(emptyList())
    val history: StateFlow<List<NotificationHistoryEntry>> = _history.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            val sp = prefs(context)
            loadRemindersLocked(sp)
            loadPreferencesLocked(sp)
            loadHistoryLocked(sp)
            loaded = true
        }
    }

    fun reloadFromPrefs(context: Context) {
        synchronized(lock) {
            val sp = prefs(context)
            loadRemindersLocked(sp)
            loadPreferencesLocked(sp)
            loadHistoryLocked(sp)
            loaded = true
        }
    }

    fun createReminder(
        context: Context,
        title: String,
        note: String = "",
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        repeatMode: ReminderRepeatMode = ReminderRepeatMode.DAILY,
        soundOption: ReminderSoundOption = ReminderSoundOption.DEFAULT,
        customSoundUri: String? = null,
        customSoundLabel: String? = null
    ): ReadingReminderItem {
        ensureLoaded(context)
        val item = ReadingReminderItem(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "Reading Reminder" },
            note = note.trim(),
            year = year,
            month = month.coerceIn(1, 12),
            dayOfMonth = dayOfMonth.coerceIn(1, 31),
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            repeatMode = repeatMode,
            enabled = true,
            soundOption = soundOption,
            customSoundUri = customSoundUri,
            customSoundLabel = customSoundLabel,
            createdAtMs = System.currentTimeMillis()
        )
        synchronized(lock) {
            val updated = listOf(item) + _reminders.value
            _reminders.value = updated
            persistRemindersLocked(prefs(context))
        }
        ReminderScheduler.syncAllAlarms(context)
        return item
    }

    fun updateReminder(context: Context, item: ReadingReminderItem) {
        ensureLoaded(context)
        synchronized(lock) {
            val list = _reminders.value.toMutableList()
            val idx = list.indexOfFirst { it.id == item.id }
            if (idx >= 0) {
                list[idx] = item
            } else {
                list.add(0, item)
            }
            _reminders.value = list
            persistRemindersLocked(prefs(context))
        }
        ReminderScheduler.syncAllAlarms(context)
    }

    fun deleteReminder(context: Context, reminderId: String) {
        ensureLoaded(context)
        ReminderScheduler.cancelReminderAlarm(context, reminderId)
        synchronized(lock) {
            _reminders.value = _reminders.value.filterNot { it.id == reminderId }
            persistRemindersLocked(prefs(context))
        }
    }

    fun setReminderEnabled(context: Context, reminderId: String, enabled: Boolean) {
        ensureLoaded(context)
        synchronized(lock) {
            _reminders.value = _reminders.value.map {
                if (it.id == reminderId) it.copy(enabled = enabled, snoozedUntilMs = null) else it
            }
            persistRemindersLocked(prefs(context))
        }
        if (!enabled) {
            ReminderScheduler.cancelReminderAlarm(context, reminderId)
        }
        ReminderScheduler.syncAllAlarms(context)
    }

    fun snoozeReminder(context: Context, reminderId: String, minutes: Int? = null): ReadingReminderItem? {
        ensureLoaded(context)
        var updatedItem: ReadingReminderItem? = null
        synchronized(lock) {
            val snoozeMins = (minutes ?: _preferences.value.defaultSnoozeMinutes).coerceIn(1, 180)
            val until = System.currentTimeMillis() + snoozeMins * 60_000L
            _reminders.value = _reminders.value.map { item ->
                if (item.id == reminderId) {
                    item.copy(snoozedUntilMs = until, enabled = true).also { updatedItem = it }
                } else {
                    item
                }
            }
            persistRemindersLocked(prefs(context))
        }
        updatedItem?.let {
            appendHistory(
                context = context,
                reminderId = it.id,
                title = it.title,
                message = "Snoozed for ${(minutes ?: _preferences.value.defaultSnoozeMinutes).coerceIn(1, 180)}m",
                status = NotificationHistoryStatus.SNOOZED
            )
            ReminderScheduler.syncAllAlarms(context)
        }
        return updatedItem
    }

    fun markReminderCompleted(context: Context, reminderId: String): ReadingReminderItem? {
        ensureLoaded(context)
        var updatedItem: ReadingReminderItem? = null
        val now = System.currentTimeMillis()
        synchronized(lock) {
            _reminders.value = _reminders.value.map { item ->
                if (item.id == reminderId) {
                    val next = if (item.repeatMode == ReminderRepeatMode.ONCE) {
                        item.copy(
                            completedAtMs = now,
                            snoozedUntilMs = null,
                            enabled = false
                        )
                    } else {
                        val cal = ReminderScheduler.advanceCalendar(item, now)
                        item.copy(
                            year = cal.get(Calendar.YEAR),
                            month = cal.get(Calendar.MONTH) + 1,
                            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                            completedAtMs = now,
                            snoozedUntilMs = null
                        )
                    }
                    next.also { updatedItem = it }
                } else {
                    item
                }
            }
            persistRemindersLocked(prefs(context))
        }
        updatedItem?.let {
            appendHistory(
                context = context,
                reminderId = it.id,
                title = it.title,
                message = if (it.repeatMode == ReminderRepeatMode.ONCE) {
                    "Marked completed"
                } else {
                    "Completed • Next: ${it.formattedDate()} ${it.formattedTime()}"
                },
                status = NotificationHistoryStatus.COMPLETED
            )
            ReminderScheduler.syncAllAlarms(context)
        }
        return updatedItem
    }

    fun dismissReminder(context: Context, reminderId: String): ReadingReminderItem? {
        ensureLoaded(context)
        var updatedItem: ReadingReminderItem? = null
        val now = System.currentTimeMillis()
        synchronized(lock) {
            _reminders.value = _reminders.value.map { item ->
                if (item.id == reminderId) {
                    val next = if (item.repeatMode == ReminderRepeatMode.ONCE) {
                        item.copy(snoozedUntilMs = null, enabled = false)
                    } else {
                        val cal = ReminderScheduler.advanceCalendar(item, now)
                        item.copy(
                            year = cal.get(Calendar.YEAR),
                            month = cal.get(Calendar.MONTH) + 1,
                            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                            snoozedUntilMs = null
                        )
                    }
                    next.also { updatedItem = it }
                } else {
                    item
                }
            }
            persistRemindersLocked(prefs(context))
        }
        updatedItem?.let {
            appendHistory(
                context = context,
                reminderId = it.id,
                title = it.title,
                message = "Dismissed",
                status = NotificationHistoryStatus.DISMISSED
            )
            ReminderScheduler.syncAllAlarms(context)
        }
        return updatedItem
    }

    fun updatePreferences(context: Context, prefsValue: ReminderPreferences) {
        ensureLoaded(context)
        synchronized(lock) {
            _preferences.value = prefsValue
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
            id = UUID.randomUUID().toString(),
            reminderId = reminderId,
            title = title,
            message = message,
            status = status,
            timestampMs = System.currentTimeMillis()
        )
        synchronized(lock) {
            _history.value = (listOf(entry) + _history.value).take(MAX_HISTORY)
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
            loaded = true
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadRemindersLocked(sp: SharedPreferences) {
        val raw = sp.getString(KEY_REMINDERS, null)
        if (raw.isNullOrBlank()) {
            _reminders.value = emptyList()
            return
        }
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: run {
            _reminders.value = emptyList()
            return
        }
        val list = buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    ReadingReminderItem(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = obj.optString("title", "Reading Reminder"),
                        note = obj.optString("note", ""),
                        year = obj.optInt("year", 2026),
                        month = obj.optInt("month", 1),
                        dayOfMonth = obj.optInt("dayOfMonth", 1),
                        hour = obj.optInt("hour", 20),
                        minute = obj.optInt("minute", 0),
                        repeatMode = ReminderRepeatMode.fromId(obj.optString("repeatMode", "daily")),
                        enabled = obj.optBoolean("enabled", true),
                        completedAtMs = if (obj.has("completedAtMs") && !obj.isNull("completedAtMs")) obj.optLong("completedAtMs") else null,
                        snoozedUntilMs = if (obj.has("snoozedUntilMs") && !obj.isNull("snoozedUntilMs")) obj.optLong("snoozedUntilMs") else null,
                        soundOption = ReminderSoundOption.fromId(obj.optString("soundOption", "default")),
                        customSoundUri = obj.optString("customSoundUri", "").ifBlank { null },
                        customSoundLabel = obj.optString("customSoundLabel", "").ifBlank { null },
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                    )
                )
            }
        }
        _reminders.value = list
    }

    private fun persistRemindersLocked(sp: SharedPreferences) {
        val arr = JSONArray()
        _reminders.value.forEach { item ->
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
                    put("completedAtMs", item.completedAtMs ?: JSONObject.NULL)
                    put("snoozedUntilMs", item.snoozedUntilMs ?: JSONObject.NULL)
                    put("soundOption", item.soundOption.id)
                    put("customSoundUri", item.customSoundUri ?: "")
                    put("customSoundLabel", item.customSoundLabel ?: "")
                    put("createdAtMs", item.createdAtMs)
                }
            )
        }
        sp.edit().putString(KEY_REMINDERS, arr.toString()).apply()
    }

    private fun loadPreferencesLocked(sp: SharedPreferences) {
        val raw = sp.getString(KEY_PREFS, null)
        if (raw.isNullOrBlank()) {
            _preferences.value = ReminderPreferences()
            return
        }
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: run {
            _preferences.value = ReminderPreferences()
            return
        }
        _preferences.value = ReminderPreferences(
            globalEnabled = obj.optBoolean("globalEnabled", true),
            quietHoursEnabled = obj.optBoolean("quietHoursEnabled", false),
            quietStartHour = obj.optInt("quietStartHour", 22),
            quietStartMinute = obj.optInt("quietStartMinute", 0),
            quietEndHour = obj.optInt("quietEndHour", 7),
            quietEndMinute = obj.optInt("quietEndMinute", 0),
            vibrateEnabled = obj.optBoolean("vibrateEnabled", true),
            showActionButtons = obj.optBoolean("showActionButtons", true),
            defaultSnoozeMinutes = obj.optInt("defaultSnoozeMinutes", 10),
            defaultSoundOption = ReminderSoundOption.fromId(obj.optString("defaultSoundOption", "default")),
            defaultCustomSoundUri = obj.optString("defaultCustomSoundUri", "").ifBlank { null },
            defaultCustomSoundLabel = obj.optString("defaultCustomSoundLabel", "").ifBlank { null }
        )
    }

    private fun persistPreferencesLocked(sp: SharedPreferences) {
        val p = _preferences.value
        val obj = JSONObject().apply {
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
        sp.edit().putString(KEY_PREFS, obj.toString()).apply()
    }

    private fun loadHistoryLocked(sp: SharedPreferences) {
        val raw = sp.getString(KEY_HISTORY, null)
        if (raw.isNullOrBlank()) {
            _history.value = emptyList()
            return
        }
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: run {
            _history.value = emptyList()
            return
        }
        _history.value = buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    NotificationHistoryEntry(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        reminderId = obj.optString("reminderId"),
                        title = obj.optString("title"),
                        message = obj.optString("message"),
                        status = NotificationHistoryStatus.fromId(obj.optString("status", "triggered")),
                        timestampMs = obj.optLong("timestampMs", 0L)
                    )
                )
            }
        }
    }

    private fun persistHistoryLocked(sp: SharedPreferences) {
        val arr = JSONArray()
        _history.value.forEach { entry ->
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
        sp.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }
}
