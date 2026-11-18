package com.example.notifications

import java.util.Calendar
import java.util.Locale

enum class ReminderRepeatMode(val id: String, val label: String) {
    ONCE("once", "Once"),
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly");

    companion object {
        fun fromId(id: String?): ReminderRepeatMode =
            entries.firstOrNull { it.id == id } ?: DAILY
    }
}

enum class ReminderSoundOption(val id: String, val label: String) {
    DEFAULT("default", "FocusLock Chime"),
    ALARM("alarm", "Gentle Wake"),
    BELL("bell", "Soft Bell"),
    SILENT("silent", "Silent"),
    CUSTOM("custom", "Device Tone");

    companion object {
        fun fromId(id: String?): ReminderSoundOption =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

enum class NotificationHistoryStatus(val id: String, val label: String) {
    TRIGGERED("triggered", "Delivered"),
    SNOOZED("snoozed", "Snoozed"),
    COMPLETED("completed", "Completed"),
    DISMISSED("dismissed", "Dismissed"),
    QUIET_SUPPRESSED("quiet_suppressed", "Muted (Quiet Hours)");

    companion object {
        fun fromId(id: String?): NotificationHistoryStatus =
            entries.firstOrNull { it.id == id } ?: TRIGGERED
    }
}

data class ReadingReminderItem(
    val id: String,
    val title: String,
    val note: String = "",
    val year: Int,
    val month: Int, // 1..12
    val dayOfMonth: Int, // 1..31
    val hour: Int, // 0..23
    val minute: Int, // 0..59
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

    fun formattedDate(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
        }
        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "M$month"
        return "$monthName ${cal.get(Calendar.DAY_OF_MONTH)}, $year"
    }

    fun effectiveSoundLabel(): String = when (soundOption) {
        ReminderSoundOption.CUSTOM -> customSoundLabel?.takeIf { it.isNotBlank() } ?: soundOption.label
        else -> soundOption.label
    }
}

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
    fun isWithinQuietHours(timeMillis: Long = System.currentTimeMillis()): Boolean {
        if (!quietHoursEnabled) return false
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return isMinuteWithinWindow(nowMinutes)
    }

    fun isMinuteWithinWindow(totalMinutesOfDay: Int): Boolean {
        if (!quietHoursEnabled) return false
        val start = quietStartHour.coerceIn(0, 23) * 60 + quietStartMinute.coerceIn(0, 59)
        val end = quietEndHour.coerceIn(0, 23) * 60 + quietEndMinute.coerceIn(0, 59)
        if (start == end) return false
        return if (start < end) {
            totalMinutesOfDay in start until end
        } else {
            totalMinutesOfDay >= start || totalMinutesOfDay < end
        }
    }

    fun formattedQuietWindow(): String {
        val s = String.format(Locale.getDefault(), "%02d:%02d", quietStartHour, quietStartMinute)
        val e = String.format(Locale.getDefault(), "%02d:%02d", quietEndHour, quietEndMinute)
        return "$s – $e"
    }
}

data class NotificationHistoryEntry(
    val id: String,
    val reminderId: String,
    val title: String,
    val message: String,
    val status: NotificationHistoryStatus,
    val timestampMs: Long = System.currentTimeMillis()
)
