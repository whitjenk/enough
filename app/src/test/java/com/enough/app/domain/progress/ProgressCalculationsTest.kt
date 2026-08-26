package com.enough.app.domain.progress

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.model.FeltLevel
import com.enough.app.data.model.MealSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ProgressCalculationsTest {

    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 7, 11)
    private val now = today.atTime(12, 0).atZone(zone).toInstant()

    private fun mealOn(date: LocalDate, fiber: Double, servings: Double = 1.0): MealWithFood {
        val food = Food(id = 1, name = "F", servingLabel = "1", carbsG = 20.0, fiberG = fiber, proteinG = 3.0)
        val ts = date.atTime(LocalTime.NOON).atZone(zone).toInstant()
        return MealWithFood(
            MealEntry(id = 0, foodId = 1, servingsMultiplier = servings, timestamp = ts, source = MealSource.TEXT),
            food,
        )
    }

    @Test
    fun `fiber is grouped and summed by calendar day`() {
        val meals = listOf(
            mealOn(today, 5.0),
            mealOn(today, 3.0, servings = 2.0), // +6
            mealOn(today.minusDays(1), 4.0),
        )
        val byDay = ProgressCalculations.fiberByDay(meals, zone)
        assertEquals(11.0, byDay[today]!!, 1e-9)
        assertEquals(4.0, byDay[today.minusDays(1)]!!, 1e-9)
    }

    @Test
    fun `fiber series is full-length, oldest-first, and zero-filled`() {
        val meals = listOf(mealOn(today, 10.0), mealOn(today.minusDays(2), 6.0))
        val series = ProgressCalculations.fiberSeries(7, meals, now, zone)

        assertEquals(7, series.size)
        assertEquals(today.minusDays(6), series.first().date)
        assertEquals(today, series.last().date)
        assertEquals(10.0, series.last().fiberG, 1e-9)
        assertEquals(6.0, series[4].fiberG, 1e-9) // two days ago
        assertEquals(0.0, series[5].fiberG, 1e-9) // yesterday, nothing logged
    }

    @Test
    fun `days logged counts distinct days in the window, not total logs`() {
        val instants = listOf(
            today.atTime(8, 0).atZone(zone).toInstant(),
            today.atTime(19, 0).atZone(zone).toInstant(), // same day, second log
            today.minusDays(3).atTime(9, 0).atZone(zone).toInstant(),
            today.minusDays(10).atTime(9, 0).atZone(zone).toInstant(), // outside 7-day window
        )
        assertEquals(2, ProgressCalculations.daysLoggedInLast(7, instants, now, zone))
    }

    @Test
    fun `logged-day series marks the right days oldest-first`() {
        val instants = listOf(
            today.atTime(8, 0).atZone(zone).toInstant(),
            today.minusDays(3).atTime(9, 0).atZone(zone).toInstant(),
        )
        val series = ProgressCalculations.loggedDaySeries(7, instants, now, zone)
        // Oldest-first over 7 days: indices 0..6 map to today-6 .. today.
        assertEquals(listOf(false, false, false, true, false, false, true), series)
    }

    @Test
    fun `empty history yields zero days logged and an all-zero series`() {
        assertEquals(0, ProgressCalculations.daysLoggedInLast(7, emptyList(), now, zone))
        assertEquals(0.0, ProgressCalculations.fiberSeries(7, emptyList(), now, zone).sumOf { it.fiberG }, 1e-9)
    }

    @Test
    fun `felt series is oldest-first and a skipped day is null, never a missed marker`() {
        val feltByDay = mapOf(
            today to FeltLevel.GOOD,
            today.minusDays(3) to FeltLevel.ROUGH,
        )
        val series = ProgressCalculations.feltSeries(7, feltByDay, now, zone)

        assertEquals(7, series.size)
        assertEquals(FeltLevel.GOOD, series.last()) // today
        assertEquals(FeltLevel.ROUGH, series[3]) // three days ago
        assertNull(series[5]) // a skipped day is simply null
        // Only the days that were actually checked in count — skips don't count against anything.
        assertEquals(2, ProgressCalculations.checkInDaysInLast(7, feltByDay, now, zone))
    }

    @Test
    fun `check-ins are independent of the logging consistency series`() {
        // A day with only a felt check-in (no meal/activity/weight) is NOT counted
        // as a "logged" day, and a logged day with no check-in is NOT counted as a
        // check-in day: the two loops never contaminate each other, so skipping the
        // optional check-in can't change the consistency view either way.
        val checkInOnlyDay = today.minusDays(1)
        val feltByDay = mapOf(checkInOnlyDay to FeltLevel.STEADY)
        val logInstants = listOf(today.atTime(9, 0).atZone(zone).toInstant()) // a real log, different day

        assertEquals(1, ProgressCalculations.daysLoggedInLast(7, logInstants, now, zone))
        assertEquals(1, ProgressCalculations.checkInDaysInLast(7, feltByDay, now, zone))
        // The logged-day series has today true, the check-in-only day false.
        val logged = ProgressCalculations.loggedDaySeries(7, logInstants, now, zone)
        assertEquals(true, logged.last()) // today, logged a meal
        assertEquals(false, logged[5]) // yesterday, only a check-in — not a "logged" day
    }

    @Test
    fun `empty check-ins yield a null series and a zero count`() {
        val series = ProgressCalculations.feltSeries(7, emptyMap(), now, zone)
        assertEquals(7, series.size)
        assertEquals(true, series.all { it == null })
        assertEquals(0, ProgressCalculations.checkInDaysInLast(7, emptyMap(), now, zone))
    }

    // --- visibleWindowDays: days before the install are absent, not misses ---

    @Test
    fun `window is one day long on the day the goal was created`() {
        assertEquals(1, ProgressCalculations.visibleWindowDays(7, today, now, zone))
    }

    @Test
    fun `window grows one day at a time until it reaches the full length`() {
        assertEquals(2, ProgressCalculations.visibleWindowDays(7, today.minusDays(1), now, zone))
        assertEquals(3, ProgressCalculations.visibleWindowDays(7, today.minusDays(2), now, zone))
        assertEquals(6, ProgressCalculations.visibleWindowDays(7, today.minusDays(5), now, zone))
    }

    @Test
    fun `window never exceeds the requested length once history is older`() {
        assertEquals(7, ProgressCalculations.visibleWindowDays(7, today.minusDays(6), now, zone))
        assertEquals(7, ProgressCalculations.visibleWindowDays(7, today.minusDays(90), now, zone))
    }

    @Test
    fun `a start date in the future is treated as day one, never a negative window`() {
        assertEquals(1, ProgressCalculations.visibleWindowDays(7, today.plusDays(3), now, zone))
    }

    @Test
    fun `an unknown start date falls back to the full window`() {
        assertEquals(7, ProgressCalculations.visibleWindowDays(7, null, now, zone))
    }

    @Test
    fun `a fresh install reports no missed days at all`() {
        // The bug this guards: a day-one install rendered six hollow dots and
        // "1 of the last 7 days", blaming the person for days the app did not exist.
        val window = ProgressCalculations.visibleWindowDays(7, today, now, zone)
        val series = ProgressCalculations.loggedDaySeries(
            window,
            listOf(today.atTime(LocalTime.NOON).atZone(zone).toInstant()),
            now,
            zone,
        )
        assertEquals(listOf(true), series)
        assertEquals(0, series.count { !it })
    }

    @Test
    fun `a fresh install with nothing logged shows one empty day, not seven`() {
        val window = ProgressCalculations.visibleWindowDays(7, today, now, zone)
        val series = ProgressCalculations.loggedDaySeries(window, emptyList(), now, zone)
        assertEquals(listOf(false), series)
    }

    @Test
    fun `a clamped window still reports real history within it`() {
        // Installed three days ago, logged on two of them.
        val window = ProgressCalculations.visibleWindowDays(7, today.minusDays(2), now, zone)
        val logs = listOf(
            today.atTime(LocalTime.NOON).atZone(zone).toInstant(),
            today.minusDays(2).atTime(LocalTime.NOON).atZone(zone).toInstant(),
        )
        assertEquals(3, window)
        assertEquals(listOf(true, false, true), ProgressCalculations.loggedDaySeries(window, logs, now, zone))
        assertEquals(2, ProgressCalculations.daysLoggedInLast(window, logs, now, zone))
    }
}
