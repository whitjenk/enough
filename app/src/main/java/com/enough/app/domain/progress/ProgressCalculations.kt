package com.enough.app.domain.progress

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.model.FeltLevel
import com.enough.app.domain.nutrition.MealNutrition
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Fiber logged on one calendar day. */
data class DailyFiber(val date: LocalDate, val fiberG: Double)

/**
 * Pure aggregations behind the Progress screen. No Room/Android dependency, so
 * the trend/consistency math is directly unit-tested. "Consistency" is
 * deliberately streak-free — a count of days logged, never a streak that breaks
 * (CLAUDE.md / DESIGN.md).
 */
object ProgressCalculations {

    /**
     * How many days of history there are actually anything to say about: the
     * requested [nDays], shortened on a young install to the days that have
     * elapsed since [startedOn] (inclusive of both ends).
     *
     * This exists because a full-length window on day one is not neutral. A
     * fresh install rendered six hollow dots and "Logged on 1 of the last 7
     * days" — reporting six misses for days the app did not exist on the phone,
     * which is exactly the shame pattern the app forbids, aimed at days the
     * person could not possibly have logged. Days before [startedOn] are absent,
     * not empty: the window grows to its full length as real history accrues.
     *
     * A null [startedOn] (no goal recorded yet) falls back to the full window —
     * there is nothing to clamp against, and under-reporting real history would
     * be its own kind of wrong.
     */
    fun visibleWindowDays(
        nDays: Int,
        startedOn: LocalDate?,
        now: Instant,
        zone: ZoneId,
    ): Int {
        if (startedOn == null) return nDays
        val today = now.atZone(zone).toLocalDate()
        // A clock skew or a restored backup can put the start date in the
        // future; treat that as day one rather than a negative window.
        val elapsed = ChronoUnit.DAYS.between(startedOn, today) + 1
        return elapsed.coerceIn(1L, nDays.toLong()).toInt()
    }

    /** Total fiber grams grouped by the calendar day each meal was logged. */
    fun fiberByDay(
        meals: List<MealWithFood>,
        zone: ZoneId,
        calibration: EstimateCalibration = EstimateCalibration.BALANCED,
    ): Map<LocalDate, Double> =
        meals.groupBy { it.meal.timestamp.atZone(zone).toLocalDate() }
            .mapValues { (_, dayMeals) -> MealNutrition.fiberGrams(dayMeals, calibration) }

    /**
     * A [nDays]-long series ending today (oldest first), zero-filled for days
     * with no meals, so the trend chart always has a full, aligned window.
     */
    fun fiberSeries(
        nDays: Int,
        meals: List<MealWithFood>,
        now: Instant,
        zone: ZoneId,
        calibration: EstimateCalibration = EstimateCalibration.BALANCED,
    ): List<DailyFiber> {
        val byDay = fiberByDay(meals, zone, calibration)
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

    /**
     * The felt check-in for each of the last [nDays] calendar days (oldest first,
     * ending today), or `null` for a day with no check-in. A missing day is just a
     * `null` in the series — never a "missed" marker — so skipping the optional
     * check-in carries no penalty (SPEC §7.6 Step 1). Independent of the logging
     * consistency series above: a check-in is a reflection, not a log.
     */
    fun feltSeries(
        nDays: Int,
        feltByDay: Map<LocalDate, FeltLevel>,
        now: Instant,
        zone: ZoneId,
    ): List<FeltLevel?> {
        val today = now.atZone(zone).toLocalDate()
        return (nDays - 1 downTo 0).map { back -> feltByDay[today.minusDays(back.toLong())] }
    }

    /** How many of the last [nDays] days had a check-in. Streak-free; skips don't count against anything. */
    fun checkInDaysInLast(
        nDays: Int,
        feltByDay: Map<LocalDate, FeltLevel>,
        now: Instant,
        zone: ZoneId,
    ): Int = feltSeries(nDays, feltByDay, now, zone).count { it != null }
}
