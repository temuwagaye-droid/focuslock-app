package com.example.bedtime

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class BedtimeSchedule(
    val isEnabled: Boolean = false,
    val bedtimeHour: Int = 22,    // 10:00 PM
    val bedtimeMinute: Int = 0,
    val wakeUpHour: Int = 6,      // 6:00 AM
    val wakeUpMinute: Int = 0,
    val activeDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    ),
    val advanceReminderMinutes: Int = 30, // 15, 30, or 60
    val strictMode: Boolean = false,
    val emergencyCooldownMinutes: Int = 5
)

data class BedtimeStats(
    val currentStreakDays: Int = 0,
    val weeklyAdherencePercent: Int = 0,
    val totalBedtimeHoursTracked: Float = 0f,
    val earlyExitCount: Int = 0
)

object BedtimeManager {
    private const val PREFS_NAME = "focuslock_bedtime_prefs"
    private const val KEY_ENABLED = "bedtime_enabled"
    private const val KEY_BEDTIME_HOUR = "bedtime_hour"
    private const val KEY_BEDTIME_MINUTE = "bedtime_minute"
    private const val KEY_WAKEUP_HOUR = "wakeup_hour"
    private const val KEY_WAKEUP_MINUTE = "wakeup_minute"
    private const val KEY_ACTIVE_DAYS = "active_days"
    private const val KEY_REMINDER_MINS = "reminder_mins"
    private const val KEY_STRICT = "strict_mode"
    private const val KEY_LOCK_ACTIVE = "lock_active"
    private const val KEY_EMERGENCY_UNTIL = "emergency_until"
    private const val KEY_STREAK = "bedtime_streak"
    private const val KEY_EARLY_EXITS = "early_exits"

    private val _schedule = MutableStateFlow(BedtimeSchedule())
    val schedule: StateFlow<BedtimeSchedule> = _schedule.asStateFlow()

    private val _isBedtimeLockActive = MutableStateFlow(false)
    val isBedtimeLockActive: StateFlow<Boolean> = _isBedtimeLockActive.asStateFlow()

    private val _stats = MutableStateFlow(BedtimeStats())
    val stats: StateFlow<BedtimeStats> = _stats.asStateFlow()

    private val _emergencyUnlockActive = MutableStateFlow(false)
    val emergencyUnlockActive: StateFlow<Boolean> = _emergencyUnlockActive.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean(KEY_ENABLED, false)
            val bedtimeHour = prefs.getInt(KEY_BEDTIME_HOUR, 22)
            val bedtimeMinute = prefs.getInt(KEY_BEDTIME_MINUTE, 0)
            val wakeUpHour = prefs.getInt(KEY_WAKEUP_HOUR, 6)
            val wakeUpMinute = prefs.getInt(KEY_WAKEUP_MINUTE, 0)
            val rawDays = prefs.getString(KEY_ACTIVE_DAYS, "2,3,4,5,6") ?: "2,3,4,5,6"
            val days = rawDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            val reminder = prefs.getInt(KEY_REMINDER_MINS, 30)
            val strict = prefs.getBoolean(KEY_STRICT, false)

            _schedule.value = BedtimeSchedule(
                isEnabled = isEnabled,
                bedtimeHour = bedtimeHour,
                bedtimeMinute = bedtimeMinute,
                wakeUpHour = wakeUpHour,
                wakeUpMinute = wakeUpMinute,
                activeDays = if (days.isEmpty()) setOf(2, 3, 4, 5, 6) else days,
                advanceReminderMinutes = reminder,
                strictMode = strict
            )

            val lockActive = prefs.getBoolean(KEY_LOCK_ACTIVE, false)
            val emergencyUntil = prefs.getLong(KEY_EMERGENCY_UNTIL, 0L)
            val now = System.currentTimeMillis()

            if (emergencyUntil > now) {
                _emergencyUnlockActive.value = true
                _isBedtimeLockActive.value = false
            } else {
                _emergencyUnlockActive.value = false
                if (isEnabled) {
                    _isBedtimeLockActive.value = checkShouldLockNow(_schedule.value)
                } else {
                    _isBedtimeLockActive.value = false
                }
            }

            val streak = prefs.getInt(KEY_STREAK, 3)
            val earlyExits = prefs.getInt(KEY_EARLY_EXITS, 0)
            _stats.value = BedtimeStats(
                currentStreakDays = streak,
                weeklyAdherencePercent = 85,
                totalBedtimeHoursTracked = (streak * 7.5f),
                earlyExitCount = earlyExits
            )
        }
    }

    fun updateSchedule(context: Context, newSchedule: BedtimeSchedule) {
        _schedule.value = newSchedule
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, newSchedule.isEnabled)
            .putInt(KEY_BEDTIME_HOUR, newSchedule.bedtimeHour)
            .putInt(KEY_BEDTIME_MINUTE, newSchedule.bedtimeMinute)
            .putInt(KEY_WAKEUP_HOUR, newSchedule.wakeUpHour)
            .putInt(KEY_WAKEUP_MINUTE, newSchedule.wakeUpMinute)
            .putString(KEY_ACTIVE_DAYS, newSchedule.activeDays.joinToString(","))
            .putInt(KEY_REMINDER_MINS, newSchedule.advanceReminderMinutes)
            .putBoolean(KEY_STRICT, newSchedule.strictMode)
            .apply()

        evaluateCurrentStatus(context)
    }

    fun evaluateCurrentStatus(context: Context) {
        val s = _schedule.value
        if (!s.isEnabled) {
            setLockActive(context, false)
            return
        }
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val emergencyUntil = prefs.getLong(KEY_EMERGENCY_UNTIL, 0L)
        if (emergencyUntil > now) {
            _emergencyUnlockActive.value = true
            setLockActive(context, false)
            return
        } else {
            _emergencyUnlockActive.value = false
        }

        val shouldLock = checkShouldLockNow(s)
        setLockActive(context, shouldLock)
    }

    fun setLockActive(context: Context, active: Boolean) {
        _isBedtimeLockActive.value = active
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOCK_ACTIVE, active).apply()
    }

    fun triggerEmergencyOverride(context: Context, minutes: Int = 5) {
        val until = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_EMERGENCY_UNTIL, until).apply()
        _emergencyUnlockActive.value = true
        setLockActive(context, false)
    }

    fun earlyExitBedtimeLock(context: Context) {
        setLockActive(context, false)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentExits = prefs.getInt(KEY_EARLY_EXITS, 0) + 1
        prefs.edit().putInt(KEY_EARLY_EXITS, currentExits).apply()
        _stats.value = _stats.value.copy(earlyExitCount = currentExits)
    }

    fun checkShouldLockNow(s: BedtimeSchedule): Boolean {
        if (!s.isEnabled) return false
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val bedtimeMinutes = s.bedtimeHour * 60 + s.bedtimeMinute
        val wakeUpMinutes = s.wakeUpHour * 60 + s.wakeUpMinute

        if (bedtimeMinutes <= wakeUpMinutes) {
            // Same-day lock (e.g. 1:00 AM to 7:00 AM)
            if (!s.activeDays.contains(currentDay)) return false
            return currentMinutes in bedtimeMinutes until wakeUpMinutes
        } else {
            // Crosses midnight (e.g. 10:00 PM to 6:00 AM)
            // Evening part (current >= bedtime)
            if (currentMinutes >= bedtimeMinutes) {
                return s.activeDays.contains(currentDay)
            }
            // Morning part (current < wakeUp) -> check previous day was active
            if (currentMinutes < wakeUpMinutes) {
                val prevDay = if (currentDay == Calendar.SUNDAY) Calendar.SATURDAY else currentDay - 1
                return s.activeDays.contains(prevDay)
            }
            return false
        }
    }

    fun getTimeUntilNextBedtimeSeconds(): Long {
        val s = _schedule.value
        val cal = Calendar.getInstance()
        val nowMillis = cal.timeInMillis

        var targetCal = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, s.bedtimeHour)
            set(Calendar.MINUTE, s.bedtimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCal.timeInMillis <= nowMillis) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Advance until on an active day
        var loops = 0
        while (!s.activeDays.contains(targetCal.get(Calendar.DAY_OF_WEEK)) && loops < 7) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
            loops++
        }

        val diff = (targetCal.timeInMillis - nowMillis) / 1000L
        return diff.coerceAtLeast(0L)
    }

    fun getTimeUntilWakeUpSeconds(): Long {
        val s = _schedule.value
        val cal = Calendar.getInstance()
        val nowMillis = cal.timeInMillis

        var targetCal = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, s.wakeUpHour)
            set(Calendar.MINUTE, s.wakeUpMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCal.timeInMillis <= nowMillis) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diff = (targetCal.timeInMillis - nowMillis) / 1000L
        return diff.coerceAtLeast(0L)
    }
}
