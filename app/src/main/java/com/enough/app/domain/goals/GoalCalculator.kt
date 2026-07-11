package com.enough.app.domain.goals

import kotlin.math.roundToInt

/**
 * Pure calculations for the three Phase 0 goals. No Android/Room dependency so
 * these are directly unit-tested; the ViewModel calls into them.
 */
object GoalCalculator {
    const val MIN_WEIGHT_LOSS_PERCENT = 5.0
    const val MAX_WEIGHT_LOSS_PERCENT = 7.0
    const val DEFAULT_WEIGHT_LOSS_PERCENT = 5.0

    /** CDC framing: 14g of fiber per 1,000 kcal (SPEC.md §0.5). */
    const val FIBER_G_PER_1000_KCAL = 14.0

    const val DEFAULT_WEEKLY_ACTIVITY_MINUTES = 150

    /**
     * Target weight after losing [percent]% of [startWeightKg]. Percent is
     * coerced into the evidence-backed 5–7% band so callers can't set a goal
     * outside what the program supports.
     */
    fun targetWeightKg(startWeightKg: Double, percent: Double): Double {
        val clamped = percent.coerceIn(MIN_WEIGHT_LOSS_PERCENT, MAX_WEIGHT_LOSS_PERCENT)
        return startWeightKg * (1.0 - clamped / 100.0)
    }

    /**
     * Daily fiber target in grams from self-reported daily calories, rounded to
     * the nearest gram. A non-positive calorie estimate yields 0.
     */
    fun fiberTargetGrams(dailyCalories: Int): Int {
        if (dailyCalories <= 0) return 0
        return (FIBER_G_PER_1000_KCAL * dailyCalories / 1000.0).roundToInt()
    }
}
