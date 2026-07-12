package com.enough.app.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetMomentTest {

    private val today = 20_000L

    @Test
    fun `absence of three or more days triggers the reset moment`() {
        assertTrue(
            ResetMoment.shouldShow(
                todayEpochDay = today,
                daysSinceLastLog = 3,
                hadWideMissYesterday = false,
                lastShownEpochDay = null,
            ),
        )
    }

    @Test
    fun `fewer than three quiet days does not trigger`() {
        assertFalse(
            ResetMoment.shouldShow(
                todayEpochDay = today,
                daysSinceLastLog = 2,
                hadWideMissYesterday = false,
                lastShownEpochDay = null,
            ),
        )
    }

    @Test
    fun `a wide miss on the last completed day triggers even without absence`() {
        assertTrue(
            ResetMoment.shouldShow(
                todayEpochDay = today,
                daysSinceLastLog = 0,
                hadWideMissYesterday = true,
                lastShownEpochDay = null,
            ),
        )
    }

    @Test
    fun `never-logged user does not get a reset moment`() {
        assertFalse(
            ResetMoment.shouldShow(
                todayEpochDay = today,
                daysSinceLastLog = null,
                hadWideMissYesterday = false,
                lastShownEpochDay = null,
            ),
        )
    }

    @Test
    fun `stays visible for the rest of the day once shown`() {
        // Already shown today; even if the raw trigger math would now suppress
        // (e.g. episode already fired), it keeps showing for today.
        assertTrue(
            ResetMoment.shouldShow(
                todayEpochDay = today,
                daysSinceLastLog = 5,
                hadWideMissYesterday = false,
                lastShownEpochDay = today,
            ),
        )
    }

    @Test
    fun `does not re-fire on a later day of the same quiet episode`() {
        // Shown yesterday (today-1) during a quiet spell; still quiet today with
        // no new log in between -> stays silent, never escalating.
        assertFalse(
            ResetMoment.shouldShow(
                todayEpochDay = today,
                daysSinceLastLog = 6,
                hadWideMissYesterday = false,
                lastShownEpochDay = today - 1,
            ),
        )
    }

    @Test
    fun `re-enables for a new rough patch after the person logs again`() {
        // Last shown long ago (day 100). The person has since logged; last log is
        // recent (2 days ago) and they've gone quiet again -> a genuinely new
        // episode, so it may fire.
        assertTrue(
            ResetMoment.shouldShow(
                todayEpochDay = today,
                daysSinceLastLog = 3,
                hadWideMissYesterday = false,
                lastShownEpochDay = 100L,
            ),
        )
    }
}
