package com.enough.app.domain.progress

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.model.MealSource
import org.junit.Assert.assertEquals
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
}
