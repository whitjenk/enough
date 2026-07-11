package com.enough.app.domain.nutrition

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.model.MealSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MealNutritionTest {

    private fun meal(fiber: Double, carbs: Double, protein: Double, servings: Double): MealWithFood {
        val food = Food(id = 1, name = "Food", servingLabel = "1 cup", carbsG = carbs, fiberG = fiber, proteinG = protein)
        val entry = MealEntry(
            id = 1,
            foodId = 1,
            servingsMultiplier = servings,
            timestamp = Instant.EPOCH,
            source = MealSource.TEXT,
        )
        return MealWithFood(entry, food)
    }

    @Test
    fun `totals scale each meal by its serving multiplier and sum`() {
        val meals = listOf(
            meal(fiber = 7.5, carbs = 20.0, protein = 7.6, servings = 2.0), // black beans x2
            meal(fiber = 3.1, carbs = 27.0, protein = 1.3, servings = 1.0), // banana x1
        )
        val totals = MealNutrition.totals(meals)
        assertEquals(18.1, totals.fiberG, 1e-9)
        assertEquals(67.0, totals.carbsG, 1e-9)
        assertEquals(16.5, totals.proteinG, 1e-9)
        assertEquals(18, totals.fiberGRounded)
    }

    @Test
    fun `empty list totals to zero`() {
        val totals = MealNutrition.totals(emptyList())
        assertEquals(0.0, totals.fiberG, 1e-9)
        assertEquals(0, totals.fiberGRounded)
    }

    @Test
    fun `fractional servings are honored`() {
        assertEquals(4.9, MealNutrition.fiberGrams(listOf(meal(9.8, 12.0, 4.7, 0.5))), 1e-9)
    }
}
