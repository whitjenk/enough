package com.enough.app.domain.progress

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.domain.nutrition.MealNutrition
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Fiber logged on one calendar day. */
data class DailyFiber(val date: LocalDate, val fiberG: Double)

/**
 * Pure aggregations behind the Progress screen. No Room/Android dependency, so
 * the trend/consistency math is directly unit-tested. "Consistency" is
 * deliberately streak-free — a count of days logged, never a streak that breaks
 * (CLAUDE.md / DESIGN.md).
 */
object ProgressCalculations {

    /** Total fiber grams grouped by the calendar day each meal was logged. */
    fun fiberByDay(meals: List<MealWithFood>, zone: ZoneId): Map<LocalDate, Double> =
        meals.groupBy { it.meal.timestamp.atZone(zone).toLocalDate() }
            .mapValues { (_, dayMeals) -> MealNutrition.fiberGrams(dayMeals) }

    /**
     * A [nDays]-long series ending today (oldest first), zero-filled for days
     * with no meals, so the trend chart always has a full, aligned window.
     */
    fun fiberSeries(
        nDays: Int,
        meals: List<MealWithFood>,
        now: Instant,
        zone: ZoneId,
    ): List<DailyFiber> {
        val byDay = fiberByDay(meals, zone)
        val today = now.atZone(zone).toLocalDate()
        return (nDays - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            DailyFiber(date = date, fiberG = byDay[date] ?: 0.0)
        }
    }

    /**
     * For each of the last [nDays] calendar days (oldest first, ending today),
     * whether at least one log fell on that day. Backs the streak-free
     * consistency dots — a gap is just an unfilled dot, nothing "breaks".
     */
    fun loggedDaySeries(
        nDays: Int,
        logInstants: List<Instant>,
        now: Instant,
        zone: ZoneId,
    ): List<Boolean> {
        val loggedDates = logInstants.map { it.atZone(zone).toLocalDate() }.toSet()
        val today = now.atZone(zone).toLocalDate()
        return (nDays - 1 downTo 0).map { back -> today.minusDays(back.toLong()) in loggedDates }
    }

    /**
     * How many of the last [nDays] calendar days (including today) had at least
     * one log. Streak-free: a gap doesn't reset anything.
     */
    fun daysLoggedInLast(
        nDays: Int,
        logInstants: List<Instant>,
        now: Instant,
        zone: ZoneId,
    ): Int = loggedDaySeries(nDays, logInstants, now, zone).count { it }
}
