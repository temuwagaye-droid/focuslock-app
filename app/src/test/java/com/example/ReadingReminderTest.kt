package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.NotificationHelper
import com.example.notifications.NotificationHistoryStatus
import com.example.notifications.ReadingReminderStore
import com.example.notifications.ReminderPreferences
import com.example.notifications.ReminderRepeatMode
import com.example.notifications.ReminderScheduler
import com.example.notifications.ReminderSoundOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReadingReminderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ReadingReminderStore.clearAllForTesting(context)
    }

    @Test
    fun `create edit toggle and delete reminder persists across reload`() {
        val created = ReadingReminderStore.createReminder(
            context = context,
            title = "Morning Chapter",
            note = "15 mins Kindle",
            year = 2026,
            month = 11,
            dayOfMonth = 20,
            hour = 7,
            minute = 30,
            repeatMode = ReminderRepeatMode.DAILY,
            soundOption = ReminderSoundOption.BELL,
            customSoundUri = null,
            customSoundLabel = null
        )
        assertEquals(1, ReadingReminderStore.reminders.value.size)
        assertEquals("Morning Chapter", created.title)
        assertEquals(ReminderSoundOption.BELL, created.soundOption)

        val edited = created.copy(
            title = "Evening Deep Read",
            hour = 21,
            minute = 15,
            repeatMode = ReminderRepeatMode.WEEKLY,
            soundOption = ReminderSoundOption.CUSTOM,
            customSoundUri = "content://media/internal/audio/media/42",
            customSoundLabel = "Zen Gong"
        )
        ReadingReminderStore.updateReminder(context, edited)
        ReadingReminderStore.reloadFromPrefs(context)

        val fetched = ReadingReminderStore.getReminderById(context, created.id)
        assertNotNull(fetched)
        assertEquals("Evening Deep Read", fetched?.title)
        assertEquals(21, fetched?.hour)
        assertEquals(15, fetched?.minute)
        assertEquals(ReminderRepeatMode.WEEKLY, fetched?.repeatMode)
        assertEquals("Zen Gong", fetched?.effectiveSoundLabel())

        ReadingReminderStore.setReminderEnabled(context, created.id, false)
        assertFalse(ReadingReminderStore.getReminderById(context, created.id)!!.enabled)

        ReadingReminderStore.deleteReminder(context, created.id)
        assertTrue(ReadingReminderStore.reminders.value.isEmpty())
    }

    @Test
    fun `daily weekly and monthly repetition advance accurately including month end clamping`() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromAfterBase = baseCal.timeInMillis + 1_000L

        val dailyItem = ReadingReminderStore.createReminder(
            context = context,
            title = "Daily",
            note = "",
            year = 2026,
            month = 1,
            dayOfMonth = 31,
            hour = 9,
            minute = 0,
            repeatMode = ReminderRepeatMode.DAILY,
            soundOption = ReminderSoundOption.DEFAULT
        )
        val nextDaily = ReminderScheduler.advanceCalendar(dailyItem, fromAfterBase)
        assertEquals(Calendar.FEBRUARY, nextDaily.get(Calendar.MONTH))
        assertEquals(1, nextDaily.get(Calendar.DAY_OF_MONTH))

        val weeklyItem = dailyItem.copy(repeatMode = ReminderRepeatMode.WEEKLY)
        val nextWeekly = ReminderScheduler.advanceCalendar(weeklyItem, fromAfterBase)
        assertEquals(Calendar.FEBRUARY, nextWeekly.get(Calendar.MONTH))
        assertEquals(7, nextWeekly.get(Calendar.DAY_OF_MONTH))

        val monthlyItem = dailyItem.copy(repeatMode = ReminderRepeatMode.MONTHLY)
        val nextMonthly = ReminderScheduler.advanceCalendar(monthlyItem, fromAfterBase)
        assertEquals(Calendar.FEBRUARY, nextMonthly.get(Calendar.MONTH))
        assertEquals(28, nextMonthly.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `snooze complete and dismiss actions update state and log notification history`() {
        val item = ReadingReminderStore.createReminder(
            context = context,
            title = "Night Reading",
            note = "",
            year = 2026,
            month = 11,
            dayOfMonth = 20,
            hour = 20,
            minute = 0,
            repeatMode = ReminderRepeatMode.ONCE,
            soundOption = ReminderSoundOption.ALARM
        )

        val snoozed = ReadingReminderStore.snoozeReminder(context, item.id, snoozeMinutes = 15)
        assertNotNull(snoozed?.snoozedUntilMs)
        assertEquals(NotificationHistoryStatus.SNOOZED, ReadingReminderStore.history.value.first().status)

        val completed = ReadingReminderStore.markReminderCompleted(context, item.id)
        assertNotNull(completed?.completedAtMs)
        assertFalse(completed!!.enabled)
        assertEquals(NotificationHistoryStatus.COMPLETED, ReadingReminderStore.history.value.first().status)

        val repeating = ReadingReminderStore.createReminder(
            context = context,
            title = "Recurring Read",
            note = "",
            year = 2026,
            month = 11,
            dayOfMonth = 20,
            hour = 20,
            minute = 0,
            repeatMode = ReminderRepeatMode.DAILY,
            soundOption = ReminderSoundOption.DEFAULT
        )
        val dismissed = ReadingReminderStore.dismissReminder(context, repeating.id)
        assertTrue(dismissed!!.enabled)
        assertEquals(NotificationHistoryStatus.DISMISSED, ReadingReminderStore.history.value.first().status)

        ReadingReminderStore.clearHistory(context)
        assertTrue(ReadingReminderStore.history.value.isEmpty())
    }

    @Test
    fun `quiet hours evaluate overnight and daytime windows and suppress notifications`() {
        val overnight = ReminderPreferences(
            quietHoursEnabled = true,
            quietStartHour = 22,
            quietStartMinute = 0,
            quietEndHour = 7,
            quietEndMinute = 0
        )
        assertTrue(overnight.isMinuteWithinWindow(23 * 60 + 30))
        assertTrue(overnight.isMinuteWithinWindow(6 * 60 + 59))
        assertFalse(overnight.isMinuteWithinWindow(7 * 60))
        assertFalse(overnight.isMinuteWithinWindow(15 * 60))

        val daytime = ReminderPreferences(
            quietHoursEnabled = true,
            quietStartHour = 13,
            quietStartMinute = 0,
            quietEndHour = 15,
            quietEndMinute = 0
        )
        assertTrue(daytime.isMinuteWithinWindow(13 * 60 + 45))
        assertFalse(daytime.isMinuteWithinWindow(15 * 60))

        // Always-quiet test window via custom notification trigger
        val cal = Calendar.getInstance()
        val alwaysQuietPrefs = ReminderPreferences(
            globalEnabled = true,
            quietHoursEnabled = true,
            quietStartHour = (cal.get(Calendar.HOUR_OF_DAY) + 23) % 24,
            quietStartMinute = 0,
            quietEndHour = (cal.get(Calendar.HOUR_OF_DAY) + 2) % 24,
            quietEndMinute = 0
        )
        val reminder = ReadingReminderStore.createReminder(
            context = context,
            title = "Quiet Test",
            note = "",
            year = 2026,
            month = 11,
            dayOfMonth = 20,
            hour = 20,
            minute = 0,
            repeatMode = ReminderRepeatMode.DAILY,
            soundOption = ReminderSoundOption.SILENT
        )
        assertNull(NotificationHelper.resolveSoundUri(ReminderSoundOption.SILENT, null))
        NotificationHelper.showCustomReminderNotification(context, reminder, alwaysQuietPrefs)
        assertEquals(NotificationHistoryStatus.QUIET_SUPPRESSED, ReadingReminderStore.history.value.first().status)
    }
}
