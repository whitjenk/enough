package com.enough.app.domain.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The back-off contract from SPEC §7.8. These tests exist to make the
 * anti-escalation promise in `CLAUDE.md` a thing that breaks the build if it is
 * ever quietly reversed — the failure mode being guarded against is a future
 * "just bump the frequency for retention" change.
 */
class ReminderScheduleTest {

    private val today = 20_000L

    private fun fire(
        enabled: Boolean = true,
        unopened: Int = 0,
        lastFired: Long? = null,
        lastOpened: Long? = null,
        day: Long = today,
    ) = ReminderSchedule.shouldFire(enabled, unopened, lastFired, lastOpened, day)

    @Test
    fun `disabled never fires`() {
        assertFalse(fire(enabled = false))
        assertFalse(fire(enabled = false, unopened = 0, lastFired = today - 30))
    }

    @Test
    fun `fires once on a fresh day`() {
        assertTrue(fire(lastFired = today - 1))
    }

    @Test
    fun `does not fire twice on the same day`() {
        assertFalse(fire(lastFired = today))
    }

    @Test
    fun `does not fire when the person already opened the app today`() {
        // A reminder is for someone who hasn't been here. Showing up on your own
        // should never be answered with a nudge to show up.
        assertFalse(fire(lastFired = today - 1, lastOpened = today))
    }

    @Test
    fun `cadence quiets as the app goes unopened, and never gets louder`() {
        assertEquals(ReminderCadence.DAILY, ReminderSchedule.cadenceFor(0))
        assertEquals(ReminderCadence.DAILY, ReminderSchedule.cadenceFor(4))
        assertEquals(ReminderCadence.WEEKLY, ReminderSchedule.cadenceFor(5))
        assertEquals(ReminderCadence.WEEKLY, ReminderSchedule.cadenceFor(7))
        assertEquals(ReminderCadence.STOPPED, ReminderSchedule.cadenceFor(8))
        assertEquals(ReminderCadence.STOPPED, ReminderSchedule.cadenceFor(500))
    }

    @Test
    fun `weekly cadence waits a full week between fires`() {
        val unopened = ReminderSchedule.UNOPENED_BEFORE_WEEKLY
        assertFalse(fire(unopened = unopened, lastFired = today - 6))
        assertTrue(fire(unopened = unopened, lastFired = today - 7))
    }

    @Test
    fun `stopped stays stopped no matter how much time passes`() {
        val unopened = ReminderSchedule.UNOPENED_BEFORE_STOP
        assertFalse(fire(unopened = unopened, lastFired = today - 1))
        assertFalse(fire(unopened = unopened, lastFired = today - 365))
        assertFalse(fire(unopened = unopened, lastFired = null))
    }

    @Test
    fun `only opening the app brings it back, and it comes back all the way`() {
        val silenced = ReminderSchedule.UNOPENED_BEFORE_STOP + 20
        assertEquals(ReminderCadence.STOPPED, ReminderSchedule.cadenceFor(silenced))

        val afterReturn = ReminderSchedule.afterAppOpened()

        assertEquals(0, afterReturn)
        assertEquals(ReminderCadence.DAILY, ReminderSchedule.cadenceFor(afterReturn))
        // No penalty, no re-earning, no probation period — same as a missed day
        // anywhere else in this app.
        assertTrue(fire(unopened = afterReturn, lastFired = today - 400))
    }

    @Test
    fun `firing walks the full path from daily to silence and stops there`() {
        var unopened = 0
        var fires = 0
        var lastFired: Long? = null
        var day = today

        // Simulate a person who installs the app and never opens it again.
        repeat(400) {
            if (ReminderSchedule.shouldFire(
                    enabled = true,
                    unopenedCount = unopened,
                    lastFiredEpochDay = lastFired,
                    lastOpenedEpochDay = null,
                    todayEpochDay = day,
                )
            ) {
                fires++
                lastFired = day
                unopened = ReminderSchedule.afterFiring(unopened)
            }
            day++
        }

        // Over more than a year of being ignored, the app speaks a handful of
        // times and then goes quiet for good. If this number ever grows, the
        // change is an escalation and it is the wrong change.
        assertEquals(ReminderSchedule.UNOPENED_BEFORE_STOP, fires)
        assertEquals(ReminderCadence.STOPPED, ReminderSchedule.cadenceFor(unopened))
    }
}
