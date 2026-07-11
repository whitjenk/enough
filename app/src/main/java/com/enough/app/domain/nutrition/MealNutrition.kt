package com.enough.app.domain.nutrition

import com.enough.app.data.local.dao.MealWithFood
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

    fun fiberGrams(meals: List<MealWithFood>): Double = totals(meals).fiberG
}
