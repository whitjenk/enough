package com.enough.app.feature.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderSchedulerTest {

    private val zone = ZoneId.of("America/New_York")

    private fun at(hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 8, 24, hour, minute, 0, 0, zone)

    @Test
    fun `waits until later the same day when the time is still ahead`() {
        val delay = ReminderScheduler.delayUntilNext(16 * 60, at(9))
        assertEquals(7 * 60, delay.toMinutes())
    }

    @Test
    fun `rolls to tomorrow once today's time has passed`() {
        val delay = ReminderScheduler.delayUntilNext(16 * 60, at(18))
        assertEquals(22 * 60, delay.toMinutes())
    }

    @Test
    fun `the exact minute counts as passed, so it cannot fire twice`() {
        val delay = ReminderScheduler.delayUntilNext(16 * 60, at(16, 0))
        assertEquals(24 * 60, delay.toMinutes())
    }

    @Test
    fun `handles a time earlier in the day than now`() {
        val delay = ReminderScheduler.delayUntilNext(9 * 60, at(20))
        assertEquals(13 * 60, delay.toMinutes())
    }
}
