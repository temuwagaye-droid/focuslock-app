package com.example.notifications

import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class ReminderRepeatMode(val id: String, val label: String) {
    ONCE("once", "Once"),
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly");

    companion object {
        fun fromId(id: String): ReminderRepeatMode =
            entries.firstOrNull { it.id == id } ?: DAILY
    }
}

enum class ReminderSoundOption(val id: String, val label: String) {
    DEFAULT("default", "Default Chime"),
    ALARM("alarm", "Gentle Alarm"),
    BELL("bell", "Soft Bell"),
    SILENT("silent", "Silent"),
    CUSTOM("custom", "Custom Sound");

    companion object {
        fun fromId(id: String): ReminderSoundOption =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

enum class NotificationHistoryStatus(val id: String, val label: String) {
    TRIGGERED("triggered", "Triggered"),
    SNOOZED("snoozed", "Snoozed"),
    COMPLETED("completed", "Completed"),
    DISMISSED("dismissed", "Dismissed"),
    QUIET_SUPPRESSED("quiet_suppressed", "Quiet Hours");

    companion object {
        fun fromId(id: String): NotificationHistoryStatus =
            entries.firstOrNull { it.id == id } ?: TRIGGERED
    }
}

data class ReadingReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val hour: Int,
    val minute: Int,
    val repeatMode: ReminderRepeatMode = ReminderRepeatMode.DAILY,
    val enabled: Boolean = true,
    val completedAtMs: Long? = null,
    val snoozedUntilMs: Long? = null,
    val soundOption: ReminderSoundOption = ReminderSoundOption.DEFAULT,
    val customSoundUri: String? = null,
    val customSoundLabel: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    fun formattedTime(): String =
        String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun formattedDate(): String =
        String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month.coerceIn(1, 12), dayOfMonth.coerceIn(1, 31))

    fun effectiveSoundLabel(): String =
        if (soundOption == ReminderSoundOption.CUSTOM && !customSoundLabel.isNullOrBlank()) {
            customSoundLabel
        } else {
            soundOption.label
        }
}

data class NotificationHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val reminderId: String,
    val title: String,
    val message: String,
    val status: NotificationHistoryStatus,
    val timestampMs: Long = System.currentTimeMillis()
)

data class ReminderPreferences(
    val globalEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 7,
    val quietEndMinute: Int = 0,
    val vibrateEnabled: Boolean = true,
    val showActionButtons: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val defaultSoundOption: ReminderSoundOption = ReminderSoundOption.DEFAULT,
    val defaultCustomSoundUri: String? = null,
    val defaultCustomSoundLabel: String? = null
) {
    fun isWithinQuietHours(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!quietHoursEnabled) return false
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return isMinuteWithinWindow(minuteOfDay)
    }

    fun isMinuteWithinWindow(minuteOfDay: Int): Boolean {
        if (!quietHoursEnabled) return false
        val start = quietStartHour.coerceIn(0, 23) * 60 + quietStartMinute.coerceIn(0, 59)
        val end = quietEndHour.coerceIn(0, 23) * 60 + quietEndMinute.coerceIn(0, 59)
        return if (start == end) {
            true
        } else if (start < end) {
            minuteOfDay in start until end
        } else {
            minuteOfDay >= start || minuteOfDay < end
        }
    }

    fun formattedQuietWindow(): String = String.format(
        Locale.getDefault(),
        "%02d:%02d – %02d:%02d",
        quietStartHour.coerceIn(0, 23),
        quietStartMinute.coerceIn(0, 59),
        quietEndHour.coerceIn(0, 23),
        quietEndMinute.coerceIn(0, 59)
    )
}
