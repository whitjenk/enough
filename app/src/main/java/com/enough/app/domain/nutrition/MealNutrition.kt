package com.enough.app.domain.nutrition

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.model.EstimateCalibration
import kotlin.math.roundToInt

/** Nutrient totals for a set of meals, each scaled by its serving multiplier. */
data class NutritionTotals(
    val fiberG: Double,
    val carbsG: Double,
    val proteinG: Double,
) {
    val fiberGRounded: Int get() = fiberG.roundToInt()
}

/**
 * Pure aggregation of logged meals into nutrient totals. A meal contributes its
 * food's per-serving nutrients times [MealWithFood]'s serving multiplier. Kept
 * dependency-free so both the Today screen and the rules engine reuse it.
 */
object MealNutrition {
    fun totals(meals: List<MealWithFood>): NutritionTotals {
        var fiber = 0.0
        var carbs = 0.0
        var protein = 0.0
        meals.forEach { item ->
            val m = item.meal.servingsMultiplier
            fiber += item.food.fiberG * m
            carbs += item.food.carbsG * m
            protein += item.food.proteinG * m
        }
        return NutritionTotals(fiberG = fiber, carbsG = carbs, proteinG = protein)
    }

    /**
     * Fiber logged so far, adjusting each meal's fiber by the person's
     * [calibration] before summing (SPEC §5): low leans the estimate down 15%,
     * high leans it up 15%, balanced leaves it as logged. Per-meal so it stays
     * correct once entry types with different uncertainty land (Phase 1).
     */
    fun fiberGrams(
        meals: List<MealWithFood>,
        calibration: EstimateCalibration = EstimateCalibration.BALANCED,
    ): Double = meals.sumOf { item ->
        item.food.fiberG * item.meal.servingsMultiplier * calibration.fiberMultiplier
    }

    /** How far a coarse quick-log's honest display range spreads around its center. */
    const val COARSE_ESTIMATE_SPREAD = 0.25

    /** The displayed low–high fiber band for one coarse-estimate entry. */
    data class FiberRange(val lowG: Int, val highG: Int)

    /**
     * Honest display range for a coarse quick-log entry: ±[COARSE_ESTIMATE_SPREAD]
     * around the same calibrated per-meal value that feeds the day's total, so
     * the row's band and the headline number never disagree about the center.
     */
    fun coarseFiberRange(
        item: MealWithFood,
        calibration: EstimateCalibration = EstimateCalibration.BALANCED,
    ): FiberRange {
        val center = item.food.fiberG * item.meal.servingsMultiplier * calibration.fiberMultiplier
        return FiberRange(
            lowG = (center * (1 - COARSE_ESTIMATE_SPREAD)).roundToInt(),
            highG = (center * (1 + COARSE_ESTIMATE_SPREAD)).roundToInt(),
        )
    }
}
